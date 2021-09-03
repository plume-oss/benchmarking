package io.github.plume.oss

import drivers._
import metrics.{CacheMetrics, DriverTimeKey, ExtractorTimeKey, PlumeTimer}
import options.CacheOptions
import store.LocalCache
import util.{ExtractorConst, ResourceCompilationUtil}

import org.slf4j.{Logger, LoggerFactory}
import org.yaml.snakeyaml.Yaml

import java.io.{BufferedWriter, FileWriter, File => JavaFile}
import java.time.LocalDateTime
import java.util
import scala.jdk.CollectionConverters
import scala.language.postfixOps
import scala.tools.nsc
import scala.util.Using

object Main extends App {

  lazy val logger: Logger = LoggerFactory.getLogger(Main.getClass)
  val CONFIG_PATH = "/config.yaml"
  val PROGRAMS_PATH = "/programs"
  val DOCKER_PATH = "/docker"
  val FILE_PREF = s"${ResourceCompilationUtil.INSTANCE.getTEMP_DIR}${nsc.io.File.separator}cpg-benchmark"
  io.github.plume.oss.util.PlumeKeyProvider.INSTANCE.setKeyPoolSize(4000000)
  val config: util.LinkedHashMap[String, Any] = parseConfig(CONFIG_PATH)
  val iterations: Int = config.getOrDefault("iterations", 5).asInstanceOf[Int]
  RunBenchmark.timeout = config.getOrDefault("timeout", 5 * 60).asInstanceOf[Int].toLong // Default 5 hours
  PrettyPrinter.setLogger(logger)
  PrettyPrinter.announcePlumeVersion()

  logger.info(s"Running $iterations iterations of each benchmark")
  val experiment: Experiment = getExperiment(config)
  val programs: List[Program] = getPrograms(config)
  val drivers = getDrivers(config)
  CacheOptions.INSTANCE.setCacheSize(config.getOrDefault("cache-size", 100000L).toString.toLong)

