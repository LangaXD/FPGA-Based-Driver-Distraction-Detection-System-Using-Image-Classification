# FPGA-Based Driver Distraction Detection System

MSc project (Computer Science, AI & Robotics) at the University of Hertfordshire. Image-classification-based driver distraction detection, extended to camera-view and low-light robustness, and deployed on real FPGA/Zynq hardware for the alert stage plus on-device CNN inference.

The work has two halves that build on each other:

1. **AI / robustness** — train and evaluate CNNs for driver distraction detection, then stress-test them against two realistic domain shifts a single-dataset model never sees: a different camera angle, and low-light/night driving.
2. **Hardware** — take the trained model to actual hardware: an RTL decision/alert controller synthesised for a real FPGA part, and a standalone Zynq (ZC702) board running the CNN itself, camera in and alert out, no laptop involved at runtime.

A companion Android app and backend show a full alert pipeline end to end (detection on the board → backend → phone notification).

## Results at a glance

| Stage | Model | Test accuracy |
|---|---|---|
| Single-view baseline (State Farm) | Baseline CNN | 75.6% |
| Single-view baseline (State Farm) | **MobileNetV2** | **84.7%** |
| Single-view baseline (State Farm) | EfficientNet-B0 | 82.5% |
| Cross-view (State Farm + SAM-DD, front+side) | MobileNetV2, fine-tuned | 91.8% mixed (91.1% side / 92.8% front) |
| Low-light robustness (day + night) | MobileNetV2, fine-tuned | 77.9% mixed (81.2% day / 71.1% night) |
| On-device, ZC702 board (TFLite INT8) | MobileNetV2 cross-view, fine-tuned | 77.3%, matched 1:1 against the laptop model on the same images |

Full breakdowns, confusion matrices and training curves are in `results/`. The reasoning behind each stage — why cross-view and low-light needed their own training runs, what the accuracy trade-offs mean — is in the report (`report/final_report.pdf`).

## Repository layout

```
notebooks/       the AI pipeline, run in order (01-10) - this is where the actual code lives
notebooks/demos/ laptop-side live demo notebooks (webcam/video/photo)
experiments/     trained model weights + training logs, one folder per model
results/         figures, tables, confusion matrices, screenshots used in the report
data/            subject-wise train/val/test split files (the images themselves aren't stored here, see below)
fpga/            RTL, synthesis, simulation, and the ZC702 board pipeline
mobile_app/      Android app + FastAPI backend for alert notifications
report/          the final report (PDF + LaTeX source)
docs/            survey, methodology, planning documents
references/      dataset sources and literature references
```

## Setup

```
python -m venv .venv
.venv\Scripts\activate        # or source .venv/bin/activate on Linux/Mac
pip install -r requirements.txt
```

Python 3.10+ recommended (matches the TensorFlow/Keras versions the models were trained with). The Android app and backend have their own setup steps below.

### Datasets

Datasets aren't stored in this repository — they're large, and most have their own licence terms. Download them yourself and point the notebooks at your local copy:

