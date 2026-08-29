# 04 - Subject-wise evaluation

Not a separate model - this is where the three single-view models (baseline CNN, MobileNetV2, EfficientNet-B0) get compared side by side, all evaluated on the same subject-wise held-out test set (`notebooks/05_evaluation_analysis.ipynb`).

Subject-wise splitting matters here specifically because a random image-level split would let images from the same driver land in both train and test, letting a model partially learn driver identity (clothing, seat position, camera framing) instead of the actual distraction class - which would inflate every number above. `data/splits/` guarantees no driver appears in more than one split.

| Model | Test accuracy | Macro F1 | Weighted F1 |
|---|---|---|---|
| Baseline CNN | 75.60% | 75.27% | 76.57% |
| **MobileNetV2** | **84.67%** | **84.01%** | **84.94%** |
| EfficientNet-B0 | 82.47% | 81.58% | 82.79% |

Full comparison tables and figures: `results/tables/model_comparison_summary.csv`, `results/figures/model_comparison_test_accuracy.png`, `model_comparison_f1_scores.png`, `accuracy_vs_fps_all_models.png`.
