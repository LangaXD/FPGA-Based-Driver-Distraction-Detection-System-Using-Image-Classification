# 06 - Cross-view MobileNetV2

Tests whether a model trained on one camera angle generalises to another, and fixes it where it doesn't (`notebooks/09_cross_view_training_mobilenetv2.ipynb`). State Farm is side-view only; merging in SAM-DD (front + side, same 42 subjects, via the unified schema built in notebook 08) gives a real front/side comparison instead of a single-view number.

| | Mixed | Side | Front |
|---|---|---|---|
| Frozen backbone | 75.94% | 73.93% | 78.79% |
| **Fine-tuned** | **91.76%** | **91.05%** | **92.77%** |

The frozen-backbone run already shows the core finding this stage exists to produce: uneven generalisation across views (front noticeably better than side, on data neither view was over-represented in). Fine-tuning the last 30 MobileNetV2 layers closes most of that gap and lifts both views well past the frozen numbers - every class improved, with the previously weak `talking_to_passenger` recall going from 13.6% to 51.9%.

The fine-tuned checkpoint here (`best_mobilenetv2_crossview_finetuned.keras`) is the model carried forward to the FPGA/ZC702 stage - converted to TFLite INT8 in `fpga/deployment/` and verified to match its own predictions 1:1 on-device, so the accuracy numbers above are what the board actually runs, not just a lab result.

Figures: `results/figures/mobilenetv2_crossview_{training_curves,confusion_matrices,qualitative_samples}.png`. Tables: `results/reports/mobilenetv2_crossview_*`, `results/tables/mobilenetv2_crossview_*`.
