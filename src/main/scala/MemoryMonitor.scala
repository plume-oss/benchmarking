package io.github.plume.oss
import org.slf4j.{Logger, LoggerFactory}

import java.io.{BufferedWriter, FileWriter, File => JavaFile}
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable.ListBuffer
import scala.util.Using

class MemoryMonitor(job: Job) extends Thread {

  val db: String = if (!job.sootOnly) job.dbName else "Soot"
  val project: String =
    job.program.name.subSequence(job.program.name.lastIndexOf('/') + 1, job.program.name.length).toString

  lazy val logger: Logger = LoggerFactory.getLogger(Main.getClass)
  var stopProcess = new AtomicBoolean(false)

  override def run(): Unit =
    while (!stopProcess.get()) {
      captureMemoryResult()
      Thread.sleep(300)
    }

  def close(): Unit = {
    stopProcess.lazySet(true)
    join()
  }

  def writeLine(usedMemory: Long): Unit = {
    val csv = initializeFile()
    Using.resource(new BufferedWriter(new FileWriter(csv, true))) {
      _.append(s"$db,$project,$usedMemory\n")//$mean,$stddev,$max,$min\n")
    }
  }

  def captureMemoryResult(): Unit = {
    val runtime = Runtime.getRuntime
    val usedMemory = runtime.totalMemory - runtime.freeMemory
    writeLine(usedMemory)
  }

  private def initializeFile(): JavaFile = {
    val csv = new JavaFile(s"./results/memory_results.csv")
    if (!csv.exists()) {
      new JavaFile("./results/").mkdir()
      logger.info(s"Creating memory capture file ${csv.getAbsolutePath}")
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append("Database,Project,Memory\n")//Mean,StdDev,Max,Min\n")
      }
    }
    csv
  }

}
