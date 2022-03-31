package com.github.plume.oss

import com.github.plume.oss.RunBenchmark.TaintAnalysisResult
import io.joern.dataflowengineoss.queryengine.ReachableByResult
import org.slf4j.{ Logger, LoggerFactory }

import java.time.Duration
import java.util.Optional

object PrettyPrinter {

  var logger: Logger = LoggerFactory.getLogger(PrettyPrinter.getClass)
  var currentIteration: Int = 0

  def setLogger(logger: Logger): Unit = this.logger = logger

  def announceIteration(i: Int, d: String): Unit = {
    currentIteration = i
    logger.info(s"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
    logger.info(
      s"$i${if (i % 10 == 1) "st" else if (i % 10 == 2) "nd" else if (i % 10 == 3) "rd" else "th"} iteration on driver $d"
    )
    logger.info(s"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
  }

  def announceBenchmark(name: String, commit: String, i: Int = 0, prefix: Char = 'I'): Unit = {
    logger.info(s"---------------------------------------------------------------------------")
    logger.info(s"[$currentIteration:$prefix$i] $name @ $commit")
    logger.info(s"---------------------------------------------------------------------------")
  }

  def announceResults(b: BenchmarkResult): Unit = {
    val totalTime = b.time
    logger.info(s"")
    logger.info(s"Benchmark results of ${b.fileName}")
    logger.info(s"")
    logger.info(s"PHASE ${b.phase}")
    logger.info(s"\tCPG Projection time...........${readableTime(totalTime)}")
    logger.info(s"\tConnect/Deserialize...........${readableTime(b.connectDeserialize)}")
    logger.info(s"\tDisconnect/Serialize..........${readableTime(b.disconnectSerialize)}")
    logger.info(s"\tProcessed Classes.............${b.programClasses}")
    logger.info(s"\tProcessed Methods (LIB/EXT)...(${b.programMethods}/${b.externalMethods})")
    logger.info(s"\tGraph Size (N/E)..............(${b.nodeCount}/${b.edgeCount})")
    logger.info(s"")
  }

  def announceTaintAnalysisResults(b: TaintAnalysisResult): Unit = {
    val totalTime = b.time
    logger.info(s"")
    logger.info(s"Taint analysis results")
    logger.info(s"")
    logger.info(s"\tTotal Duration................${readableTime(totalTime)}")
    logger.info(s"\tMatched Sources...............${b.sources}")
    logger.info(s"\tMatched Sinks.................${b.sinks}")
    logger.info(s"\tMatched Flows.................${b.flows}")
    logger.info(s"\tCache Hit Percentage..........${b.cacheHits.toDouble / (b.cacheHits + b.cacheMisses) * 100.0}")
    logger.info(s"")
  }

  def announceTaintConfig(taintConfig: TaintConfig): Unit = {
    logger.info(s"")
    logger.info(s"Taint analysis configuration")
    logger.info(s"")
    logger.info(s"\tNo. Sources..................${taintConfig.sources.size} types")
    logger.info(s"\tNo. Sinks....................${taintConfig.sinks.size} types")
    logger.info(s"\tNo. Sanitizers...............${taintConfig.sanitization.size} types")
    logger.info(s"")
  }

  case class PathLine(parentMethod: String,
                      code: String,
                      typeName: Optional[Object] = Optional.empty(),
                      nodeLine: Optional[Object] = Optional.empty(),
                      argIndex: Optional[Object] = Optional.empty())

  def showReachablePaths(results: List[ReachableByResult]): Unit = {
    import io.shiftleft.semanticcpg.language._

    if (results.isEmpty) logger.info("=== No paths to display ===")
    else logger.info("=== The following data flow paths were found to be reachable ===")

    results
      .map { result =>
        result.path.map(
          x =>
            PathLine(
              x.node.method.fullName.substring(0, x.node.method.fullName.lastIndexOf(':')),
              x.node.code,
              x.node.propertyOption("TYPE_FULL_NAME"),
              x.node.propertyOption("LINE_NUMBER"),
              x.node.propertyOption("ARGUMENT_INDEX")
          )
        )
      }
      .distinct
      .map { n: Vector[PathLine] =>
        n.map {
          case PathLine(methodName, code, typeName, lineNumber, argIndex) =>
            val sb = new StringBuilder()
            sb.append(s"\t[$methodName:")
            if (lineNumber.isPresent) {
              sb.append(lineNumber.get())
            }
            if (argIndex.isPresent) {
              sb.append(s" - arg ${argIndex.get()}")
            }
            sb.append("]")
            if (typeName.isPresent) {
              sb.append(s"(${typeName.get()})")
            }
            sb.append(s" $code")
            sb.toString()
        }
      }
      .zipWithIndex
      .foreach {
        case (path, i) =>
          logger.info(s"PATH $i:")
          path.foreach(logger.info)
      }
  }

  def readableTime(nanoTime: Long): String = {
    val d = Duration.ofNanos(nanoTime)
    s"${d.toHoursPart}H ${d.toMinutesPart} min ${d.toSecondsPart} s ${d.toMillisPart} ms"
  }

}
