package io.github.plume.oss

import util.ExtractorConst

import org.slf4j.{Logger, LoggerFactory}

import java.time.LocalTime
import java.util.concurrent.TimeUnit

object PrettyPrinter {

  var logger: Logger = LoggerFactory.getLogger(PrettyPrinter.getClass)

  def setLogger(logger: Logger): Unit = this.logger = logger

  def announcePlumeVersion(): Unit = {
    val bar = "==========================================================================="
    val centerText = s"PLUME v${ExtractorConst.INSTANCE.getPlumeVersion}"
    val numSpaces = (bar.length - centerText.length) / 2
    logger.info(bar)
    logger.info(s"${" " * numSpaces}$centerText")
    logger.info(bar)
  }

  def announceIteration(i: Int, d: String): Unit = {
    logger.info(s"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
    logger.info(
      s"$i${if (i % 10 == 1) "st" else if (i % 10 == 2) "nd" else if (i % 10 == 3) "rd" else "th"} iteration on driver $d"
    )
    logger.info(s"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
  }

  def announceBenchmark(f: String): Unit = {
    logger.info(s"---------------------------------------------------------------------------")
    logger.info(s"Running benchmark on: $f")
    logger.info(s"---------------------------------------------------------------------------")
  }

  def announceResults(b: BenchmarkResult): Unit = {
    val totalTime = b.compilingAndUnpacking + b.soot + b.scpgPasses + b.baseCpgBuilding
    val dbTime = b.databaseRead + b.databaseWrite
    logger.info(s"")
    logger.info(s"Benchmark results:")
    logger.info(s"")
    logger.info(s"\tCompiling and Unpacking.......${readableTime(b.compilingAndUnpacking)}")
    logger.info(s"\tSoot Related Processing.......${readableTime(b.soot)}")
    logger.info(s"\tBase CPG Building.............${readableTime(b.baseCpgBuilding)}")
    logger.info(s"\tSCPG Passes...................${readableTime(b.scpgPasses)}")
    logger.info(s"\t=======================Total: ${readableTime(totalTime)} (wall clock)")
    logger.info(s"\tDatabase Writes...............${readableTime(b.databaseWrite)}")
    logger.info(s"\tDatabase Reads................${readableTime(b.databaseRead)}")
    logger.info(s"\t=======================Total: ${readableTime(dbTime)} (CPU clock)")
  }

  def readableTime(nanoTime: Long): String =
    LocalTime
      .of(
        TimeUnit.NANOSECONDS.toHours(nanoTime).toInt,
        (TimeUnit.NANOSECONDS.toMinutes(nanoTime) - TimeUnit.NANOSECONDS.toHours(nanoTime) * 60).toInt,
        (TimeUnit.NANOSECONDS.toSeconds(nanoTime) - TimeUnit.NANOSECONDS.toMinutes(nanoTime) * 60).toInt,
        (nanoTime - TimeUnit.NANOSECONDS.toMicros(nanoTime) * 1000).toInt
      )
      .toString

}
