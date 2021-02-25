package io.github.plume.oss

case class BenchmarkResult(
    fileName: String,
    database: String,
    loadingAndCompiling: Long,
    buildSoot: Long,
    buildPasses: Long,
)
