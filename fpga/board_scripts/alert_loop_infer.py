"""Continuous capture -> classify -> alert loop for the ZC702 board.

Runs the fine-tuned MobileNetV2 model against the connected camera in a
loop, applies the same 8-consecutive-confident-tick hysteresis logic as
fpga/rtl/distraction_alert_controller.v (and the notebook 09 webcam
temporal-smoothing cell), and POSTs to the backend's /api/alert-event
exactly once per real alert - on the rising edge, not once per frame -
matching the RTL module's `alert` output semantics. Also sounds the
physical buzzer wired to the ZC702's J62 header on that same rising edge
(see fpga/vivado_overlay_buzzer/ and fpga/board_notebooks/buzzer_tone_test.ipynb
for how that path was built and tuned).

Usage:
    python3 alert_loop_infer.py                  # run forever against the USB webcam
    python3 alert_loop_infer.py --max-ticks 30    # stop after N ticks (testing)
    python3 alert_loop_infer.py --self-test       # no camera/model - verifies the
                                                   # hysteresis state machine against
                                                   # the same scenarios as
                                                   # fpga/sim/distraction_alert_controller_tb.v
"""
import argparse
import base64
import glob
import sys
import time

import numpy as np
import requests

LABEL_NAMES = [
    "safe_driving", "texting_right", "phone_right", "texting_left", "phone_left",
    "adjusting_radio", "drinking", "reaching_behind", "hair_or_makeup", "talking_to_passenger",
]

MODEL_PATH = "/home/xilinx/mobilenetv2_crossview_finetuned_int8.tflite"
# Matched by the device's own UVC product-string descriptor, not a USB VID:PID -
# this particular webcam's descriptor is "HDF Webcam USB: HDF Webcam USB" with no
# ID embedded in it (unlike the HDMI capture card's "UVC Camera (345f:2109)"),
# confirmed via /sys/class/video4linux/videoN/name. 1b3f:2008 (Generalplus) is a
# separate audio/HID-only device on this board, not a camera - has no video
# interface at all, so it can never collide with this match.
WEBCAM_NAME_MATCH = "HDF Webcam USB"
SAFE_CLASS_ID = 0

# Point this at wherever mobile_app/backend/ is actually running. Plain HTTP
# on a non-standard port can get silently blocked on some networks (a campus
# content filter intercepts non-standard ports and serves a block page or
# times out); if the board reaches the backend over a network you don't
# control, putting HTTPS on 443 behind a real domain avoids that problem.
BACKEND_URL = "http://localhost:8001/api/alert-event"

# Buzzer: wired to J62 pin 1 (PMOD2_0 / package pin V7) via a minimal PYNQ
# overlay (fpga/vivado_overlay_buzzer/), driven as a passive-buzzer square
# wave since the buzzer on hand has no built-in oscillator. 2500 Hz was
# picked during manual listening tests as the clearest/loudest of several
# frequencies tried (fpga/board_notebooks/buzzer_tone_test.ipynb).
BUZZER_OVERLAY_PATH = "/home/xilinx/buzzer_overlay/buzzer.bit"
BUZZER_FREQUENCY_HZ = 2500
BUZZER_DURATION_S = 0.6

_buzzer_pin = None


def find_camera_device(name_match):
    """Resolves the current /dev/videoN node for a device by matching a
    substring of its own UVC descriptor name, rather than trusting a fixed
    node - numbering shifts across reboots and depending on what else is
    plugged in (documented repeatedly on this board: the webcam has been at
    video0, video2, and elsewhere across sessions)."""
    for name_path in sorted(glob.glob("/sys/class/video4linux/video*/name")):
        with open(name_path) as f:
            name = f.read().strip()
        if name_match in name:
            dev_num = name_path.split("/")[-2]
            return f"/dev/{dev_num}"
    raise RuntimeError(f"No video device found matching '{name_match}' - is the webcam connected?")

# Matches RTL CONF_THRESHOLD (102/255 ~= 0.40) and notebook 09's webcam cell.
CONFIDENCE_THRESHOLD = 0.40
ALERT_ON_COUNT = 8
ALERT_OFF_COUNT = 8


