package com.github.plume.oss

import CompressionUtil._
import drivers._

import com.github.nscala_time.time.Imports.LocalDateTime
import io.shiftleft.codepropertygraph.generated.NodeTypes
import io.shiftleft.codepropertygraph.generated.nodes.Call
import org.slf4j.{ Logger, LoggerFactory }
import overflowdb.traversal.Traversal

import java.io.{ BufferedWriter, FileWriter, File => JFile }
import java.nio.file.{ Files, Paths }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try, Using }

object RunBenchmark {

  lazy val logger: Logger = LoggerFactory.getLogger(RunBenchmark.getClass)

  val experimentConfig: ExperimentConfig = YamlDeserializer.experimentConfig("/experiments_conf.yaml")
  val taintConfig: TaintConfig = YamlDeserializer.taintDefsConfig("/taint_definitions.yaml")

  /**
    * Timeout in minutes
    */
  var timeout: Long = 5L * 60L

  private def runWithTimeout[T](timeoutMin: Long)(f: => T): T = {
    System.gc()
    Await.result(Future(f), timeoutMin minutes)
  }

  private def runWithTimeout[T](timeoutMin: Long, default: T)(f: => T): T =
    Try(runWithTimeout(timeoutMin)(f)) match {
      case Success(x) => x
      case Failure(y) => logger.error("Error occurred during experiment", y); default
    }

  def clearSerializedFiles(driver: DriverConfig): Unit = {
    val storage = driver match {
      case d: TinkerGraphConfig => Some(d.storageLocation)
      case d: OverflowDbConfig  => Some(d.storageLocation)
      case _                    => None
    }
    storage match {
      case Some(filePath) =>
        try {
          new JFile(filePath).delete()
        } catch {
          case e: Exception =>
            logger.error(s"Exception while deleting serialized file ${filePath}.", e)
        }
      case None =>
    }
  }

  /**
    * This job runs the first build while recording memory usage of the application and storage for serializable graphs.
    * If Soot Only mode is enabled then only memory will be recorded.
    *
    * @return true if one of the jobs timed out, false if otherwise
    */
  def runBuildAndStore(job: Job): Boolean = {
    val driver = DriverUtil.createDriver(job.driverConfig)
    try {
      val x = runInitBuild(job, driver, withExport = true)
      if (!job.experiment.runSootOnlyBuilds) {
        measureStorage(job)
        captureBenchmarkResult(x).timedOut
      } else {
        false
      }
    } finally {
      cleanUp(job, driver)
    }
  }

  private def measureStorage(job: Job): Unit = {
    val zipP = Files.createTempFile("plume-zip-benchmark-", ".zip").toAbsolutePath
    val zstdP = Files.createTempFile("plume-zstd-benchmark-", ".tar.zstd").toAbsolutePath
    val tarP = Files.createTempFile("plume-tar-benchmark-", ".tar").toAbsolutePath
    val xzP = Files.createTempFile("plume-lzma-benchmark-", ".tar.xz").toAbsolutePath
    try {
      val maybeSizes = job.driverConfig match {
        case c: OverflowDbConfig =>
          val input = Paths.get(c.storageLocation)
          tar(input, tarP)
          Some(
            StorageResult(
              job.program.jars.last.length,
              input.toFile.length(),
              zip(input, zipP),
              zstd(tarP, zstdP),
              xz(tarP, xzP)
            )
          )
        case c: TinkerGraphConfig =>
          val input = Paths.get(c.storageLocation)
          tar(input, tarP)
          Some(
            StorageResult(
              job.program.jars.last.length,
              input.toFile.length(),
              zip(input, zipP),
              zstd(tarP, zstdP),
              xz(tarP, xzP)
            )
          )
        case _ => None
      }
      maybeSizes match {
        case Some(result) =>
          captureStorageResult(job, result)
        case None =>
      }
    } finally {
      Seq(zipP, zstdP, tarP, xzP).foreach(_.toFile.delete())
    }
  }

