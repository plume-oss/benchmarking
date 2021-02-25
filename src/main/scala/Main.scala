package io.github.plume.oss

import drivers._

import org.apache.logging.log4j.core.LoggerContext
import org.slf4j.{Logger, LoggerFactory}
import org.yaml.snakeyaml.Yaml

import java.io.{BufferedWriter, FileWriter, File => JavaFile}
import java.time.LocalDateTime
import scala.util.Using

object Main {

  lazy val logger: Logger = LoggerFactory.getLogger(Main.getClass)
  val LOG4J2_XML = "../../../../log4j2.xml"
  val CONFIG_PATH = "../../../../config.yaml"
  val PROGRAMS_PATH = "../../../../programs"
  val DOCKER_PATH = "../../../../docker"

  def main(args: Array[String]): Unit = {
    import org.apache.logging.log4j.LogManager
    val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
    context.setConfigLocation(getClass.getResource(LOG4J2_XML).toURI)
    val config = parseConfig(CONFIG_PATH)

    val files = getFilesToBenchmarkAgainst(PROGRAMS_PATH)
    logger.info(s"Found ${files.length} files to benchmark against.")
    logger.debug(s"The files are: ${files.map(_.getName()).mkString(",")}")
    getDrivers(config).foreach { case (dbName, driver) =>
      if (DockerManager.hasDockerDependency(dbName)) DockerManager.startDockerFile(dbName)
      Using.resource(driver) { d =>
        handleConnection(d)
        handleSchema(d)
        files.foreach { f =>
          logger.info(s"$dbName $driver")
          logger.info(s"Running benchmark for ${f.getName} using driver ${driver.getClass}")
          d.clearGraph()
          captureBenchmarkResult(runBenchmark(f, dbName, d))
        }
      }
      if (DockerManager.hasDockerDependency(dbName)) DockerManager.closeAnyDockerContainers(dbName)
    }
  }

  def handleSchema(driver: IDriver): Unit = {
    driver match {
      case x: ISchemaSafeDriver =>
        logger.info(s"Building schema for $driver.")
        x.buildSchema()
      case _ =>
    }
  }

  def handleConnection(driver: IDriver): Unit = {
    driver match {
      case x: GremlinDriver => x.connect()
      case y: OverflowDbDriver => y.connect()
      case z: Neo4jDriver => z.connect()
      case _ =>
    }
  }

  def runBenchmark(f: JavaFile, dbName: String, driver: IDriver): BenchmarkResult = {
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
    val csv = new JavaFile("./results.csv")
    if (!csv.exists()) {
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append(s"date,fileName,database,loadingAndCompiling,buildSoot,buildPasses,\n")
      }
    }
    Using.resource(new BufferedWriter(new FileWriter(csv, true))) {
      _.append(s"${LocalDateTime.now()},${b.fileName},${b.database},${b.loadingAndCompiling},${b.buildSoot},${b.buildPasses},\n")
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
            val dbName = configs.getOrDefault("db", "unknown").asInstanceOf[String]
            dbName match {
              case "tinkergraph" =>
                Tuple2(dbName, DriverCreator.createTinkerGraphDriver(configs))
              case "overflowdb" =>
                Tuple2(dbName, DriverCreator.createOverflowDbDriver(configs))
              case s"janus$_" => Tuple2(dbName, DriverCreator.createJanusGraphDriver(configs))
              case "tigergraph" => Tuple2(dbName, DriverCreator.createTigerGraphDriver(configs))
              case "neo4j" => Tuple2(dbName, DriverCreator.createNeo4jDriver(configs))
              case "neptune" => Tuple2(dbName, DriverCreator.createNeptuneDriver(configs))
              case "unknown" => logger.warn(s"No database specified for configuration $config."); null
              case _ => logger.warn(s"Database name '$dbName' not registered. "); null
            }
          }.toArray
          .toList
          .asInstanceOf[List[(String, IDriver)]]
          .filterNot { tup: (String, IDriver) => tup == null || tup._2 == null }

      case _ => List.empty[(String, IDriver)]
    }
  }


  def getFilesToBenchmarkAgainst(prefixPath: String): Array[JavaFile] =
    new JavaFile(getClass.getResource(prefixPath).getFile).listFiles()

}
