package io.github.plume.oss

import drivers.IDriver

import java.io.{File => JavaFile}

case class Experiment(
    runBuildAndStore: Boolean,
    runLiveUpdates: Boolean,
    runDisconnectedUpdates: Boolean,
    runFullBuilds: Boolean,
    runSootOnlyBuilds: Boolean,
)

case class Program(name: String, jars: List[JavaFile])

case class Job(driver: IDriver, program: Program, dbName: String, sootOnly: Boolean = false) {
  val driverName: String = driver.getClass.toString.stripPrefix("class io.github.plume.oss.drivers.")
}

case class BenchmarkResult(
    fileName: String,
    phase: String,
    database: String,
    compilingAndUnpacking: Long = -1L,
    soot: Long = -1L,
    programStructureBuilding: Long = -1L,
    baseCpgBuilding: Long = -1L,
    databaseWrite: Long = -1L,
    databaseRead: Long = -1L,
    dataFlowPasses: Long = -1L,
    cacheHits: Long = -1L,
    cacheMisses: Long = -1L,
    connectDeserialize: Long = -1L,
    disconnectSerialize: Long = -1L
) {

  val timedOut: Boolean = compilingAndUnpacking == -1L

  override def toString: String =
    s"BenchmarkResult { " +
      s"fileName=$fileName, " +
      s"database=$database, " +
      s"compilingAndUnpacking=${compilingAndUnpacking * Math.pow(10, -9)}s, " +
      s"soot=${soot * Math.pow(10, -9)}s, " +
      s"programStructureBuilding=${programStructureBuilding * Math.pow(10, -9)}s, " +
      s"baseCpgBuilding=${baseCpgBuilding * Math.pow(10, -9)}s, " +
      s"databaseWrite=${databaseWrite * Math.pow(10, -9)}s, " +
      s"databaseRead=${databaseRead * Math.pow(10, -9)}s, " +
      s"dataFlowPasses=${dataFlowPasses * Math.pow(10, -9)}s " +
      s"cacheHits=${cacheHits}s, " +
      s"cacheMisses=${cacheMisses}s, " +
      s"connectDeserialize=${connectDeserialize}s, " +
      s"disconnectSerialize=${disconnectSerialize}s " +
      "}"
}
