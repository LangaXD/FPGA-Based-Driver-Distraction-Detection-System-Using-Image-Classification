# 03 - EfficientNet-B0

The second transfer-learning model tried (`notebooks/04_transfer_learning.ipynb`), for comparison against MobileNetV2 on the same split and training recipe.

**Result:** 82.47% test accuracy, 81.58% macro F1, 82.79% weighted F1 (`results/reports/efficientnetb0_summary.csv`, `efficientnetb0_classification_report.csv`) - close to MobileNetV2 but slightly behind, and heavier, so MobileNetV2 was the one taken forward into later stages.

Saved in three forms: `best_efficientnetb0.weights.h5` / `best_efficientnetb0_finetuned.weights.h5` (weights only) and `efficientnetb0_finetuned_savedmodel/` (full TF SavedModel).

Confusion matrix: `results/confusion_matrices/efficientnetb0_confusion_matrix.png`.
