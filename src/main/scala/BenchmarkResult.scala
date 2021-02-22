package io.github.plume.oss

case class BenchmarkResult(
    fileName: String,
    loadingAndCompiling: Long,
    buildSoot: Long,
    buildPasses: Long,
)