class AlertHysteresis:
    """Python port of fpga/rtl/distraction_alert_controller.v, tick-for-tick.

    The RTL's always-block uses non-blocking assignment, so its alert
    set/clear check reads the run counters *before* this tick's increment
    is applied - the same ordering is used here (check first, update
    after) so this class fires on the exact same tick the RTL would.
    Low-confidence ("uncertain") ticks hold both counters steady rather
    than resetting them, same as the RTL and the notebook 09 webcam cell.
    """

    def __init__(self):
        self.distracted_run = 0
        self.safe_run = 0
        self.alert = False

    def tick(self, class_id, confidence):
        """Feed one classification tick. Returns True exactly on the tick
        the alert transitions from off to on - the only tick that should
        POST to the backend.
        """
        is_distracted = class_id != SAFE_CLASS_ID and confidence >= CONFIDENCE_THRESHOLD
        is_safe = class_id == SAFE_CLASS_ID and confidence >= CONFIDENCE_THRESHOLD

        just_fired = False
        if not self.alert and is_distracted and self.distracted_run >= ALERT_ON_COUNT - 1:
            self.alert = True
            just_fired = True
        elif self.alert and is_safe and self.safe_run >= ALERT_OFF_COUNT - 1:
            self.alert = False

        if is_distracted:
            self.distracted_run = min(self.distracted_run + 1, ALERT_ON_COUNT)
            self.safe_run = 0
        elif is_safe:
            self.safe_run = min(self.safe_run + 1, ALERT_OFF_COUNT)
            self.distracted_run = 0
        # else: uncertain tick, both counters hold steady

        return just_fired


def _get_buzzer_pin():
    """Lazily loads the buzzer PYNQ overlay once per process and caches the
    GPIO pin handle - loading an overlay is comparatively slow, so this must
    not happen on every single alert.
    """
    global _buzzer_pin
    if _buzzer_pin is None:
        from pynq import Overlay
        ol = Overlay(BUZZER_OVERLAY_PATH)
        _buzzer_pin = ol.buzzer_gpio.channel1[0]
    return _buzzer_pin


def sound_buzzer_alert(frequency_hz=BUZZER_FREQUENCY_HZ, duration_s=BUZZER_DURATION_S):
    """Rings the physical buzzer wired to J62. Bit-bangs a square wave (the
    buzzer on hand is passive - no built-in oscillator, a steady DC level
    only produces a single click, not a tone) using a tight busy-wait loop
    rather than time.sleep() for less jitter, matching the tuning done in
    fpga/board_notebooks/buzzer_tone_test2.ipynb.

    Best-effort: a buzzer/overlay failure must never crash the alert loop,
    same philosophy as post_alert()'s own handling of a failed HTTP POST.
    """
    try:
        pin = _get_buzzer_pin()
        period = 1.0 / frequency_hz
        half_period = period / 2
        end_time = time.time() + duration_s
        next_toggle = time.time()
        state = 0
        while time.time() < end_time:
            now = time.time()
            if now >= next_toggle:
                state = 1 - state
                pin.write(state)
                next_toggle += half_period
        pin.write(0)
    except Exception as e:
        print(f"BUZZER FAILED: {e}")


def post_alert(class_name, confidence, frame_bgr=None):
    """POSTs a real alert. If frame_bgr (a BGR numpy array, straight from
    cv2.VideoCapture.read()) is given, it's JPEG-encoded and sent along so the
    Android app can show what the driver was actually doing, not just a text
    label - see mobile_app/backend/app/main.py's /api/alert-event.
    """
    payload = {"class_name": class_name, "confidence": confidence}
    if frame_bgr is not None:
        import cv2
        ok, jpeg = cv2.imencode(".jpg", frame_bgr)
        if ok:
            payload["image_base64"] = base64.b64encode(jpeg.tobytes()).decode()
        else:
            print("WARNING: failed to JPEG-encode the alert frame - sending without an image")

    try:
        resp = requests.post(BACKEND_URL, json=payload, timeout=10)
        print(f"POST {BACKEND_URL} -> {resp.status_code} {resp.text}")
        return resp.status_code == 200
    except Exception as e:
        # Best-effort, matching AuthRepository.registerFcmToken's philosophy
        # on the Android side: don't crash the loop over one failed POST.
        print(f"POST FAILED: {e}")
        return False