| Dataset | Used for | Source |
|---|---|---|
| State Farm Distracted Driver Detection | Baseline training (notebooks 01-05) | [Kaggle](https://www.kaggle.com/competitions/state-farm-distracted-driver-detection/data) |
| SAM-DD | Cross-view training (notebooks 07-09) | see `references/dataset_sources.md` |
| Low-Light Driver Distraction Dataset | Low-light robustness (notebook 06) | [Mendeley, DOI 10.17632/ykmr99nrsg.2](https://doi.org/10.17632/ykmr99nrsg.2) |

Each notebook's early cells set the expected local folder layout — check `references/dataset_sources.md` and `docs/dataset_overview.md` for the exact structure each adapter expects. `data/splits/` already contains the subject-wise State Farm split used throughout the project, so notebook 02 doesn't need to be re-run unless you want to regenerate it.

## Running the pipeline

Notebooks are numbered in the order they're meant to run, and each one picks up where the last left off (reading from `data/`, `experiments/`, `results/` as needed):

| Notebook | What it does |
|---|---|
| `01_dataset_exploration` | Loads State Farm, checks class balance and image integrity |
| `02_subject_wise_split` | Builds the train/val/test split by driver, not by image (avoids subject leakage) |
| `03_baseline_cnn_training` | Trains a CNN from scratch as the accuracy floor |
| `04_transfer_learning` | Trains MobileNetV2 and EfficientNet-B0 via transfer learning |
| `05_evaluation_analysis` | Compares all three models: accuracy, F1, confusion matrices |
| `06_ir_like_augmentation` | Low-light robustness: trains on real day+night footage, measures the domain-shift gap |
| `07_multiview_dataset_review` | Reviews the multi-view datasets (SAM-DD, Low-Light DD) before integration |
| `08_unified_multiview_dataset_builder` | Converts each dataset into one shared schema (see below) so they can be trained on together |
| `09_cross_view_training_mobilenetv2` | Trains MobileNetV2 across camera views (front + side), frozen backbone then fine-tuned |
| `10_synthetic_front_view_augmentation_plan` | Gap analysis for a possible synthetic-augmentation follow-up (planning only, no image generation) |

`notebooks/demos/` runs the fine-tuned model live from a laptop webcam, a video file, or a single photo — useful for a quick demo without the board.

Each trained model is saved under `experiments/<stage>/` and reloaded by later notebooks, so you don't need to retrain earlier stages to run a later one — the weights are already included in this repo.

### The unified multi-view schema

Notebook 08 converts every dataset into one shared table so models can train across camera views and lighting conditions without dataset-specific code downstream. Each row has:

```
image_path, label_id, label_name, subject_id, dataset_name,
camera_view, vehicle_id, lighting, modality, split, is_synthetic, notes
```

State Farm, SAM-DD and the Low-Light dataset each get their own adapter cell that maps their raw folder structure onto this schema — including documenting *how* confident each class mapping is, since two of the three datasets (SAM-DD, Low-Light DD) have no official class-to-folder mapping and had to be resolved by visually auditing sample images. The built table is written to `data/processed/unified_multiview_metadata.csv` (git-ignored, rebuild it locally by running notebook 08).

## FPGA hardware

Two independent hardware results, both real and verified on physical parts:

- **Alert-stage RTL** (`fpga/rtl/distraction_alert_controller.v`) — takes a class ID + confidence per tick, raises an alert only after 8 consecutive confident "distracted" predictions (hysteresis, matching the same debounce logic used in the software demos), synthesised for a Xilinx Artix-7 (`xc7a35tcpg236-1`): 18 LUTs, 13 FFs, 0 BRAM/DSP, meets 100MHz timing with +7.3ns slack. Reports in `fpga/reports/`. Regenerate the Vivado GUI project with `fpga/scripts/create_project.tcl`; the simulation testbenches run standalone via `fpga/notebooks/01_rtl_simulation.ipynb`.
- **On-device CNN inference** — the fine-tuned cross-view model, converted to TFLite INT8 (`fpga/deployment/`) and run standalone on a ZC702 (Zynq-7000) board: camera in, inference, alert out, no laptop at runtime. `fpga/board_scripts/alert_loop_infer.py` is the main deployment script; `fpga/board_notebooks/` has demo notebooks for live camera, HDMI capture, video file, and single-photo input. `fpga/vivado_overlay_buzzer/` is a small PYNQ overlay (AXI GPIO) that drives a physical buzzer on the same alert signal.

`fpga/README.md` has the full write-up, including what's synthesis-verified vs. what's actually run on hardware.

## Mobile app + backend

`mobile_app/backend/` is a small FastAPI service (login, alert history, FCM push) backed by SQLite — see `mobile_app/backend/README.md` for endpoints and how to run it locally. `mobile_app/DistractionAlertApp/` is the Kotlin/Android companion app (Retrofit + Room + MPAndroidChart) that talks to it; see `mobile_app/DistractionAlertApp/README.md` for the build steps and `FIREBASE_SETUP.md` for wiring up push notifications with your own Firebase project (the app ships without a real Firebase config — you need to supply your own).

## Report and background reading

- `report/final_report.pdf` — the full write-up: methodology, results, evaluation, and the hardware chapters. LaTeX source is in `report/latex_source/`.
- `docs/survey/` — the literature survey behind the project (PRISMA-based).
- `docs/methodology.md`, `docs/timeline.md` — project planning documents.

## License

MIT — see `LICENSE`.
