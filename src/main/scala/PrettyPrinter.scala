package io.github.plume.oss

import util.ExtractorConst

import org.slf4j.{Logger, LoggerFactory}

import java.time.LocalTime
import java.util.concurrent.TimeUnit

object PrettyPrinter {

  var logger: Logger = LoggerFactory.getLogger(PrettyPrinter.getClass)

  def setLogger(logger: Logger): Unit = this.logger = logger

  def announcePlumeVersion(): Unit = {
    logger.info("===============")
    logger.info(s"\tPLUME v${ExtractorConst.INSTANCE.getPlumeVersion}")
    logger.info("===============")
  }

  def announceIteration(i: Int, d: String): Unit = {
    logger.info(s"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
    logger.info(
      s"$i${if (i % 10 == 1) "st" else if (i % 10 == 2) "nd" else if (i % 10 == 3) "rd" else "th"} iteration on driver $d"
    )
    logger.info(s"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
  }

  def announceBenchmark(f: String): Unit = {
    logger.info(s"--------------------------------------------------------------------")
    logger.info(s"Running benchmark on: $f")
    logger.info(s"--------------------------------------------------------------------")
  }

  def announceResults(b: BenchmarkResult): Unit = {
    val totalTime = b.loadingAndCompiling + b.unitGraphBuilding + b.scpgPasses + b.baseCpgBuilding
    logger.info(s"")
    logger.info(s"Benchmark results:")
    logger.info(s"")
    logger.info(s"\tLoading and Compiling.........${readableTime(b.loadingAndCompiling)}")
    logger.info(s"\tUnit Graph Building...........${readableTime(b.unitGraphBuilding)}")
    logger.info(s"\tBase CPG Building.............${readableTime(b.baseCpgBuilding)}")
    logger.info(s"\tSCPG Passes...................${readableTime(b.scpgPasses)}")
    logger.info(s"\t=======================Total: ${readableTime(totalTime)}")
    logger.info(s"\tDatabase Writes...............${readableTime(b.databaseWrite)}")
    logger.info(s"\tDatabase Reads................${readableTime(b.databaseRead)}")
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
