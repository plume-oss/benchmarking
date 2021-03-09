package io.github.plume.oss

case class BenchmarkResult(
    fileName: String,
    database: String,
    loadingAndCompiling: Long,
    unitGraphBuilding: Long,
    databaseWrite: Long,
    databaseRead: Long,
    scpgPasses: Long
) {
  override def toString: String = s"BenchmarkResult { " +
    s"fileName=$fileName, " +
    s"database=$database, " +
    s"loadingAndCompiling=${loadingAndCompiling*Math.pow(10, -9)}s }, " +
    s"unitGraphBuilding=${unitGraphBuilding*Math.pow(10, -9)}s, " +
    s"databaseWrite=${databaseWrite*Math.pow(10, -9)}s, " +
    s"databaseRead=${databaseRead*Math.pow(10, -9)}s, " +
    s"scpgPasses=${scpgPasses*Math.pow(10, -9)}s, " +
    " }"
}
