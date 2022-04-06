# Experiments

To run scalability benchmarks, first unpack the `src/main/resources/programs.tar.lzma` to
a `src/main/resources/programs` directory then run `RUN.sh`. These have configurable
parameters under `src/main/resources/*.yaml`.

To run IFSPEC taint analysis benchmarks run `RUN_IFSPEC.sh` that runs the tests defined under
`src/test`. The test suite will generate the body of a LaTeX table of the results at the end.