  def cleanUp(job: Job, driver: IDriver): Unit = {
    if (driver.isConnected) {
      driver match {
        case _: TinkerGraphDriver =>
        case _: OverflowDbDriver  =>
        case _                    => driver.clear() // remote db's need to be explicitly cleared.
      }
      driver.close()
    }
    clearSerializedFiles(job.driverConfig)
  }

  def runBenchmark(f: JFile, job: Job, driver: IDriver, phase: String): BenchmarkResult = {
    PrettyPrinter.announceBenchmark(job.program.name, f.getName.stripSuffix(".jar"))
    new Jimple2Cpg()
      .createCpg(f.getAbsolutePath, driver = driver, sootOnlyBuild = job.experiment.runSootOnlyBuilds)
      .close() // close reference graph
    // Capture additional graph info
    val (nodeCount, edgeCount) = driver match {
      case d: OverflowDbDriver => (d.cpg.graph.nodeCount(), d.cpg.graph.edgeCount())
      case _                   => (-1, -1)
    }
    val externalMethods = driver
      .propertyFromNodes(NodeTypes.METHOD, "IS_EXTERNAL", "FULL_NAME")
      .count { m: Map[String, Any] =>
        val isExternal = m("IS_EXTERNAL").asInstanceOf[Boolean]
        val fullName = m("FULL_NAME").toString
        isExternal && !fullName.contains("<operator>")
      }
    if (phase.contains("DISCUPT")) closeConnectionWithExport(job, driver)
    val b = BenchmarkResult(
      fileName = job.program.name,
      phase = phase,
      database = if (!job.experiment.runSootOnlyBuilds) job.driverName else "Soot",
      time = PlumeStatistics.results().getOrElse(PlumeStatistics.TIME_EXTRACTION, -1L),
      connectDeserialize = PlumeStatistics.results().getOrElse(PlumeStatistics.TIME_OPEN_DRIVER, -1L),
      disconnectSerialize = PlumeStatistics.results().getOrElse(PlumeStatistics.TIME_CLOSE_DRIVER, -1L),
      programClasses = PlumeStatistics.results().getOrElse(PlumeStatistics.PROGRAM_CLASSES, 0L),
      programMethods = PlumeStatistics.results().getOrElse(PlumeStatistics.PROGRAM_METHODS, 0L),
      externalMethods = externalMethods,
      nodeCount = nodeCount,
      edgeCount = edgeCount
    )
    PrettyPrinter.announceResults(b)
    PlumeStatistics.reset()
    b
  }

