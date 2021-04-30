package io.github.plume.oss
import org.slf4j.{Logger, LoggerFactory}

import java.io.{BufferedWriter, FileWriter, File => JavaFile}
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import scala.util.Using

class MemoryMonitor(db: String, project: String) extends Thread {

  lazy val logger: Logger = LoggerFactory.getLogger(Main.getClass)
  var stopProcess = new AtomicBoolean(false)

  override def run(): Unit = {
      while(!stopProcess.get()) {
        captureMemoryResult()
        Thread.sleep(2500)
      }
  }

  def close(): Unit = {
    stopProcess.lazySet(true)
    join()
  }

  def captureMemoryResult(): Unit = {
    val runtime = Runtime.getRuntime
    val freeMemory = runtime.freeMemory
    val usedMemory = runtime.totalMemory - runtime.freeMemory
    val csv = new JavaFile(s"./results/memory_results_${db}_$project.csv")
    if (!csv.exists()) {
      new JavaFile("./results/").mkdir()
      logger.info(s"Creating memory capture file ${csv.getAbsolutePath}")
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append("Date,Size [B],Used [B]\n")
      }
    }
    Using.resource(new BufferedWriter(new FileWriter(csv, true))) {
      _.append(s"${LocalDateTime.now},${freeMemory},${usedMemory}\n")
    }
  }
}
