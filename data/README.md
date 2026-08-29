# Data

Datasets aren't stored in this repository - they're large (State Farm alone is several GB) and most have their own licence terms. This folder holds the split files and (once you build it locally) the processed metadata table; the actual images stay wherever you download them.

## Layout

```
raw/        where to put downloaded datasets locally (git-ignored)
processed/  built metadata tables, e.g. the unified multi-view table from notebook 08 (git-ignored)
splits/     the subject-wise State Farm train/val/test split (tracked - this is the actual split used everywhere in the repo)
```

## State Farm expected layout

```
data/raw/state_farm/
    imgs/
        train/
            c0/ ... c9/
        test/
    driver_imgs_list.csv
```

`driver_imgs_list.csv`'s `subject` column is what `notebooks/02_subject_wise_split.ipynb` uses to build the subject-wise split - without it, splitting falls back to random image-level splitting, which lets the same driver appear in both train and test and inflates accuracy. See `references/dataset_sources.md` for where to get each dataset and `docs/dataset_overview.md` for more detail on State Farm specifically.

## The unified multi-view schema

Notebooks 07-09 work from one shared table instead of dataset-specific code, built by `notebooks/08_unified_multiview_dataset_builder.ipynb` into `data/processed/unified_multiview_metadata.csv`. Each row:

```
image_path, label_id, label_name, subject_id, dataset_name,
camera_view, vehicle_id, lighting, modality, split, is_synthetic, notes
```

Rebuild it locally by running notebook 08 against your own local copies of State Farm, SAM-DD and the Low-Light Driver Distraction Dataset.
