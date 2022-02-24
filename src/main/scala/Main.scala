package com.github.plume.oss

import com.github.plume.oss.drivers.ISchemaSafeDriver
import org.slf4j.{ Logger, LoggerFactory }

import scala.language.postfixOps

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
    driverConf match {
      case c: TigerGraphConfig => DriverUtil.createDriver(c).asInstanceOf[ISchemaSafeDriver].buildSchema()
      case c: Neo4jConfig      => DriverUtil.createDriver(c).asInstanceOf[ISchemaSafeDriver].buildSchema()
      case _                   =>
    }
    for (i <- 1 to experimentConfig.iterations) {
      val driverName =
        driverConf.getClass.toString.stripPrefix("class com.github.plume.oss.").stripSuffix("Config")
      PrettyPrinter.announceIteration(i, driverName)

      datasetConfigs
        .filter(_.enabled)
        .map(Job(driverName, driverConf, _, experimentConfig))
        .foreach(runExperiment)
    }
  }

  private val mailBody =
    s"""
    |Benchmark measuring:
    |${driverConfigs
         .filter(_.enabled)
         .map(_.getClass.toString.stripPrefix("class com.github.plume.oss.").stripSuffix("Config"))
         .map(x => s"\t* $x")
         .mkString("\n")}
    |With config:
    |__________________________________________________________
    |\titerations             | ${experimentConfig.iterations}
    |\ttimeout                | ${experimentConfig.timeout}
    |\trunBuildAndStore       | ${experimentConfig.runBuildAndStore}
    |\trunLiveUpdates         | ${experimentConfig.runLiveUpdates}
    |\trunDisconnectedUpdates | ${experimentConfig.runDisconnectedUpdates}
    |\trunFullBuilds          | ${experimentConfig.runFullBuilds}
    |__________________________________________________________
    |is complete.
    |""".stripMargin

  MailUtil.sendNotification("Plume Benchmark Complete", mailBody)

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
      // Run live updates
      if (job.experiment.runLiveUpdates) {
        if (RunBenchmark.runLiveUpdates(job)) return true
      }
      // Run disconnected updates
      if (job.experiment.runDisconnectedUpdates) {
        if (RunBenchmark.runDisconnectedUpdates(job)) return true
      }
      // Run full builds
      if (job.experiment.runFullBuilds) {
        if (RunBenchmark.runFullBuilds(job)) return true
      }
      // Run Soot only builds
      if (job.experiment.runSootOnlyBuilds) {
        if (RunBenchmark.runBuildAndStore(job)) return true
      }
      false
    } catch {
      case e: Exception =>
        MailUtil.sendError(e)
        logger.error(e.getMessage, e)
        e.printStackTrace()
        false
    }

}