  logger.info(s"Found ${programs.length} programs to benchmark against ${drivers.length} drivers.")
  logger.debug(s"The files are: ${programs.map(_.name).mkString(",")}")
  drivers.foreach {
    case (dbName, driver, containers, conf) =>
      if (conf.getOrDefault("use-docker", false).asInstanceOf[Boolean] &&
          DockerManager.hasDockerDependency(dbName)) {
        DockerManager.startDockerFile(dbName, containers)
      }
      try {
        openConnection(driver)
        handleSchema(driver)
        for (i <- 1 to iterations) {
          val driverName = driver.getClass.toString.stripPrefix("class io.github.plume.oss.drivers.")
          PrettyPrinter.announceIteration(i, driverName)
          programs.map(Job(driver, _, dbName)).foreach { job =>
            try {
              runExperiment(job)
            } catch {
              case e: Exception => logger.error("Encountered exception while performing benchmark. Skipping...", e)
            } finally {
              LocalCache.INSTANCE.clear()
            }
          }
        }
      } finally {
        closeConnection(driver)
      }
      if (conf.getOrDefault("use-docker", false).asInstanceOf[Boolean] &&
          DockerManager.hasDockerDependency(dbName)) {
        DockerManager.closeAnyDockerContainers(dbName)
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
      if (experiment.runBuildAndStore) {
        if (RunBenchmark.runBuildAndStore(job)) return true
      }
      // Run live updates
      if (experiment.runLiveUpdates) {
        if (RunBenchmark.runLiveUpdates(job)) return true
      }
      // Run disconnected updates
      if (experiment.runDisconnectedUpdates) {
        if (RunBenchmark.runDisconnectedUpdates(job)) return true
      }
      // Run full builds
      if (experiment.runFullBuilds) {
        if (RunBenchmark.runFullBuilds(job)) return true
      }
      false
    } catch {
      case e: Exception =>
        logger.error(e.getMessage, e)
        false
    }

  def handleSchema(driver: IDriver): Unit =
    driver match {
      case x: ISchemaSafeDriver =>
        logger.info(s"Building schema for $driver.")
        x.buildSchema()
      case _ =>
    }

  def openConnection(driver: IDriver): Unit =
    driver match {
      case x: GremlinDriver    => if (!x.getConnected) x.connect()
      case y: OverflowDbDriver => if (!y.getConnected$plume) y.connect()
      case z: Neo4jDriver      => if (!z.getConnected) z.connect()
      case _                   =>
    }

  def closeConnection(driver: IDriver): Unit =
    driver match {
      case x: GremlinDriver    => if (x.getConnected) x.close()
      case y: OverflowDbDriver => if (y.getConnected$plume) y.close()
      case z: Neo4jDriver      => if (z.getConnected) z.close()
      case _                   =>
    }

  def clearSerializedFiles(): Unit = {
    val kryo = new JavaFile(FILE_PREF + ".kryo")
    val bin = new JavaFile(FILE_PREF + ".bin")
    try {
      kryo.delete()
    } catch {
      case _: Exception =>
    }
    try {
      bin.delete()
    } catch {
      case _: Exception =>
    }
  }

  def openConnectionAndConfigure(driver: IDriver): Unit =
    driver match {
      case w: TinkerGraphDriver =>
        if (w.getConnected) w.close()
        w.connect()
        try {
          w.importGraph(FILE_PREF + ".kryo")
        } catch {
          case _: Exception => logger.debug("TinkerGraph export does not exist yet.")
        }
      case x: GremlinDriver =>
        if (x.getConnected) x.close()
        x.connect()
      case y: OverflowDbDriver =>
        if (y.getConnected$plume) y.close()
        y.storageLocation(FILE_PREF + ".bin")
        y.connect()
      case z: Neo4jDriver =>
        if (z.getConnected) z.close()
        z.connect()
      case _ =>
    }

  def closeConnectionWithExport(driver: IDriver): Unit =
    driver match {
      case w: TinkerGraphDriver =>
        try {
          w.exportGraph(FILE_PREF + ".kryo")
        } catch {
          case _: Exception => logger.debug("TinkerGraph export does not exist yet.")
        } finally {
          w.close()
        }
      case x: GremlinDriver    => x.close()
      case y: OverflowDbDriver => y.close()
      case z: Neo4jDriver      => z.close()
      case _                   =>
    }

  def runBenchmark(f: JavaFile, name: String, phase: String, dbName: String, driver: IDriver): BenchmarkResult = {
    PrettyPrinter.announceBenchmark(name, f.getName.stripSuffix(".jar"))
    new Extractor(driver).load(f).project()
    val extractorTimes = PlumeTimer.INSTANCE.getExtractorTimes
    val driverTimes = PlumeTimer.INSTANCE.getDriverTimes
    val b = BenchmarkResult(
      fileName = name,
      phase = phase,
      database = dbName,
      compilingAndUnpacking = extractorTimes.get(ExtractorTimeKey.COMPILING_AND_UNPACKING),
      soot = extractorTimes.get(ExtractorTimeKey.SOOT),
      programStructureBuilding = extractorTimes.get(ExtractorTimeKey.PROGRAM_STRUCTURE_BUILDING),
      baseCpgBuilding = extractorTimes.get(ExtractorTimeKey.BASE_CPG_BUILDING),
      databaseWrite = driverTimes.get(DriverTimeKey.DATABASE_WRITE),
      databaseRead = driverTimes.get(DriverTimeKey.DATABASE_READ),
      dataFlowPasses = extractorTimes.get(ExtractorTimeKey.DATA_FLOW_PASS),
      cacheHits = CacheMetrics.INSTANCE.getHits,
      cacheMisses = CacheMetrics.INSTANCE.getMisses,
      connectDeserialize = driverTimes.get(DriverTimeKey.CONNECT_DESERIALIZE),
      disconnectSerialize = driverTimes.get(DriverTimeKey.DISCONNECT_SERIALIZE)
    )
    PlumeTimer.INSTANCE.reset()
    CacheMetrics.INSTANCE.reset()
    PrettyPrinter.announceResults(b)
    b
  }

  def captureBenchmarkResult(b: BenchmarkResult): BenchmarkResult = {
    val csv = new JavaFile("./results/result.csv")
    if (!csv.exists()) {
      new JavaFile("./results/").mkdir()
      csv.createNewFile()
      Using.resource(new BufferedWriter(new FileWriter(csv))) {
        _.append(
          "DATE," +
            "PLUME_VERSION," +
            "FILE_NAME," +
            "PHASE," +
            "DATABASE," +
            "COMPILING_AND_UNPACKING," +
            "SOOT," +
            "PROGRAM_STRUCTURE_BUILDING," +
            "BASE_CPG_BUILDING," +
            "DATABASE_WRITE," +
            "DATABASE_READ," +
            "DATA_FLOW_PASS," +
            "CACHE_HITS," +
            "CACHE_MISSES," +
            "CONNECT_DESERIALIZE," +
            "DISCONNECT_SERIALIZE" +
            "\n"
        )
      }
    }
    Using.resource(new BufferedWriter(new FileWriter(csv, true))) {
      _.append(
        s"${LocalDateTime.now()}," +
          s"${ExtractorConst.INSTANCE.getPlumeVersion}," +
          s"${b.fileName}," +
          s"${b.phase}," +
          s"${b.database}," +
          s"${b.compilingAndUnpacking}," +
          s"${b.soot}," +
          s"${b.programStructureBuilding}," +
          s"${b.baseCpgBuilding}," +
          s"${b.databaseWrite}," +
          s"${b.databaseRead}," +
          s"${b.dataFlowPasses}," +
          s"${b.cacheHits}," +
          s"${b.cacheMisses}," +
          s"${b.connectDeserialize}," +
          s"${b.disconnectSerialize}\n"
      )
    }
    b
  }

  def parseConfig(configPath: String): java.util.LinkedHashMap[String, Any] = {
    val config = new Yaml()
    Using.resource(getClass.getResourceAsStream(configPath)) { is =>
      return config.load(is).asInstanceOf[java.util.LinkedHashMap[String, Any]]
    }
  }

  def getDrivers(
      config: java.util.LinkedHashMap[String, Any]
  ): List[(String, IDriver, List[String], java.util.LinkedHashMap[String, Any])] =
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
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]],
                 dbConf)
              case "OverflowDB" =>
                (dbName,
                 DriverCreator.createOverflowDbDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]],
                 dbConf)
              case s"JanusGraph$_" =>
                (dbName,
                 DriverCreator.createJanusGraphDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]],
                 dbConf)
              case s"TigerGraph$_" =>
                (dbName,
                 DriverCreator.createTigerGraphDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]],
                 dbConf)
              case "Neo4j" =>
                (dbName,
                 DriverCreator.createNeo4jDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]],
                 dbConf)
              case "Neptune" =>
                (dbName,
                 DriverCreator.createNeptuneDriver(dbConf),
                 dbConf.getOrDefault("containers", new java.util.ArrayList()).asInstanceOf[java.util.ArrayList[String]],
                 dbConf)
              case _ => logger.warn(s"Database name '$dbName' not registered. "); null
            }
          }
          .toArray
          .toList
          .map {
            case (x, y, z, u) =>
              (x, y, CollectionConverters.ListHasAsScala(z.asInstanceOf[java.util.ArrayList[String]]).asScala.toList, u)
          }
          .asInstanceOf[List[(String, IDriver, List[String], java.util.LinkedHashMap[String, Any])]]
          .filterNot { tup: (String, IDriver, List[String], java.util.LinkedHashMap[String, Any]) =>
            tup == null || tup._2 == null
          }

      case _ => List.empty[(String, IDriver, List[String], java.util.LinkedHashMap[String, Any])]
    }

  def getPrograms(config: util.LinkedHashMap[String, Any]): List[Program] = {
    val ps = config.get("programs").asInstanceOf[java.util.LinkedHashMap[String, Any]]
    ps.entrySet()
      .stream()
      .map {
        _.getValue.asInstanceOf[java.util.Map[String, Any]]
      }
      .filter { pConf: java.util.Map[String, Any] =>
        pConf.getOrDefault("enabled", false).asInstanceOf[Boolean]
      }
      .map { pConf =>
        val listOfJars = CollectionConverters
          .ListHasAsScala(
            pConf
              .getOrDefault("jars", new util.ArrayList[String]())
              .asInstanceOf[util.ArrayList[String]]
          )
          .asScala
          .map { f =>
            new JavaFile(getClass.getResource(s"$PROGRAMS_PATH/$f.jar").getFile)
          }
          .reverse
          .toList
        Program(
          name = pConf.get("name").asInstanceOf[String],
          jars = listOfJars
        )
      }
      .toArray
      .toList
      .asInstanceOf[List[Program]]
  }

  def getExperiment(config: util.LinkedHashMap[String, Any]): Experiment =
    Experiment(
      runBuildAndStore = config
        .get("experiment")
        .asInstanceOf[util.LinkedHashMap[String, Any]]
        .getOrDefault("run-build-and-store", false)
        .asInstanceOf[Boolean],
      runLiveUpdates = config
        .get("experiment")
        .asInstanceOf[util.LinkedHashMap[String, Any]]
        .getOrDefault("run-updates", false)
        .asInstanceOf[Boolean],
      runDisconnectedUpdates = config
        .get("experiment")
        .asInstanceOf[util.LinkedHashMap[String, Any]]
        .getOrDefault("run-disconnected-updates", false)
        .asInstanceOf[Boolean],
      runFullBuilds = config
        .get("experiment")
        .asInstanceOf[util.LinkedHashMap[String, Any]]
        .getOrDefault("run-full-builds", false)
        .asInstanceOf[Boolean]
    )

}
