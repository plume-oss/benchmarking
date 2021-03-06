package io.github.plume.oss

import util.ExtractorConst

import org.slf4j.{Logger, LoggerFactory}

import java.time.Duration

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

  def announceBenchmark(name: String, commit: String): Unit = {
    logger.info(s"---------------------------------------------------------------------------")
    logger.info(s"$name @ $commit")
    logger.info(s"---------------------------------------------------------------------------")
  }

  def announceResults(b: BenchmarkResult): Unit = {
    val totalTime = b.compilingAndUnpacking + b.soot + b.programStructureBuilding + b.dataFlowPasses + b.baseCpgBuilding
    val dbTime = b.databaseRead + b.databaseWrite
    logger.info(s"")
    logger.info(s"Benchmark results of ${b.fileName}")
    logger.info(s"")
    logger.info(s"PHASE ${b.phase}")
    logger.info(s"\tCompiling and Unpacking.......${readableTime(b.compilingAndUnpacking)}")
    logger.info(s"\tSoot Related Processing.......${readableTime(b.soot)}")
    logger.info(s"\tProgram Structure Building....${readableTime(b.programStructureBuilding)}")
    logger.info(s"\tBase CPG Building.............${readableTime(b.baseCpgBuilding)}")
    logger.info(s"\tData Flow Passes..............${readableTime(b.dataFlowPasses)}")
    logger.info(s"\t=======================Total: ${readableTime(totalTime)} (wall clock)")
    logger.info(s"\tConnect/Deserialize...........${readableTime(b.connectDeserialize)}")
    logger.info(s"\tDisconnect/Serialize..........${readableTime(b.disconnectSerialize)}")
    logger.info(s"\t=======================Total: ${readableTime(b.connectDeserialize + b.disconnectSerialize)}")
    logger.info(s"\tDatabase Writes...............${readableTime(b.databaseWrite)}")
    logger.info(s"\tDatabase Reads................${readableTime(b.databaseRead)}")
    logger.info(s"\t=======================Total: ${readableTime(dbTime)} (CPU clock)")
    val cacheTotal = (b.cacheMisses + b.cacheHits).toDouble
    logger.info(s"\tCache Hits....................${b.cacheHits} / ${(b.cacheHits / cacheTotal) * 100}%")
    logger.info(s"\tCache Misses..................${b.cacheMisses} / ${(b.cacheMisses / cacheTotal) * 100}%")
    logger.info(s"")
  }

  def readableTime(nanoTime: Long): String = {
    val d = Duration.ofNanos(nanoTime)
    s"${d.toHoursPart}H ${d.toMinutesPart} min ${d.toSecondsPart} s ${d.toMillisPart} ms"
  }

}
