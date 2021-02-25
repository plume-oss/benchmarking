package io.github.plume.oss

import drivers._

import scala.reflect.io.File

object DriverCreator {

  def createTinkerGraphDriver(config: java.util.LinkedHashMap[String, Any]): TinkerGraphDriver =
    if (config.getOrDefault("enabled", false) == true)
      DriverFactory.invoke(GraphDatabase.TINKER_GRAPH).asInstanceOf[TinkerGraphDriver]
    else
      null

  def createOverflowDbDriver(config: java.util.LinkedHashMap[String, Any]): OverflowDbDriver =
    if (config.getOrDefault("enabled", false) == true) {
      val driver = DriverFactory.invoke(GraphDatabase.OVERFLOWDB).asInstanceOf[OverflowDbDriver]
      val storageLoc = config.getOrDefault("storage", "/tmp/plume/cpg.bin").asInstanceOf[String]
      File(storageLoc).delete()
      driver.setStorageLocation(storageLoc)
      driver.setOverflow(config.getOrDefault("set_overflow", true).asInstanceOf[Boolean])
      driver.setHeapPercentageThreshold(config.getOrDefault("set_heap_percentage_threshold", 80).asInstanceOf[Int])
      driver.setSerializationStatsEnabled(
        config.getOrDefault("set_serialization_stats_enabled", false).asInstanceOf[Boolean]
      )
      driver
    } else
      null

  def createJanusGraphDriver(config: java.util.LinkedHashMap[String, Any]): JanusGraphDriver =
    if (config.getOrDefault("enabled", false) == true) {
      val driver = DriverFactory.invoke(GraphDatabase.JANUS_GRAPH).asInstanceOf[JanusGraphDriver]
      driver.remoteConfig(config.get("remote_config").asInstanceOf[String])
      driver
    } else
      null

  def createNeptuneDriver(config: java.util.LinkedHashMap[String, Any]): NeptuneDriver =
    if (config.getOrDefault("enabled", false) == true) {
      val driver = DriverFactory.invoke(GraphDatabase.NEPTUNE).asInstanceOf[NeptuneDriver]
      driver.addHostnames(config.getOrDefault("hostnames", List("127.0.0.1")).asInstanceOf[List[String]]: _*)
        .keyCertChainFile(config.getOrDefault("key_cert_chain_file", "src/test/resources/conf/SFSRootCAG2.pem").asInstanceOf[String])
        .port(config.getOrDefault("port", 8182).asInstanceOf[Int])
      driver
    } else
      null

  def createTigerGraphDriver(config: java.util.LinkedHashMap[String, Any]): TigerGraphDriver =
    if (config.getOrDefault("enabled", false) == true) {
      val driver = DriverFactory.invoke(GraphDatabase.TIGER_GRAPH).asInstanceOf[TigerGraphDriver]
      driver.hostname(config.getOrDefault("hostname", "127.0.0.1").asInstanceOf[String])
        .username(config.getOrDefault("username", "tigergraph").asInstanceOf[String])
        .password(config.getOrDefault("password", "tigergraph").asInstanceOf[String])
        .secure(config.getOrDefault("secure", false).asInstanceOf[Boolean])
        .authKey(config.getOrDefault("auth_key", "").asInstanceOf[String])
        .restPpPort(config.getOrDefault("restPpPort", 9000).asInstanceOf[Int])
        .gsqlPort(config.getOrDefault("gsqlPort", 14240).asInstanceOf[Int])
    } else
      null

  def createNeo4jDriver(config: java.util.LinkedHashMap[String, Any]): Neo4jDriver =
    if (config.getOrDefault("enabled", false) == true) {
      val driver = DriverFactory.invoke(GraphDatabase.NEO4J).asInstanceOf[Neo4jDriver]
      driver.hostname(config.getOrDefault("hostname", "127.0.0.1").asInstanceOf[String])
        .username(config.getOrDefault("username", "neo4j").asInstanceOf[String])
        .password(config.getOrDefault("password", "neo4j123").asInstanceOf[String])
        .database(config.getOrDefault("database", "neo4j").asInstanceOf[String])
        .port(config.getOrDefault("port", 7687).asInstanceOf[Int])
    } else
      null
}
