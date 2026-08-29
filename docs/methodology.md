# Methodology Plan

## Overall Project Methodology

The project has two connected phases.

## Phase 1: AI and Robustness Phase

This phase focuses on image classification, dataset bias, subject-wise evaluation and low-light/IR/NIR robustness.

Steps:

1. Literature review
2. Dataset selection
3. Dataset exploration
4. Class distribution analysis
5. Subject distribution analysis
6. Random split baseline
7. Subject-wise split evaluation
8. Baseline CNN model
9. Transfer learning models
10. Evaluation using accuracy, macro-F1, per-class recall and confusion matrix
11. Synthetic IR-like/NIR-style augmentation experiment if feasible
12. Robustness analysis

## Phase 2: FPGA Hardware Phase

This phase focuses on hardware deployment after the AI pipeline is stable.

Steps:

1. Select lightweight model
2. Apply model compression
3. Apply quantisation-aware training
4. Convert model for hardware deployment
5. Map model to FPGA
6. Evaluate latency
7. Evaluate FPS
8. Evaluate power
9. Evaluate energy per frame
10. Report LUT, DSP and BRAM usage
11. Compare accuracy before and after quantisation

## Evaluation Metrics

AI evaluation:

- Accuracy
- Macro precision
- Macro recall
- Macro F1-score
- Per-class precision
- Per-class recall
- Confusion matrix
- False-negative analysis

Hardware evaluation:

- Latency
- FPS
- Throughput
- Power
- Energy per frame
- LUT usage
- DSP usage
- BRAM usage
- Accuracy loss after quantisation

## Important Experimental Warning

Random image-level splitting can cause subject leakage. Subject-wise splitting is more realistic because it tests unseen-driver generalisation.
