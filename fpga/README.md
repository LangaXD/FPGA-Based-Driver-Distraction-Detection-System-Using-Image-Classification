# FPGA and hardware deployment

Two independent, genuinely-verified hardware results:

1. An RTL alert/decision controller, simulated and synthesised for a real FPGA part.
2. The trained CNN itself running standalone on a ZC702 (Zynq-7000) board - camera in, inference, alert out, no laptop involved at runtime.

Both are honestly scoped below: what's simulated, what's synthesised, and what actually ran on physical hardware.

## 1. Alert controller (RTL)

**Why this component:** a full quantised-CNN-on-FPGA accelerator (weight quantisation, a fixed-point conv/matmul datapath, feature-map memory scheduling) is a multi-week undertaking on its own. The alert/decision stage is a smaller, fully-verifiable component that mirrors the same debounce logic used on the software side (`notebooks/09_cross_view_training_mobilenetv2.ipynb`'s temporal smoothing): raw per-frame predictions are noisy, so the alert should only fire after several consecutive confident "distracted" frames, not on one flickered classification.

**`fpga/rtl/distraction_alert_controller.v`** - inputs are `class_id` (0-9), `confidence` (8-bit fixed-point, Q0.8, matching a softmax output scaled to 0-255), and `valid` (one pulse per classification). Two saturating counters track consecutive confident-distracted and confident-safe ticks; `alert` raises after 8 consecutive confident-distracted ticks and clears after 8 consecutive confident-safe ticks (hysteresis, so it doesn't chatter at the boundary). Low-confidence ticks hold the counters steady rather than resetting them. `CONF_THRESHOLD = 102` (~0.40 in Q0.8) matches the same 0.40 confidence threshold used in the Python temporal-smoothing code.

**Simulation** (`fpga/sim/distraction_alert_controller_tb.v`, run via `xvlog` → `xelab` → `xsim`, log at `fpga/sim/sim_run.log`): a self-checking testbench, 10/10 checks passed. Two worth calling out: a 3-tick transient burst of "distracted" classifications does not raise the alert (the debounce genuinely rejects noise, not just runs), and a low-confidence tick in the middle of a distracted run doesn't reset the count (the "uncertain" state is handled deliberately).

**Synthesis** (`fpga/scripts/run_synth.tcl` → `fpga/reports/`), out-of-context, targeting `xc7a35tcpg236-1` - the Artix-7 part on a Digilent Basys3 board, chosen as a genuinely low-cost academic FPGA rather than an arbitrary part:

| Resource | Used | Available | Utilisation |
|---|---|---|---|
| Slice LUTs | 18 | 20,800 | 0.09% |
| Slice Registers | 13 | 41,600 | 0.03% |
| Block RAM | 0 | 50 | 0% |
| DSP slices | 0 | 90 | 0% |

Constrained to 100MHz: all timing met, WNS = +7.276ns (≈367MHz theoretical Fmax for this module alone - the CNN, not this controller, would be the real system's throughput bottleneck). Power is a vector-less estimate at 0.069W (Vivado itself flags this as "medium confidence," not a real switching-activity measurement).

**End-to-end pipeline simulation** - the question this answers is "camera → trained model → FPGA → alert, does the actual interface work, driven by real model output, not a hand-wavy diagram." `fpga/rtl/uart_rx.v` is a standard 8N1 UART receiver (the piece that would sit on a physical board and receive bytes over USB-serial from a laptop); `fpga/rtl/camera_link_top.v` decodes a 2-byte frame (`class_id`, `confidence`) and drives the same alert controller. `fpga/sim/camera_link_top_tb.v` bit-bangs a real UART stream reproducing 28 real predictions from `experiments/06_cross_view_mobilenetv2/best_mobilenetv2_crossview.keras` on 28 real test images (source data: `fpga/reports/real_model_session_sequence.csv`), including two genuine misclassifications the model actually made - both correctly absorbed by the debounce logic without a false alert. All 6 checks passed (log: `fpga/sim/camera_link_sim_run.log`).

**What this is not:** not the CNN running on FPGA (the classifier runs in software; this is the receive/decide/alert stage a real deployment would sit behind it) and not run on physical hardware for this part specifically - simulated with Vivado's cycle-accurate simulator, not hand-estimated, but no Basys3 board was available at this stage.

**Regenerating the GUI project:** `fpga/scripts/create_project.tcl` builds a normal Vivado GUI project wrapping the above (`fpga/vivado_project/distraction_alert.xpr` once built - not stored in the repo since Vivado regenerates it from this script). The runnable wrapper notebooks for all of this are in `fpga/notebooks/` (01 = simulation, 02 = synthesis).

## 2. On-device inference (ZC702 board)

**Board:** ZC702 Evaluation Kit (XC7Z020, Zynq-7000), running a community PYNQ image (Ubuntu 18.04, Python 3.6). Official Vitis-AI/DPU tooling doesn't support this chip family, so that path was never attempted - inference runs as plain TFLite on the Cortex-A9.

**What works end to end:** USB webcam → CNN inference → prediction, entirely on the board, no laptop involved. The fine-tuned cross-view model is converted to TFLite INT8 (`fpga/deployment/`), and `fpga/board_scripts/alert_loop_infer.py` runs it continuously with the same 8-tick hysteresis as the RTL controller, POSTs alerts to the backend, and sounds a physical buzzer through a small PYNQ overlay (`fpga/vivado_overlay_buzzer/` - AXI GPIO wired to header J62 pin 1). `fpga/board_notebooks/` has demo notebooks for live camera, HDMI capture, a video file, and a single photo.

**The real blocker along the way, and how it was resolved:** the only `tflite_runtime` wheel published for this board's exact platform (2.5.0) crashed with `Illegal instruction` on every inference. Root cause: this Cortex-A9 has `vfpv3` but no `vfpv4`/FMA (checked directly via `/proc/cpuinfo`), while the published wheel assumes `vfpv4` - a known issue shared with the PYNQ-Z1/Z2 boards, which use the same SoC. Fixed by building `tflite_runtime` from source, natively on the board, targeting `vfpv3-d16`. After the fix, `interpreter.invoke()` succeeds and predictions match the laptop model's output on the same images 100/100 - the from-source build is numerically verified, not just crash-free.

**Accuracy on-device**, same 300-image held-out sample used for both:

| | Baseline (frozen backbone) | Fine-tuned |
|---|---|---|
| Laptop, Keras float32 | 59.3% | 80.0% |
| On-device, TFLite INT8 | 54.7% | **77.3%** |

Quantisation costs roughly 3-5 points; fine-tuning (unfreezing the last 30 MobileNetV2 layers) recovers far more than that, and the improvement holds on-device, not just in the lab. Inference is slow (~9.15s/image) since XNNPACK is disabled on this ARM32 build for safety - a known, explained limitation, not a correctness concern.

## Layout

```
rtl/                   the 3 RTL modules
sim/                    testbenches + their real pass/fail logs
scripts/                create_project.tcl, run_synth.tcl
reports/                utilisation/timing/power reports from the real synthesis run
notebooks/              runnable wrappers: 01 simulation, 02 synthesis, 03 TFLite conversion, 04 board check
vivado_overlay_buzzer/  PYNQ overlay sources + the built bitstream (buzzer.bit/.hwh)
deployment/             the converted TFLite models actually run on the board
board_scripts/          the board-side Python (webcam/capture/continuous alert loop)
board_notebooks/        board-hosted demo notebooks (live camera, HDMI capture, video, photo)
```
