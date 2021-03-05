package io.github.plume.oss

case class BenchmarkResult(
    fileName: String,
    database: String,
    loadingAndCompiling: Long,
    unitGraphBuilding: Long,
    databaseWrite: Long,
    databaseRead: Long,
    scpgPasses: Long
)
