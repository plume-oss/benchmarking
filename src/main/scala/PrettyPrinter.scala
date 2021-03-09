package io.github.plume.oss

import util.ExtractorConst

import org.slf4j.{Logger, LoggerFactory}

object PrettyPrinter {

  var logger: Logger = LoggerFactory.getLogger(PrettyPrinter.getClass)

  def setLogger(logger: Logger): Unit = this.logger = logger

  def announcePlumeVersion(): Unit = {
    logger.info("===============")
    logger.info(s"\tPLUME v${ExtractorConst.INSTANCE.getPlumeVersion}")
    logger.info("===============")
  }

  def announceIteration(i: Int, d: String): Unit = {
    logger.info(s"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
    logger.info(
      s"$i${if (i % 10 == 1) "st" else if (i % 10 == 2) "nd" else if (i % 10 == 3) "rd" else "th"} on driver $d"
    )
    logger.info(s"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
  }

  def announceBenchmark(f: String): Unit = {
    logger.info(s"-----------------------------------------------------")
    logger.info(s"Running benchmark on: $f")

  }

  def announceResults(b: BenchmarkResult): Unit = {
    logger.info(s" Benchmark results:")
    logger.info(s"")
    logger.info(s"\tLoading and Compiling.........${readableTime(b.loadingAndCompiling)}")
    logger.info(s"\tUnit Graph Building...........${readableTime(b.unitGraphBuilding)}")
    logger.info(s"\tSCPG Passes...................${readableTime(b.scpgPasses)}")
    logger.info(s"\t=======================Total: ${readableTime(b.loadingAndCompiling + b.unitGraphBuilding + b.scpgPasses)}")
    logger.info(s"\tDatabase Writes...............${readableTime(b.databaseWrite)}")
    logger.info(s"\tDatabase Reads................${readableTime(b.databaseRead)}")
    logger.info(s"-----------------------------------------------------")
  }

  def readableTime(nanoTime: Long): String =
    nanoTime match {
      case it if Math.pow(10, 3).toInt until Math.pow(10, 9).toInt contains it =>
        s"${it * Math.pow(10, -9)} s"
      case it if Math.pow(10, 9).toInt until Math.pow(10, 9).toInt * 60 contains it =>
        s"${it * Math.pow(10, -9) / 60} min"
      case it if Math.pow(10, 9).toInt * 60 until Math.pow(10, 9).toInt * Math.pow(60, 2).toInt contains it =>
        s"${it * Math.pow(10, -9) / Math.pow(60, 2)} h"
      case it => s"$it ns"
    }

}
