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

  private def clearSerializedFiles(driver: DriverConfig): Unit = {
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
          case _: Exception =>
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
    val dbName = if (!job.experiment.runSootOnlyBuilds) job.driverName else "Soot"
    val default = BenchmarkResult(fileName = job.program.name, phase = "INITIAL", database = dbName)
    val driver = DriverUtil.createDriver(job.driverConfig)
    try {
      runWithTimeout(
        timeout,
        BenchmarkResult(fileName = job.program.name, phase = "INITIAL", database = dbName)
      )({
        val memoryMonitor = new MemoryMonitor(job)
        memoryMonitor.start()
        val ret = runInitBuild(job, driver)
        closeConnectionWithExport(job, driver)
        memoryMonitor.close()
        ret
      }) match {
        case x: BenchmarkResult => captureBenchmarkResult(x).timedOut
        case _                  => default.timedOut
      }
    } finally {
      cleanUp(job, driver)
      driver.close()
    }
  }

  def cleanUp(job: Job, driver: IDriver): Unit = {
    driver.clear()
    clearSerializedFiles(job.driverConfig)
  }

  def runBenchmark(f: JFile, job: Job, driver: IDriver, phase: String): BenchmarkResult = {
    PrettyPrinter.announceBenchmark(job.program.name, f.getName.stripSuffix(".jar"))
    new Jimple2Cpg().createCpg(f.getAbsolutePath, driver = driver)
    val b = BenchmarkResult(
      fileName = job.program.name,
      phase = phase,
      database = if (!job.experiment.runSootOnlyBuilds) job.program.name else "Soot",
      time = PlumeStatistics.results().getOrElse(PlumeStatistics.TIME_EXTRACTION, -1L),
      connectDeserialize = PlumeStatistics.results().getOrElse(PlumeStatistics.TIME_OPEN_DRIVER, -1L),
      disconnectSerialize = PlumeStatistics.results().getOrElse(PlumeStatistics.TIME_CLOSE_DRIVER, -1L)
    )
    PrettyPrinter.announceResults(b)
    b
  }

  def captureBenchmarkResult(b: BenchmarkResult): BenchmarkResult = {
    val csv = new JFile("./results/result.csv")
    if (!csv.exists()) {
      new JFile("./results/").mkdir()
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append(
          "DATE," +
            "FILE_NAME," +
            "PHASE," +
            "DATABASE," +
            "TIME," +
            "CONNECT_DESERIALIZE," +
            "DISCONNECT_SERIALIZE" +
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
          s"${b.disconnectSerialize}\n"
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
  def runInitBuild(job: Job, driver: IDriver): BenchmarkResult =
    runBenchmark(job.program.jars.head, job, driver, "INITIAL")
//
//  /**
//    * Runs the jobs where no connection lost and no cache cleared between runs.
//    *
//    * @return true if one of the jobs timed out, false if otherwise
//    */
//  def runLiveUpdates(job: Job): Boolean = {
//    val x = runInitBuild(job)
//    if (x.timedOut) return true
//    try {
//      job.program.jars.drop(1).zipWithIndex.foreach {
//        case (jar, i) =>
//          runWithTimeout(
//            timeout,
//            BenchmarkResult(fileName = job.program.name, phase = s"UPDATE$i", database = job.dbName)
//          )({
//            runBenchmark(jar, job.program.name, s"UPDATE$i", job.dbName, job.driver)
//          }) match {
//            case x: BenchmarkResult =>
//              captureBenchmarkResult(x)
//              if (x.timedOut) return true
//            case _ =>
//          }
//      }
//      false
//    } finally {
//      cleanUp(job.driver)
//    }
//  }
//
//  /**
//    * Runs the jobs where driver is disconnected and cache cleared between runs.
//    *
//    * @return true if one of the jobs timed out, false if otherwise
//    */
//  def runDisconnectedUpdates(job: Job): Boolean = {
//    cleanUp(job.driver)
//    runInitBuild(job)
//    closeConnectionWithExport(job.driver)
//    try {
//      job.program.jars.drop(1).zipWithIndex.foreach {
//        case (jar, i) =>
//          runWithTimeout(
//            timeout,
//            BenchmarkResult(fileName = job.program.name, phase = s"DISCUPT$i", database = job.dbName)
//          )({
//            LocalCache.INSTANCE.clear()
//            openConnectionAndConfigure(job.driver)
//            val ret = runBenchmark(jar, job.program.name, s"DISCUPT$i", job.dbName, job.driver)
//            closeConnectionWithExport(job.driver)
//            ret
//          }) match {
//            case x: BenchmarkResult =>
//              captureBenchmarkResult(x)
//              if (x.timedOut) return true
//            case _ =>
//          }
//      }
//    } finally {
//      openConnection(job.driver)
//      cleanUp(job.driver)
//    }
//    false
//  }
//
//  /**
//    * Runs the jobs where database and cache is cleared between runs.
//    *
//    * @return true if one of the jobs timed out, false if otherwise
//    */
//  def runFullBuilds(job: Job): Boolean = {
//    cleanUp(job.driver)
//    try {
//      job.program.jars.drop(1).zipWithIndex.foreach {
//        case (jar, i) =>
//          runWithTimeout(
//            timeout,
//            BenchmarkResult(fileName = job.program.name, phase = s"DISCUPT$i", database = job.dbName)
//          )({
//            cleanUp(job.driver)
//            runBenchmark(jar, job.program.name, s"BUILD$i", job.dbName, job.driver)
//          }) match {
//            case x: BenchmarkResult =>
//              captureBenchmarkResult(x)
//              if (x.timedOut) return true
//            case _ =>
//          }
//      }
//    } finally {
//      cleanUp(job.driver)
//    }
//    false
//  }

}