  def captureBenchmarkResult(b: BenchmarkResult): BenchmarkResult = {
    val csv = new JFile("../results/result.csv")
    if (!csv.exists()) {
      new JFile("../results/").mkdir()
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append(
          "DATE," +
            "FILE_NAME," +
            "PHASE," +
            "DATABASE," +
            "TIME," +
            "CONNECT_DESERIALIZE," +
            "DISCONNECT_SERIALIZE," +
            "PROGRAM_CLASSES," +
            "PROGRAM_METHODS," +
            "EXTERNAL_METHODS," +
            "NODE_COUNT," +
            "EDGE_COUNT" +
            "\n"
        )
      }
    }
    Using.resource(new BufferedWriter(new FileWriter(csv, true))) {
      _.append(
        s"${LocalDateTime.now()}," +
          s"${b.fileName}," +
          s"${b.phase}," +
          s"${b.database}," +
          s"${b.time}," +
          s"${b.connectDeserialize}," +
          s"${b.disconnectSerialize}," +
          s"${b.programClasses}," +
          s"${b.programMethods}," +
          s"${b.externalMethods}," +
          s"${b.nodeCount}," +
          s"${b.edgeCount}\n"
      )
    }
    b
  }

  case class StorageResult(jarSize: Long, graphSize: Long, zipSize: Long, zstdSize: Long, xzSize: Long)

  def captureStorageResult(job: Job, result: StorageResult): Unit = {
    val csv = new JFile("../results/storage_results.csv")
    if (!csv.exists()) {
      new JFile("../results/").mkdir()
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append(
          "DATE," +
            "FILE_NAME," +
            "DATABASE," +
            "FILE_TYPE," +
            "FILE_SIZE" +
            "\n"
        )
      }
    }
    Using.resource(new BufferedWriter(new FileWriter(csv, true))) {
      _.append(
        s"""${LocalDateTime.now()},${job.program.name},${job.driverName},Project JAR File,${result.jarSize}
           |${LocalDateTime.now()},${job.program.name},${job.driverName},Stored Graph,${result.graphSize}
           |${LocalDateTime.now()},${job.program.name},${job.driverName},Stored Graph (ZIP),${result.zipSize}
           |${LocalDateTime.now()},${job.program.name},${job.driverName},Stored Graph (Zstd),${result.zstdSize}
           |${LocalDateTime.now()},${job.program.name},${job.driverName},Stored Graph (XZ),${result.xzSize}
           |""".stripMargin
      )
    }
  }

  def closeConnectionWithExport(job: Job, driver: IDriver): Unit =
    driver match {
      case w: TinkerGraphDriver =>
        try {
          w.exportGraph(job.driverConfig.asInstanceOf[TinkerGraphConfig].storageLocation)
        } catch {
          case _: Exception => logger.debug("TinkerGraph export does not exist yet.")
        } finally {
          w.close()
        }
      case x: GremlinDriver    => x.close()
      case y: OverflowDbDriver => y.close()
      case z: Neo4jDriver      => z.close()
      case _                   =>
    }

  /**
    * Runs the first JAR (oldest).
    */
  def runInitBuild(job: Job, driver: IDriver, withExport: Boolean = false): BenchmarkResult = {
    val default = generateDefaultResult(job)
    driver.clear()
    val memoryMonitor = new MemoryMonitor(job)
    try {
      runWithTimeout(
        timeout,
        default
      )({
        memoryMonitor.start()
        val ret = runBenchmark(job.program.jars.head, job, driver, "INITIAL")
        if (withExport) closeConnectionWithExport(job, driver)
        ret
      })
    } finally {
      memoryMonitor.close()
    }
  }

  /**
    * Runs the jobs where no connection lost and no cache cleared between runs.
    *
    * @return true if one of the jobs timed out, false if otherwise
    */
  def runLiveUpdates(job: Job): Boolean = {
    val driver = DriverUtil.createDriver(job.driverConfig)
    Try(runInitBuild(job, driver)) match {
      case Failure(e) =>
        logger.error("Failure while running live update initializer.", e)
        cleanUp(job, driver)
        return false
      case Success(initResult) =>
        runTaintAnalysis(driver)
        captureBenchmarkResult(initResult)
        if (initResult.timedOut) {
          cleanUp(job, driver)
          return true
        }
    }

    try {
      job.program.jars.drop(1).zipWithIndex.foreach {
        case (jar, i) =>
          val x = runWithTimeout(
            timeout,
            generateDefaultResult(job, s"UPDATE${i + 1}")
          )({
            runBenchmark(jar, job, driver, s"UPDATE${i + 1}")
          })
          runTaintAnalysis(driver)
          captureBenchmarkResult(x)
          if (x.timedOut) return true
      }
    } finally {
      logger.info("Live update experiments done, cleaning up...")
      cleanUp(job, driver)
      if (driver.isConnected) driver.close()
    }
    false
  }

  /**
    * Runs the jobs where driver is disconnected and cache cleared between runs.
    *
    * @return true if one of the jobs timed out, false if otherwise
    */
  def runDisconnectedUpdates(job: Job): Boolean = {
    var driver = DriverUtil.createDriver(job.driverConfig)
    Try(runInitBuild(job, driver)) match {
      case Failure(e) =>
        logger.error("Failure while running disconnected update initializer.", e)
        cleanUp(job, driver)
      case Success(initResult) =>
        if (initResult.timedOut) {
          cleanUp(job, driver)
          return true
        } else {
          runTaintAnalysis(driver)
          closeConnectionWithExport(job, driver)
          captureBenchmarkResult(initResult)
        }
    }

    try {
      job.program.jars.drop(1).zipWithIndex.foreach {
        case (jar, i) =>
          driver = DriverUtil.createDriver(job.driverConfig)
          val x = runWithTimeout(
            timeout,
            generateDefaultResult(job, s"DISCUPT${i + 1}")
          )({
            runBenchmark(jar, job, driver, s"DISCUPT${i + 1}")
          })
          runTaintAnalysis(driver)
          captureBenchmarkResult(x)
          if (x.timedOut) return true
      }
    } finally {
      logger.info("Disconnected update experiments done, cleaning up...")
      cleanUp(job, driver)
      if (driver.isConnected) driver.close()
    }
    false
  }

  /**
    * Runs the jobs where database and cache is cleared between runs.
    *
    * @return true if one of the jobs timed out, false if otherwise
    */
  def runFullBuilds(job: Job): Boolean = {
    var driver = DriverUtil.createDriver(job.driverConfig)
    Try(runInitBuild(job, driver)) match {
      case Failure(e) =>
        logger.error("Failure while running full build initializer.", e)
        cleanUp(job, driver)
      case Success(initResult) =>
        cleanUp(job, driver)
        captureBenchmarkResult(initResult)
        if (initResult.timedOut) return true
    }

    try {
      job.program.jars.drop(1).zipWithIndex.foreach {
        case (jar, i) =>
          driver = DriverUtil.createDriver(job.driverConfig)
          val x = runWithTimeout(
            timeout,
            generateDefaultResult(job, s"BUILD${i + 1}")
          )({
            runBenchmark(jar, job, driver, s"BUILD${i + 1}")
          })
          captureBenchmarkResult(x)
          if (x.timedOut) return true
          runTaintAnalysis(driver)
          cleanUp(job, driver)
      }
    } finally {
      logger.info("Full build experiments done, cleaning up...")
      cleanUp(job, driver)
      if (driver.isConnected) driver.close()
    }
    false
  }

  private def runTaintAnalysis(driver: IDriver): Unit =
    driver match {
      case d: OverflowDbDriver if experimentConfig.runTaintAnalysis =>
        logger.info("Running taint analysis")
        import io.shiftleft.semanticcpg.language._
        val sources = taintConfig.sources.flatMap { case (t: String, ms: List[String]) => ms.map(m => s"$t.$m.*") }.toSeq
        val sanitizers = taintConfig.sanitization.flatMap {
          case (t: String, ms: List[String]) => ms.map(m => s"$t.$m.*")
        }.toSeq
        val sinks = taintConfig.sinks.flatMap { case (t: String, ms: List[String]) => ms.map(m => s"$t.$m.*") }.toSeq

        def sink: Traversal[Call] = d.cpg.call.methodFullName(sinks: _*)
        def sanitizer: Traversal[Call] = d.cpg.call.methodFullName(sanitizers: _*)
        def source: Traversal[Call] = d.cpg.call.methodFullName(sources: _*)

        logger.info(s"Matched ${sink.size} sinks and ${sources.size} sources. ${sanitizers.size} sanitizers found.")

        val flows = d.nodesReachableBy(source, sink)
        if (flows.isEmpty) {
          logger.info("No data flows matched.")
        } else {
          logger.info(s"${flows.size} data flows matched.")
        }
      case _ =>
    }

  private def generateDefaultResult(job: Job, phase: String = "INITIAL") = {
    val dbName = if (!job.experiment.runSootOnlyBuilds) job.driverName else "Soot"
    BenchmarkResult(fileName = job.program.name, phase = phase, database = dbName)
  }
}
