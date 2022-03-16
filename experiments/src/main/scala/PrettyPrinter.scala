package com.github.plume.oss

import org.slf4j.{Logger, LoggerFactory}

import java.time.Duration

object PrettyPrinter {

  var logger: Logger = LoggerFactory.getLogger(PrettyPrinter.getClass)

  def setLogger(logger: Logger): Unit = this.logger = logger

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
    val totalTime = b.time
    logger.info(s"")
    logger.info(s"Benchmark results of ${b.fileName}")
    logger.info(s"")
    logger.info(s"PHASE ${b.phase}")
    logger.info(s"\tCPG Projection time...........${readableTime(totalTime)} (wall clock)")
    logger.info(s"\tConnect/Deserialize...........${readableTime(b.connectDeserialize)}")
    logger.info(s"\tDisconnect/Serialize..........${readableTime(b.disconnectSerialize)}")
    logger.info(s"\tProgram Classes...............${b.programClasses}")
    logger.info(s"\tProgram Methods...............${b.programMethods}")
    logger.info(s"\tChanged Classes...............${b.changedClasses}")
    logger.info(s"\tChanged Methods...............${b.changedMethods}")
    logger.info(s"")
  }

  def readableTime(nanoTime: Long): String = {
    val d = Duration.ofNanos(nanoTime)
    s"${d.toHoursPart}H ${d.toMinutesPart} min ${d.toSecondsPart} s ${d.toMillisPart} ms"
  }

}
