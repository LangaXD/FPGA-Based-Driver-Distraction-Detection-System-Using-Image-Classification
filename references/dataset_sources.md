# Dataset sources

## Used in this project

### State Farm Distracted Driver Detection

- Link: https://www.kaggle.com/competitions/state-farm-distracted-driver-detection/data
- RGB, single side-view camera, 10 classes, 22,424 images, subject metadata included.
- Used for: the baseline/transfer-learning comparison (notebooks 01-05).

### SAM-DD

- Indoor driving-simulator dataset giving paired front + side views for the same 42 subjects.
- No official class-to-folder mapping exists for its 0-9 label folders; the mapping used here (`notebooks/08_unified_multiview_dataset_builder.ipynb`, Adapter 2) comes from a visual audit of sample images, with per-class confidence recorded in the built table's `notes` column. 8 of 10 State Farm classes map cleanly; 2 (adjusting glasses, fatigue/head-drop) don't fit the fixed taxonomy and are excluded rather than force-mapped.
- Used for: camera-view robustness (notebook 09).

### Low-Light Driver Distraction Dataset (Saad, Khalil and Abbas, ICCES 2020)

- Link: https://data.mendeley.com/datasets/ykmr99nrsg/2, DOI 10.17632/ykmr99nrsg.2
- 70 driver folders, 10 classes, 52,350 frames - but only 9 of the 70 are genuinely night footage (found via a brightness audit of a representative frame per driver); the other 61 are daytime recordings on the same camera. `notebooks/08_unified_multiview_dataset_builder.ipynb` Adapter 5 records the true lighting condition per driver rather than trusting the dataset's own framing.
- Used for: low-light/night robustness (notebook 06).

## Requested, not yet integrated

- **AUC Distracted Driver Dataset V2** - near-identical class taxonomy to State Farm, genuinely different (front) camera angle. Adapter stubbed in notebook 08, ready to fill in once access is granted.
- **100-Driver** - 4 camera views, day/night NIR, 470,208 images. Larger integration effort; adapter stubbed in notebook 08.

## Reviewed during the literature survey, not integrated

These informed the survey (`docs/survey/`) and the identification of camera-view and lighting as the two robustness gaps this project addresses, but weren't brought into the training pipeline:

- **DMD / VicomTECH Driver Monitoring Dataset** - https://dmd.vicomtech.org/, RGB/depth/IR video, multimodal driver monitoring.
- **InSight** - DOI 10.1145/3384419.3430781, NIR LED-based low-light driver state monitoring.
- **SynDD2** - https://arxiv.org/abs/2204.08096, synthetic distracted-driving data.
- **DriverMHG** - https://arxiv.org/abs/2003.00951, multi-modal driver hand-gesture dataset.
