package io.github.plume.oss
import org.slf4j.{Logger, LoggerFactory}

import java.io.{BufferedWriter, FileWriter, File => JavaFile}
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable.ListBuffer
import scala.util.Using

class MemoryMonitor(job: Job) extends Thread {

  val db: String = job.driverName
  val project: String =
    job.program.name.subSequence(job.program.name.lastIndexOf('/') + 1, job.program.name.length).toString

  lazy val logger: Logger = LoggerFactory.getLogger(Main.getClass)
  val memoryUsed: ListBuffer[Long] = new ListBuffer()
  var stopProcess = new AtomicBoolean(false)

  override def run(): Unit =
    while (!stopProcess.get()) {
      Thread.sleep(2500)
      captureMemoryResult()
    }

  def close(): Unit = {
    stopProcess.lazySet(true)
    val csv = initializeFile()
    val mean = NumericOperations.mean(memoryUsed)
    val stddev = NumericOperations.stdDev(memoryUsed)
    val max = memoryUsed.max
    val min = memoryUsed.min

    val now = Calendar.getInstance().getTime
    val minuteFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    val date = minuteFormat.format(now)
    Using.resource(new BufferedWriter(new FileWriter(csv, true))) {
      _.append(s"$date,${job.dbName},$project,$mean,$stddev,$max,$min\n")
    }
    join()
  }

  def captureMemoryResult(): Unit = {
    val memoryUsedByThisProcess = memoryUsed.size * 16 // 16 bytes per instance of Long
    val runtime = Runtime.getRuntime
    val usedMemory = runtime.totalMemory - runtime.freeMemory - memoryUsedByThisProcess
    memoryUsed.append(usedMemory)
  }

  private def initializeFile(): JavaFile = {
    val csv = new JavaFile(s"./results/memory_results.csv")
    if (!csv.exists()) {
      new JavaFile("./results/").mkdir()
      logger.info(s"Creating memory capture file ${csv.getAbsolutePath}")
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append("Date,Database,Project,Mean,StdDev,Max,Min\n")
      }
    }
    csv
  }

}
