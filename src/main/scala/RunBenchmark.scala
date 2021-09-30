package io.github.plume.oss

import Main._
import io.github.plume.oss.drivers.IDriver
import store.LocalCache

import org.slf4j.{ Logger, LoggerFactory }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }

object RunBenchmark {

  lazy val logger: Logger = LoggerFactory.getLogger(RunBenchmark.getClass)

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

  /**
    * This job runs the first build while recording memory usage of the application.
    *
    * @return true if one of the jobs timed out, false if otherwise
    */
  def runBuildAndStore(job: Job): Boolean = {
    val dbName = if (!job.sootOnly) job.dbName else "Soot"
    val default = BenchmarkResult(fileName = job.program.name, phase = "INITIAL", database = dbName)
    job.driver.clearGraph()
    LocalCache.INSTANCE.clear()
    try {
      runWithTimeout(
        timeout,
        BenchmarkResult(fileName = job.program.name, phase = "INITIAL", database = dbName)
      )({
        val memoryMonitor = new MemoryMonitor(job)
        memoryMonitor.start()
        val ret = runInitBuild(job)
        closeConnectionWithExport(job.driver)
        memoryMonitor.close()
        openConnectionAndConfigure(job.driver)
        ret
      }) match {
        case x: BenchmarkResult => captureBenchmarkResult(x).timedOut
        case _                  => default.timedOut
      }
    } finally {
      cleanUp(job.driver)
    }
  }

  def cleanUp(driver: IDriver): Unit = {
    driver.clearGraph()
    LocalCache.INSTANCE.clear()
    clearSerializedFiles()
  }

  /**
    * Runs the first JAR (oldest).
    */
  def runInitBuild(job: Job): BenchmarkResult =
    runBenchmark(job.program.jars.head, job.program.name, "INITIAL", job.dbName, job.driver, job.sootOnly)

  /**
    * Runs the jobs where no connection lost and no cache cleared between runs.
    *
    * @return true if one of the jobs timed out, false if otherwise
    */
  def runLiveUpdates(job: Job): Boolean = {
    val x = runInitBuild(job)
    if (x.timedOut) return true
    try {
      job.program.jars.drop(1).zipWithIndex.foreach {
        case (jar, i) =>
          runWithTimeout(
            timeout,
            BenchmarkResult(fileName = job.program.name, phase = s"UPDATE$i", database = job.dbName)
          )({
            runBenchmark(jar, job.program.name, s"UPDATE$i", job.dbName, job.driver)
          }) match {
            case x: BenchmarkResult =>
              captureBenchmarkResult(x)
              if (x.timedOut) return true
            case _ =>
          }
      }
      false
    } finally {
      cleanUp(job.driver)
    }
  }

  /**
    * Runs the jobs where driver is disconnected and cache cleared between runs.
    *
    * @return true if one of the jobs timed out, false if otherwise
    */
  def runDisconnectedUpdates(job: Job): Boolean = {
    cleanUp(job.driver)
    runInitBuild(job)
    closeConnectionWithExport(job.driver)
    try {
      job.program.jars.drop(1).zipWithIndex.foreach {
        case (jar, i) =>
          runWithTimeout(
            timeout,
            BenchmarkResult(fileName = job.program.name, phase = s"DISCUPT$i", database = job.dbName)
          )({
            LocalCache.INSTANCE.clear()
            openConnectionAndConfigure(job.driver)
            val ret = runBenchmark(jar, job.program.name, s"DISCUPT$i", job.dbName, job.driver)
            closeConnectionWithExport(job.driver)
            ret
          }) match {
            case x: BenchmarkResult =>
              captureBenchmarkResult(x)
              if (x.timedOut) return true
            case _ =>
          }
      }
    } finally {
      openConnection(job.driver)
      cleanUp(job.driver)
    }
    false
  }

  /**
    * Runs the jobs where database and cache is cleared between runs.
    *
    * @return true if one of the jobs timed out, false if otherwise
    */
  def runFullBuilds(job: Job): Boolean = {
    cleanUp(job.driver)
    try {
      job.program.jars.drop(1).zipWithIndex.foreach {
        case (jar, i) =>
          runWithTimeout(
            timeout,
            BenchmarkResult(fileName = job.program.name, phase = s"DISCUPT$i", database = job.dbName)
          )({
            cleanUp(job.driver)
            runBenchmark(jar, job.program.name, s"BUILD$i", job.dbName, job.driver)
          }) match {
            case x: BenchmarkResult =>
              captureBenchmarkResult(x)
              if (x.timedOut) return true
            case _ =>
          }
      }
    } finally {
      cleanUp(job.driver)
    }
    false
  }

}
