package com.github.plume.oss

import drivers._

import com.github.nscala_time.time.Imports.LocalDateTime
import org.slf4j.{ Logger, LoggerFactory }

import java.io.{ BufferedWriter, FileWriter, File => JFile }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try, Using }

object RunBenchmark {

  lazy val logger: Logger = LoggerFactory.getLogger(RunBenchmark.getClass)

  val experimentConfig: ExperimentConfig = YamlDeserializer.experimentConfig("/experiments_conf.yaml")

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
      case Failure(y) => logger.error(y.getMessage); default
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
    * This job runs the first build while recording memory usage of the application.
    *
    * @return true if one of the jobs timed out, false if otherwise
    */
  def runBuildAndStore(job: Job): Boolean = {
    val driver = DriverUtil.createDriver(job.driverConfig)
    try {
      val x = runInitBuild(job, driver, withExport = true)
      captureBenchmarkResult(x).timedOut
    } finally {
      cleanUp(job, driver)
    }
  }

  def cleanUp(job: Job, driver: IDriver): Unit = {
    if (driver.isConnected) {
      driver match {
        case _: TinkerGraphDriver =>
        case _: OverflowDbDriver =>
        case _ => driver.clear() // remote db's need to be explicitly cleared.
      }
      driver.close()
    }
    clearSerializedFiles(job.driverConfig)
  }

  def runBenchmark(f: JFile, job: Job, driver: IDriver, phase: String): BenchmarkResult = {
    PrettyPrinter.announceBenchmark(job.program.name, f.getName.stripSuffix(".jar"))
    new Jimple2Cpg().createCpg(f.getAbsolutePath, driver = driver, sootOnlyBuild = job.experiment.runSootOnlyBuilds)
    val b = BenchmarkResult(
      fileName = job.program.name,
      phase = phase,
      database = if (!job.experiment.runSootOnlyBuilds) job.driverName else "Soot",
      time = PlumeStatistics.results().getOrElse(PlumeStatistics.TIME_EXTRACTION, -1L),
      connectDeserialize = PlumeStatistics.results().getOrElse(PlumeStatistics.TIME_OPEN_DRIVER, -1L),
      disconnectSerialize = PlumeStatistics.results().getOrElse(PlumeStatistics.TIME_CLOSE_DRIVER, -1L),
      changedClasses = PlumeStatistics.results().getOrElse(PlumeStatistics.CHANGED_CLASSES, 0L),
      changedMethods = PlumeStatistics.results().getOrElse(PlumeStatistics.CHANGED_METHODS, 0L),
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
            "CHANGED_CLASSES," +
            "CHANGED_METHODS" +
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
          s"${b.changedClasses}," +
          s"${b.changedMethods}\n"
      )
    }
    b
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
    runWithTimeout(
      timeout,
      default
    )({
      val memoryMonitor = new MemoryMonitor(job)
      memoryMonitor.start()
      val ret = runBenchmark(job.program.jars.head, job, driver, "INITIAL")
      if (withExport) closeConnectionWithExport(job, driver)
      memoryMonitor.close()
      ret
    })
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
          closeConnectionWithExport(job, driver)
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
          captureBenchmarkResult(x)
          if (x.timedOut) return true
          else closeConnectionWithExport(job, driver)
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
          cleanUp(job, driver)
      }
    } finally {
      logger.info("Full build experiments done, cleaning up...")
      cleanUp(job, driver)
      if (driver.isConnected) driver.close()
    }
    false
  }

  private def generateDefaultResult(job: Job, phase: String = "INITIAL") = {
    val dbName = if (!job.experiment.runSootOnlyBuilds) job.driverName else "Soot"
    BenchmarkResult(fileName = job.program.name, phase = phase, database = dbName)
  }
}
