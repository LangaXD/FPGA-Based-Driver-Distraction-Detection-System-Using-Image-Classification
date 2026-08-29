# 05 - Low-light robustness

Tests whether a model trained only on daytime footage survives a genuine lighting domain shift, and whether training on real night footage fixes it (`notebooks/06_ir_like_augmentation.ipynb`). Originally planned as synthetic IR-like augmentation, but switched to training on the real Low-Light Driver Distraction Dataset once it was available - a strictly harder and more honest test than synthetic augmentation would have been.

Of that dataset's 70 driver folders, only 9 are genuine night footage (found via a brightness audit of a representative frame per driver, threshold-checked and confirmed visually) - the other 61 are daytime recordings on the same camera. Day and night driver pools are split separately (70/15/15 each) so both lighting conditions are guaranteed to appear in every split.

| | Baseline (day-only, zero retraining) | Frozen backbone | Fine-tuned |
|---|---|---|---|
| Mixed test accuracy | 19.53% | 62.30% | **77.91%** |
| Day accuracy | 84.67% | 64.55% | 81.21% |
| Night accuracy | 19.53% | 57.67% | **71.13%** |

The day-only model doesn't just degrade at night, it collapses (19.53%, effectively guessing 2 of 10 classes). Fine-tuning the last 30 MobileNetV2 layers on the mixed day+night table recovers most of that gap (+51.6pt night accuracy over baseline), at a real but modest cost to day accuracy (84.67% -> 81.21%) - training one shared classifier across both domains trades a little single-domain accuracy for a lot of cross-domain robustness.

Files: `best_mobilenetv2_lowlight.keras` / `best_mobilenetv2_lowlight_finetuned.keras`, `training_log_frozen_backbone.txt` / `training_log_finetuned.txt`. Figures: `results/figures/mobilenetv2_lowlight_{training_curves,confusion_matrices,qualitative_samples}.png`. Full tables: `results/reports/mobilenetv2_lowlight_*`, `results/tables/mobilenetv2_lowlight_*`.
