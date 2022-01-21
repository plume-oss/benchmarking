package com.github.plume.oss

import drivers._

object DriverUtil {

  def createDriver(config: DriverConfig): IDriver =
    config match {
      case c: TinkerGraphConfig =>
        val d = new TinkerGraphDriver()
        if (c.storageLocation.nonEmpty) d.importGraph(c.storageLocation)
        d
      case c: OverflowDbConfig =>
        new OverflowDbDriver(if (c.storageLocation.isBlank) None else Some(c.storageLocation),
                             c.setHeapPercentageThreshold,
                             c.setSerializationStatsEnabled)
      case c: NeptuneConfig =>
        new NeptuneDriver(c.hostname, c.port, c.keyCertChainFile)
      case c: Neo4jConfig =>
        new Neo4jDriver(c.hostname, c.port, c.username, c.password)
      case c: TigerGraphConfig =>
        new TigerGraphDriver(c.hostname, c.restPpPort, c.gsqlPort, c.username, c.password, secure = c.secure)
    }

}
