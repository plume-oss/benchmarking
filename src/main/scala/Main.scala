package io.github.plume.oss

import drivers.IDriver

import org.apache.logging.log4j.core.LoggerContext
import org.slf4j.{Logger, LoggerFactory}
import org.yaml.snakeyaml.Yaml

import java.io.{BufferedWriter, File, FileWriter}
import java.time.LocalDateTime
import scala.util.Using

object Main {

  lazy val logger: Logger = LoggerFactory.getLogger(Main.getClass)
  val LOG4J2_XML = "../../../../log4j2.xml"
  val CONFIG_PATH = "../../../../config.yaml"
  val PROGRAMS_PATH = "../../../../programs"

  def main(args: Array[String]): Unit = {
    import org.apache.logging.log4j.LogManager
    val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
    context.setConfigLocation(getClass.getResource(LOG4J2_XML).toURI)
    val config = parseConfig(CONFIG_PATH)

    val files = getFilesToBenchmarkAgainst(PROGRAMS_PATH)
    logger.info(s"Found ${files.length} files to benchmark against.")
    logger.debug(s"The files are: ${files.map(_.getName()).mkString(",")}")
    files.foreach(f => {
      getDrivers(config).foreach { case (dbName, driver) =>
        logger.info(s"Running benchmark for ${f.getName} using driver ${driver.getClass}")
        Using.resource(driver) { d =>
          captureBenchmarkResult(runBenchmark(f, dbName, d))
        }
      }
    })
  }

  def runBenchmark(f: File, dbName: String, driver: IDriver): BenchmarkResult = {
    val e = new Extractor(driver)
    val s1 = System.nanoTime()
    logger.info("Loading file...")
    e.load(f)
    val t1 = System.nanoTime() - s1
    val s2 = System.nanoTime()
    logger.info("Initial Soot build...")
    e.project()
    val t2 = System.nanoTime() - s2
    val s3 = System.nanoTime()
    logger.info("Running internal passes...")
    e.postProject()
    val t3 = System.nanoTime() - s3
    BenchmarkResult(f.getName, dbName, t1, t2, t3)
  }

  def captureBenchmarkResult(b: BenchmarkResult) {
    logger.info(s"Capturing benchmark for $b.")
    val csv = new File("./results.csv")
    if (!csv.exists()) {
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append(s"date,fileName,loadingAndCompiling,buildPasses,buildSoot,\n")
      }
    }
    Using.resource(new BufferedWriter(new FileWriter(csv, true))) {
      _.append(s"${LocalDateTime.now()},${b.fileName},${b.loadingAndCompiling},${b.buildPasses},${b.buildSoot},\n")
    }
  }

  def parseConfig(configPath: String): java.util.LinkedHashMap[String, Any] = {
    val config = new Yaml()
    Using.resource(getClass.getResourceAsStream(configPath)) { is =>
      return config.load(is).asInstanceOf[java.util.LinkedHashMap[String, Any]]
    }
  }

  def getDrivers(config: java.util.LinkedHashMap[String, Any]): List[(String, IDriver)] = {
    config.getOrDefault("databases", {
      Map("conf0" -> Map("db" -> "tinkergraph", "enabled" -> "true"))
    }) match {
      case dbs: java.util.LinkedHashMap[String, Any] =>
        dbs
          .entrySet().stream().map {
          _.getValue.asInstanceOf[java.util.LinkedHashMap[String, Any]]
        }
          .map { configs: java.util.LinkedHashMap[String, Any] =>
            val dbName = configs.getOrDefault("db", "unknown")
            dbName match {
              case "tinkergraph" =>
                Tuple2(dbName, DriverCreator.createTinkerGraphDriver(configs))
              case "overflowdb" =>
                Tuple2(dbName, DriverCreator.createOverflowDbDriver(configs))
              case "unknown" =>
                logger.warn(s"No database specified for configuration $config.")
                null
            }
          }.toArray.filterNot(_ == null)
          .toList
          .asInstanceOf[List[(String, IDriver)]]

      case _ => List.empty[(String, IDriver)]
    }
  }


  def getFilesToBenchmarkAgainst(prefixPath: String): Array[File] =
    new File(getClass.getResource(prefixPath).getFile).listFiles()

}
