import glob
import time
import numpy as np
import cv2
from tflite_runtime import interpreter as tflite

LABEL_NAMES = [
    "safe_driving", "texting_right", "phone_right", "texting_left", "phone_left",
    "adjusting_radio", "drinking", "reaching_behind", "hair_or_makeup", "talking_to_passenger",
]

MODEL_PATH = "/home/xilinx/mobilenetv2_crossview_finetuned_int8.tflite"
CAPTURE_CARD_USB_ID = "345f:2109"  # MacroSilicon MS2109 HDMI capture card


def find_capture_device(usb_id):
    # /dev/videoN numbering shifts across reboots depending on USB enumeration
    # order (confirmed 2026-08-15: was video0 previously, came up as video2
    # this boot) - match by device name instead of trusting a fixed node.
    for name_path in sorted(glob.glob("/sys/class/video4linux/video*/name")):
        with open(name_path) as f:
            name = f.read().strip()
        if usb_id in name:
            dev_num = name_path.split("/")[-2]
            return f"/dev/{dev_num}"
    raise RuntimeError(f"No video device found matching '{usb_id}' - is the HDMI capture card connected?")


CAPTURE_DEVICE = find_capture_device(CAPTURE_CARD_USB_ID)
print(f"Using capture device: {CAPTURE_DEVICE}")

interp = tflite.Interpreter(model_path=MODEL_PATH)
interp.allocate_tensors()
inp = interp.get_input_details()[0]
out = interp.get_output_details()[0]

cap = cv2.VideoCapture(CAPTURE_DEVICE, cv2.CAP_V4L2)
ret, frame = cap.read()
cap.release()
if not ret:
    print("CAPTURE_FAILED")
    raise SystemExit(1)

if frame.mean() < 1.0:
    print("WARNING: frame looks black - is an HDMI source actually connected and playing?")

rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
resized = cv2.resize(rgb, (224, 224), interpolation=cv2.INTER_LINEAR)
x = resized.astype(np.float32)[None, ...]

t0 = time.time()
interp.set_tensor(inp['index'], x)
interp.invoke()
y = interp.get_tensor(out['index'])[0]
elapsed = time.time() - t0

idx = int(np.argmax(y))
print(f"FRAME_SHAPE={frame.shape} MEAN_PIXEL={frame.mean():.1f}")
print(f"INFERENCE_TIME_S={elapsed:.2f}")
print(f"PREDICTED_CLASS={LABEL_NAMES[idx]} CONFIDENCE={y[idx]:.3f}")
print("END_TO_END_OK")
