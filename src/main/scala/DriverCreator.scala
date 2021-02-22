package io.github.plume.oss

import drivers.{ DriverFactory, GraphDatabase, OverflowDbDriver, TinkerGraphDriver }

import scala.reflect.io.File

object DriverCreator {

  def createTinkerGraphDriver(config: java.util.LinkedHashMap[String, Any]): TinkerGraphDriver =
    if (config.get("enabled") == true)
      DriverFactory.invoke(GraphDatabase.TINKER_GRAPH).asInstanceOf[TinkerGraphDriver]
    else
      null

  def createOverflowDbDriver(config: java.util.LinkedHashMap[String, Any]): OverflowDbDriver =
    if (config.get("enabled") == true) {
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
}
