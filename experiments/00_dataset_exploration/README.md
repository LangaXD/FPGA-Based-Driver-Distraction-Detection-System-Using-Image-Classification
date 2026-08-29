# 00 - Dataset exploration

Output of `notebooks/01_dataset_exploration.ipynb`: sanity checks on the State Farm dataset before any training happens (class balance, subject balance, image integrity).

- 22,424 images, 10 classes, roughly balanced (~2,100-2,500 images per class - see `results/tables/state_farm_class_distribution.csv`).
- 26 subjects (drivers), each contributing 500-1,250 images (`results/tables/state_farm_subject_distribution.csv`) - uneven enough that a subject-wise split (notebook 02) matters more than it would with a flatter distribution.
- Figures: `results/figures/state_farm_class_distribution.png`, `state_farm_subject_distribution.png`, `state_farm_sample_images.png`.

No model is trained at this stage - it's purely descriptive, and everything downstream (splits, training) is built on what's confirmed here.
