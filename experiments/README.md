# Experiments

Trained model weights and training logs, one folder per stage, in the order the notebooks build on each other:

| Folder | Notebook | What it is |
|---|---|---|
| `00_dataset_exploration` | 01 | dataset checks, no model |
| `01_baseline_cnn` | 03 | CNN trained from scratch, the accuracy floor |
| `02_mobilenetv2` | 04 | best single-view model, carried forward everywhere else |
| `03_efficientnet_b0` | 04 | second transfer-learning model, for comparison |
| `04_subject_wise_evaluation` | 05 | the three models above compared on the same held-out split |
| `05_ir_like_augmentation` | 06 | low-light robustness (day+night training) |
| `06_cross_view_mobilenetv2` | 09 | camera-view robustness (front+side training) |

Each folder's own README has the actual numbers and file list. Weights are tracked in this repo so the results can be reproduced without retraining; the training logs and history CSVs are the raw source the figures in `results/` were generated from.
