# Final report

`22044421-Dulhara-Anjana-Langappuli FPR.pdf` is the full write-up - methodology, results, evaluation, and the hardware chapters, with every figure and number pulled from `results/` and `experiments/`.

## Compiling from source

`latex_source/` has the full LaTeX source (Overleaf-ready). To compile locally:

```
cd latex_source
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
```

(a couple of passes are needed for the table of contents, citations and cross-references to all resolve - if a reference still shows as `??`, just run `pdflatex main.tex` once more). Alternatively, upload `latex_source/` as a project to [Overleaf](https://www.overleaf.com/) and it compiles automatically.

Requires a standard LaTeX distribution (TeX Live or MiKTeX) with `natbib`, `tocloft`, `fancyhdr` and `rotating` available - all in any full TeX Live/MiKTeX install.
