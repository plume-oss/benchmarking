package com.github.plume.oss

import drivers._

import com.github.plume.oss.domain.DataFlowCacheConfig

import scala.reflect.io.Path

object DriverUtil {

  def createDriver(config: DriverConfig, reuseCache: Boolean = true): IDriver =
    config match {
      case c: TinkerGraphConfig =>
        val d = new TinkerGraphDriver()
        if (!c.storageLocation.isBlank && Path.apply(c.storageLocation).exists) d.importGraph(c.storageLocation)
        d
      case c: OverflowDbConfig =>
        new OverflowDbDriver(
          if (c.storageLocation.isBlank) None else Some(c.storageLocation),
          c.setHeapPercentageThreshold,
          c.setSerializationStatsEnabled,
          DataFlowCacheConfig(
            if (reuseCache) c.dataFlowCacheFile else None,
            c.compressDataFlowCache,
            maxCachedPaths = c.maxCachedPaths
          )
        )
      case c: NeptuneConfig =>
        new NeptuneDriver(c.hostname, c.port, c.keyCertChainFile)
      case c: Neo4jConfig =>
        new Neo4jDriver(c.hostname, c.port, c.username, c.password)
      case c: TigerGraphConfig =>
        new TigerGraphDriver(c.hostname, c.restPpPort, c.gsqlPort, c.username, c.password)
    }

  def handleSchema(d: IDriver): Unit =
    d match {
      case x: ISchemaSafeDriver =>
        com.github.plume.oss.Main.logger.info("Building schema...")
        x.buildSchema()
      case _ =>
    }
}
