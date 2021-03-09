package io.github.plume.oss

import drivers._
import metrics.{ExtractorTimeKey, PlumeTimer}
import util.ExtractorConst

import org.apache.logging.log4j.core.LoggerContext
import org.slf4j.{Logger, LoggerFactory}
import org.yaml.snakeyaml.Yaml

import java.io.{BufferedWriter, FileWriter, File => JavaFile}
import java.time.LocalDateTime
import scala.jdk.CollectionConverters
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
    val iterations: Int = config.getOrDefault("iterations", 5).asInstanceOf[Int]
    PrettyPrinter.setLogger(logger)
    PrettyPrinter.announcePlumeVersion()
    logger.info(s"Running $iterations of the experiments")
    val files = getFilesToBenchmarkAgainst(PROGRAMS_PATH)
    logger.info(s"Found ${files.length} files to benchmark against.")
    logger.debug(s"The files are: ${files.map(_.getName()).mkString(",")}")
    getDrivers(config).foreach {
      case (dbName, driver, containers) =>
        if (DockerManager.hasDockerDependency(dbName)) DockerManager.startDockerFile(dbName, containers)
        Using.resource(driver) { d =>
          handleConnection(d)
          handleSchema(d)
          for (i <- 1 to iterations) {
            val driverName = driver.getClass.toString.stripPrefix("io.github.plume.oss.drivers.")
            PrettyPrinter.announceIteration(i, driverName)
            files.foreach { f =>
              PrettyPrinter.announceBenchmark(f.getName)
              d.clearGraph()
              try {
                captureBenchmarkResult(runBenchmark(f, dbName, d))
              } catch {
                case e: Exception => logger.error("Encountered exception while performing benchmark. Skipping...", e)
              }
            }
          }
        }
        if (DockerManager.hasDockerDependency(dbName)) DockerManager.closeAnyDockerContainers(dbName)
    }
  }

  def handleSchema(driver: IDriver): Unit =
    driver match {
      case x: ISchemaSafeDriver =>
        logger.info(s"Building schema for $driver.")
        x.buildSchema()
      case _ =>
    }

  def handleConnection(driver: IDriver): Unit =
    driver match {
      case x: GremlinDriver    => x.connect()
      case y: OverflowDbDriver => y.connect()
      case z: Neo4jDriver      => z.connect()
      case _                   =>
    }

  def runBenchmark(f: JavaFile, dbName: String, driver: IDriver): BenchmarkResult = {
    val e = new Extractor(driver)
    logger.info("Loading file...")
    e.load(f)
    logger.info("Running base CPG passes...")
    e.project()
    logger.info("Running SCPG passes...")
    e.postProject()
    val times = PlumeTimer.INSTANCE.getTimes
    val b = BenchmarkResult(
      fileName = f.getName,
      database = dbName,
      loadingAndCompiling = times.get(ExtractorTimeKey.LOADING_AND_COMPILING),
      unitGraphBuilding = times.get(ExtractorTimeKey.UNIT_GRAPH_BUILDING),
      databaseWrite = times.get(ExtractorTimeKey.DATABASE_WRITE),
      databaseRead = times.get(ExtractorTimeKey.DATABASE_READ),
      scpgPasses = times.get(ExtractorTimeKey.SCPG_PASSES)
    )
    PrettyPrinter.announceResults(b)
    b
  }

  def captureBenchmarkResult(b: BenchmarkResult) {
    logger.info(s"Capturing benchmark for $b.")
    val csv = new JavaFile("./results.csv")
    if (!csv.exists()) {
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append(
          s"DATE,PLUME_VERSION,FILE_NAME,DATABASE,LOADING_AND_COMPILING,UNIT_GRAPH_BUILDING,DATABASE_WRITE,DATABASE_READ,SCPG_PASSES\n"
        )
      }
    }
    Using.resource(new BufferedWriter(new FileWriter(csv, true))) {
      _.append(
        s"${LocalDateTime.now()},${ExtractorConst.INSTANCE.getPlumeVersion},${b.fileName},${b.database},${b.loadingAndCompiling},${b.unitGraphBuilding},${b.databaseWrite},${b.databaseRead},${b.scpgPasses}\n"
      )
    }
  }

  def parseConfig(configPath: String): java.util.LinkedHashMap[String, Any] = {
    val config = new Yaml()
    Using.resource(getClass.getResourceAsStream(configPath)) { is =>
      return config.load(is).asInstanceOf[java.util.LinkedHashMap[String, Any]]
    }
  }

  def getDrivers(config: java.util.LinkedHashMap[String, Any]): List[(String, IDriver, List[String])] =
    config.getOrDefault("databases", {
      Map("conf0" -> Map("db" -> "tinkergraph", "enabled" -> "true"))
    }) match {
      case dbs: java.util.LinkedHashMap[String, Any] =>
        dbs
          .entrySet()
          .stream()
          .map {
            _.getValue.asInstanceOf[java.util.LinkedHashMap[String, Any]]
          }
          .map { dbConf: java.util.LinkedHashMap[String, Any] =>
            val dbName = dbConf.getOrDefault("db", "unknown").asInstanceOf[String]
            dbName match {
              case "TinkerGraph" =>
                (dbName,
                 DriverCreator.createTinkerGraphDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]])
              case "OverflowDB" =>
                (dbName,
                 DriverCreator.createOverflowDbDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]])
              case s"JanusGraph$_" =>
                (dbName,
                 DriverCreator.createJanusGraphDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]])
              case s"TigerGraph$_" =>
                (dbName,
                 DriverCreator.createTigerGraphDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]])
              case "Neo4j" =>
                (dbName,
                 DriverCreator.createNeo4jDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]])
              case "Neptune" =>
                (dbName,
                 DriverCreator.createNeptuneDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]])
              case _ => logger.warn(s"Database name '$dbName' not registered. "); null
            }
          }
          .toArray
          .toList
          .map {
            case (x, y, z) =>
              (x, y, CollectionConverters.ListHasAsScala(z.asInstanceOf[java.util.ArrayList[String]]).asScala.toList)
          }
          .asInstanceOf[List[(String, IDriver, List[String])]]
          .filterNot { tup: (String, IDriver, List[String]) =>
            tup == null || tup._2 == null
          }

      case _ => List.empty[(String, IDriver, List[String])]
    }

  def getFilesToBenchmarkAgainst(prefixPath: String): Array[JavaFile] =
    new JavaFile(getClass.getResource(prefixPath).getFile).listFiles()

}
