# 02 - MobileNetV2

Transfer learning on top of ImageNet-pretrained MobileNetV2 (`notebooks/04_transfer_learning.ipynb`) - the best-performing single-view model, and the one carried forward into the cross-view and low-light stages and onto the ZC702 board.

- Same subject-wise split as the baseline.
- Two-phase training: frozen backbone first, then fine-tuning (`best_mobilenetv2.keras` and `best_mobilenetv2_finetuned.keras` respectively).

**Result:** 84.67% test accuracy, 84.01% macro F1, 84.94% weighted F1 (`results/reports/mobilenetv2_summary.csv`, `mobilenetv2_classification_report.csv`) - the best of the three single-view models, and the reason it's the one used everywhere downstream (cross-view training, low-light training, FPGA deployment).

Confusion matrix: `results/confusion_matrices/mobilenetv2_confusion_matrix.png`.
