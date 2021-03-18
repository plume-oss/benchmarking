package io.github.plume.oss

import drivers._
import metrics.{ExtractorTimeKey, PlumeTimer}
import util.ExtractorConst

import org.slf4j.{Logger, LoggerFactory}
import org.yaml.snakeyaml.Yaml

import java.io.{BufferedWriter, FileWriter, File => JavaFile}
import java.time.LocalDateTime
import java.util
import scala.jdk.CollectionConverters
import scala.util.Using

object Main extends App {

  lazy val logger: Logger = LoggerFactory.getLogger(Main.getClass)
  val CONFIG_PATH = "../../../../config.yaml"
  val PROGRAMS_PATH = "../../../../programs"
  val DOCKER_PATH = "../../../../docker"

  val config: util.LinkedHashMap[String, Any] = parseConfig(CONFIG_PATH)
  val iterations: Int = config.getOrDefault("iterations", 5).asInstanceOf[Int]
  PrettyPrinter.setLogger(logger)
  PrettyPrinter.announcePlumeVersion()
  logger.info(s"Running $iterations iterations of each benchmark")
  val files: Array[JavaFile] = getFilesToBenchmarkAgainst(PROGRAMS_PATH)
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
            try {
              d.clearGraph()
              captureBenchmarkResult(runBenchmark(f, dbName, d))
            } catch {
              case e: Exception => logger.error("Encountered exception while performing benchmark. Skipping...", e)
            }
          }
        }
      }
      if (DockerManager.hasDockerDependency(dbName)) DockerManager.closeAnyDockerContainers(dbName)
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
    new Extractor(driver).load(f).project()
    val times = PlumeTimer.INSTANCE.getTimes
    val b = BenchmarkResult(
      fileName = f.getName,
      database = dbName,
      compilingAndUnpacking = times.get(ExtractorTimeKey.COMPILING_AND_UNPACKING),
      soot = times.get(ExtractorTimeKey.SOOT),
      programStructureBuilding = times.get(ExtractorTimeKey.PROGRAM_STRUCTURE_BUILDING),
      baseCpgBuilding = times.get(ExtractorTimeKey.BASE_CPG_BUILDING),
      databaseWrite = times.get(ExtractorTimeKey.DATABASE_WRITE),
      databaseRead = times.get(ExtractorTimeKey.DATABASE_READ),
      dataFlowPasses = times.get(ExtractorTimeKey.DATA_FLOW_PASS)
    )
    PlumeTimer.INSTANCE.reset()
    PrettyPrinter.announceResults(b)
    b
  }

  def captureBenchmarkResult(b: BenchmarkResult) {
    val csv = new JavaFile("./results.csv")
    if (!csv.exists()) {
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append(
          "DATE," +
            "PLUME_VERSION," +
            "FILE_NAME," +
            "DATABASE," +
            "COMPILING_AND_UNPACKING," +
            "SOOT," +
            "PROGRAM_STRUCTURE_BUILDING," +
            "BASE_CPG_BUILDING," +
            "DATABASE_WRITE," +
            "DATABASE_READ," +
            "DATA_FLOW_PASS\n"
        )
      }
    }
    Using.resource(new BufferedWriter(new FileWriter(csv, true))) {
      _.append(
        s"${LocalDateTime.now()}," +
          s"${ExtractorConst.INSTANCE.getPlumeVersion}," +
          s"${b.fileName}," +
          s"${b.database}," +
          s"${b.compilingAndUnpacking}," +
          s"${b.soot}," +
          s"${b.programStructureBuilding}," +
          s"${b.baseCpgBuilding}," +
          s"${b.databaseWrite}," +
          s"${b.databaseRead}," +
          s"${b.dataFlowPasses}\n"
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
    config.getOrDefault(
      "databases", {
        CollectionConverters
          .MapHasAsJava(
            Map("conf0" -> CollectionConverters.MapHasAsJava(Map("db" -> "tinkergraph", "enabled" -> "true")).asJava)
          )
          .asJava
      }
    ) match {
      case dbs: java.util.Map[String, Any] =>
        dbs
          .entrySet()
          .stream()
          .map {
            _.getValue.asInstanceOf[java.util.Map[String, Any]]
          }
          .map { dbConf: java.util.Map[String, Any] =>
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
