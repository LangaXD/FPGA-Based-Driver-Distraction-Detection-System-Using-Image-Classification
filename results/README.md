# Results

Everything the notebooks in `notebooks/` and `fpga/notebooks/` produced, organised by type rather than by stage:

```
figures/             training curves, dataset distributions, model comparisons, FPGA/board figures
tables/              the same results as CSVs (per-class accuracy, comparison tables, dataset summaries)
reports/             classification reports and per-model summaries
confusion_matrices/  confusion matrix CSVs + PNGs for each single-view model
screenshots/         real captured evidence: Vivado synthesis/simulation, board inference, backend/app alerts
```

Filenames are prefixed by what produced them (`baseline_cnn_*`, `mobilenetv2_*`, `mobilenetv2_crossview_*`, `mobilenetv2_lowlight_*`, `efficientnetb0_*`, `zc702_*`), so the same figure's origin notebook is usually obvious from its name. See each `experiments/*/README.md` for which figures belong to which stage and what the numbers mean.
