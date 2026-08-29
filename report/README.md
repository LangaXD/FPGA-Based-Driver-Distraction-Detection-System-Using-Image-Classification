# Final report

`final_report.pdf` is the full write-up - methodology, results, evaluation, and the hardware chapters, with every figure and number pulled from `results/` and `experiments/`.

## Compiling from source

`latex_source/` has the full LaTeX source (Overleaf-ready). To compile locally:

```
cd latex_source
pdflatex main.tex
biber main
pdflatex main.tex
pdflatex main.tex
```

(three passes are needed for the table of contents, citations and cross-references to all resolve). Alternatively, upload `latex_source/` as a project to [Overleaf](https://www.overleaf.com/) and it compiles automatically.

Requires a standard LaTeX distribution (TeX Live or MiKTeX) with `biblatex`/`biber`, `tocloft`, `fancyhdr` and `rotating` available - all in any full TeX Live/MiKTeX install.