def run_self_test():
    """Replays the same 5 scenarios as fpga/sim/distraction_alert_controller_tb.v
    against this Python port, so the two implementations can be checked
    against each other without needing a camera or the model.
    """
    errors = 0

    def check(hysteresis, expected, label):
        nonlocal errors
        if hysteresis.alert != expected:
            print(f"FAIL {label}: expected alert={expected}, got alert={hysteresis.alert}")
            errors += 1
        else:
            print(f"PASS {label}: alert={hysteresis.alert} as expected")

    h = AlertHysteresis()
    check(h, False, "after init")

    for _ in range(10):
        h.tick(0, 0.86)  # safe_driving, high confidence
    check(h, False, "after 10 confident safe ticks")

    for _ in range(3):
        h.tick(4, 0.78)  # phone_left, short burst below ALERT_ON_COUNT
    check(h, False, "after 3 confident distracted ticks (transient, should be rejected)")

    h.tick(0, 0.86)
    check(h, False, "after returning to safe following a short burst")

    for _ in range(ALERT_ON_COUNT - 1):
        h.tick(2, 0.90)  # phone_right
    check(h, False, "one tick before ALERT_ON_COUNT reached")

    h.tick(2, 0.20)  # low confidence -> "uncertain", must not reset the run
    check(h, False, "uncertain tick should not itself trigger alert")

    fired = h.tick(2, 0.90)
    check(h, True, "alert should now be asserted after sustained distraction")
    if not fired:
        print("FAIL: tick() should have returned True on the firing tick")
        errors += 1

    for _ in range(5):
        h.tick(2, 0.90)
    check(h, True, "alert should remain asserted during continued distraction")

    for _ in range(ALERT_OFF_COUNT - 1):
        h.tick(0, 0.86)
    check(h, True, "one tick before ALERT_OFF_COUNT reached, alert should still hold")

    h.tick(0, 0.86)
    check(h, False, "alert should now be cleared after sustained safe driving")

    print()
    if errors == 0:
        print("TEST RESULT: ALL CHECKS PASSED (0 errors)")
    else:
        print(f"TEST RESULT: {errors} CHECK(S) FAILED")
    return errors == 0


def run_loop(max_ticks=None):
    from tflite_runtime import interpreter as tflite
    import cv2

    camera_device = find_camera_device(WEBCAM_NAME_MATCH)

    interp = tflite.Interpreter(model_path=MODEL_PATH)
    interp.allocate_tensors()
    inp = interp.get_input_details()[0]
    out = interp.get_output_details()[0]

    hysteresis = AlertHysteresis()
    tick_count = 0

    print(f"Starting continuous alert loop on {camera_device} (Ctrl+C to stop)")
    while max_ticks is None or tick_count < max_ticks:
        cap = cv2.VideoCapture(camera_device, cv2.CAP_V4L2)
        ret, frame = cap.read()
        cap.release()
        if not ret:
            print("CAPTURE_FAILED - retrying")
            time.sleep(1)
            continue

        img = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        img = cv2.resize(img, (224, 224), interpolation=cv2.INTER_LINEAR)
        x = img.astype(inp['dtype'])[None, ...]

        t0 = time.time()
        interp.set_tensor(inp['index'], x)
        interp.invoke()
        y = interp.get_tensor(out['index'])[0]
        elapsed = time.time() - t0

        class_id = int(np.argmax(y))
        confidence = float(y[class_id])
        tick_count += 1

        fired = hysteresis.tick(class_id, confidence)
        print(
            f"tick={tick_count} class={LABEL_NAMES[class_id]} conf={confidence:.3f} "
            f"({elapsed:.2f}s) distracted_run={hysteresis.distracted_run} "
            f"safe_run={hysteresis.safe_run} alert={hysteresis.alert}"
        )

        if fired:
            print(f"*** ALERT FIRED: {LABEL_NAMES[class_id]} (confidence {confidence:.3f}) ***")
            sound_buzzer_alert()
            post_alert(LABEL_NAMES[class_id], confidence, frame_bgr=frame)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--max-ticks", type=int, default=None)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()

    if args.self_test:
        ok = run_self_test()
        sys.exit(0 if ok else 1)
    else:
        run_loop(max_ticks=args.max_ticks)
