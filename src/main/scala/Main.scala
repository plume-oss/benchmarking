package com.github.plume.oss

import drivers._

import org.slf4j.{ Logger, LoggerFactory }
import org.yaml.snakeyaml.Yaml

import java.io.{ BufferedWriter, FileWriter, File => JavaFile }
import java.time.LocalDateTime
import java.util
import scala.jdk.CollectionConverters
import scala.language.postfixOps
import scala.util.Using

object Main extends App {

  lazy val logger: Logger = LoggerFactory.getLogger(Main.getClass)

  val experimentConfig = YamlDeserializer.experimentConfig("/experiments_conf.yaml")
  val driverConfigs = YamlDeserializer.driverConfig("/driver_conf.yaml").configs
  val datasetConfigs = YamlDeserializer.datasetConfig("/dataset_conf.yaml").configs

  PrettyPrinter.setLogger(logger)

  logger.info(s"Running ${experimentConfig.iterations} iterations of each benchmark")
  logger.info(s"Found ${datasetConfigs.size} programs to benchmark against ${driverConfigs.size} drivers.")
  logger.debug(s"The files are: ${datasetConfigs.map(_.name).mkString(",")}")

  driverConfigs.filter(_.enabled).foreach { driverConf =>
    for (i <- 1 to experimentConfig.iterations) {
      val driverName =
        driverConf.getClass.toString.stripPrefix("class com.github.plume.oss.Domain$").stripSuffix("Config")
      PrettyPrinter.announceIteration(i, driverName)

      datasetConfigs
        .filter(_.enabled)
        .map(Job(driverName, driverConf, _, experimentConfig))
        .foreach(runExperiment)
    }
  }

  /**
    * Runs through all the configured experiments.
    *
    * @return true if one of the jobs timed out, false if otherwise
    */
  def runExperiment(job: Job): Boolean =
    try {
      // Run build and export
      if (job.experiment.runBuildAndStore) {
        if (RunBenchmark.runBuildAndStore(job)) return true
      }
//      // Run live updates
//      if (job.experiment.runLiveUpdates) {
//        if (RunBenchmark.runLiveUpdates(job)) return true
//      }
//      // Run disconnected updates
//      if (job.experiment.runDisconnectedUpdates) {
//        if (RunBenchmark.runDisconnectedUpdates(job)) return true
//      }
//      // Run full builds
//      if (job.experiment.runFullBuilds) {
//        if (RunBenchmark.runFullBuilds(job)) return true
//      }
//      // Run Soot only builds
//      if (job.experiment.runSootOnlyBuilds) {
//        if (RunBenchmark.runBuildAndStore(job.copy(sootOnly = true))) return true
//      }
      false
    } catch {
      case e: Exception =>
        logger.error(e.getMessage, e)
        false
    }

}
