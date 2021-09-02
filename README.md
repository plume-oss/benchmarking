# Benchmarking

The benchmarking suite for Plume related performance measures.

## Configuration

The suite is configured at `src/main/resources/config.yaml` where one can enable/disable
certain configurations and specify which storage backends to use.

The programs part of the configuration shows which jar in the `src/main/resources/programs` 
folder to use for the initial build and which to use in the update. These jars are currently
named after their commit hash.

## Running the suite

Use `sbt run` to start the benchmarks. Taint analysis is performed from sources
and sinks defined in `src/main/resources/taint_definitions.yaml`.
