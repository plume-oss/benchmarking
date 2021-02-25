package io.github.plume.oss

import drivers._

import org.apache.logging.log4j.core.LoggerContext
import org.slf4j.{Logger, LoggerFactory}
import org.yaml.snakeyaml.Yaml

import java.io.{BufferedWriter, FileWriter, File => JavaFile}
import java.time.LocalDateTime
import scala.sys.process.{Process, ProcessLogger, stringSeqToProcess}
import scala.util.Using
import scala.util.control.Breaks.{break, breakable}

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
      Using.resource(driver) { d =>
        files.foreach { f =>
          logger.info(s"$dbName $driver")
          logger.info(s"Running benchmark for ${f.getName} using driver ${driver.getClass}")
          handleDockerDependency(dbName)
          handleConnection(d)
          handleSchema(d)
          d.clearGraph()
          captureBenchmarkResult(runBenchmark(f, dbName, d))
        }
      }
    }
  }

  def handleSchema(driver: IDriver): Unit = {
    driver match {
      case x: ISchemaSafeDriver => x.buildSchema()
      case _ =>
    }
  }

  def handleConnection(driver: IDriver): Unit = {
    driver match {
      case x: GremlinDriver => x.connect()
      case y: OverflowDbDriver => y.connect()
      case z: Neo4jDriver => z.connect()
    }
  }

  def handleDockerDependency(dbName: String): Unit = {
    val hasDockerCompose = getDockerComposeFiles.map {
      _.getName.contains(dbName)
    }.foldLeft(false)(_ || _)
    if (hasDockerCompose) startDockerFile(dbName)
  }

  def startDockerFile(dbName: String): Unit = {
    logger.info(s"Docker Compose file found for $dbName, starting...")
    val dockerComposeFile = new JavaFile(getClass.getResource(s"$DOCKER_PATH${JavaFile.separator}$dbName.yml").toURI)
    val dockerComposeUp = Process(Seq("docker-compose", "-f", dockerComposeFile.getAbsolutePath, "up"))
    logger.info(s"Starting process ${dockerComposeUp}")
    dockerComposeUp.run(ProcessLogger(_ => ()))
    var status = false
    while (!status) {
      val healthCheck = Seq("docker", "inspect", "--format='{{json .State.Health}}'", "janusgraph-plume-benchmark")
      val rawResponse = healthCheck.lazyLines
      breakable {
        for (x <- rawResponse) {
          val jsonRaw = x.substring(1, x.length - 1)
          io.circe.parser.parse(jsonRaw) match {
            case Left(failure) => logger.warn(failure.message, failure.underlying)
            case Right(json) =>
              json.\\("Status").head.asString match {
                case Some("unhealthy") => logger.info("Container is unhealthy")
                case Some("starting") => logger.info("Container is busy starting")
                case Some("healthy") => logger.info("Container is healthy! Proceeding...")
                  status = true
                  break
              }
          }
        }
      }
      Thread.sleep(1000)
    }
  }

  def getDockerComposeFiles: Array[JavaFile] = new JavaFile(getClass.getResource(DOCKER_PATH).getFile).listFiles()

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
            val dbName = configs.getOrDefault("db", "unknown").asInstanceOf[String]
            dbName match {
              case "tinkergraph" =>
                Tuple2(dbName, DriverCreator.createTinkerGraphDriver(configs))
              case "overflowdb" =>
                Tuple2(dbName, DriverCreator.createOverflowDbDriver(configs))
              case s"janus$x" => Tuple2(dbName, DriverCreator.createJanusGraphDriver(configs))
              case "unknown" => logger.warn(s"No database specified for configuration $config."); null
              case _ => logger.warn(s"Database name '${dbName}' not registered. "); null
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
