# 01 - Baseline CNN

A small CNN trained from scratch (`notebooks/03_baseline_cnn_training.ipynb`), used as the accuracy floor the transfer-learning models are compared against.

- Subject-wise split (`data/splits/`), so no driver appears in more than one of train/val/test.
- 224x224 input, light augmentation (rotation/zoom/contrast), trained for up to 20 epochs with early stopping on validation accuracy.

**Result:** 75.60% test accuracy, 75.27% macro F1, 76.57% weighted F1 (`results/reports/baseline_cnn_summary.csv`, full per-class breakdown in `baseline_cnn_classification_report.csv`).

Files: `best_baseline_cnn.keras` (weights), `baseline_cnn_test_run_history.csv` (per-epoch accuracy/loss). Confusion matrix and training curves: `results/confusion_matrices/baseline_cnn_confusion_matrix.png`, `results/figures/baseline_cnn_test_run_accuracy.png`.
