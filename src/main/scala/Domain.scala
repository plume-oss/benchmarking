package com.github.plume.oss

import net.jcazevedo.moultingyaml.{
  deserializationError,
  DefaultYamlProtocol,
  YamlArray,
  YamlBoolean,
  YamlFormat,
  YamlNumber,
  YamlObject,
  YamlString,
  YamlValue
}
import java.io.{ File => JFile }

abstract class DriverConfig {
  def enabled: Boolean
}

case class DriverConfigurations(configs: List[DriverConfig])

case class OverflowDbConfig(enabled: Boolean,
                            storageLocation: String,
                            setOverflow: Boolean,
                            setHeapPercentageThreshold: Int,
                            setSerializationStatsEnabled: Boolean)
    extends DriverConfig
case class TinkerGraphConfig(enabled: Boolean, storageLocation: String) extends DriverConfig
case class NeptuneConfig(enabled: Boolean, hostname: String, port: Int, keyCertChainFile: String) extends DriverConfig
case class Neo4jConfig(enabled: Boolean,
                       hostname: String,
                       port: Int,
                       username: String,
                       password: String)
    extends DriverConfig
case class TigerGraphConfig(enabled: Boolean,
                            username: String,
                            password: String,
                            hostname: String,
                            restPpPort: Int,
                            gsqlPort: Int,
                            secure: Boolean)
    extends DriverConfig

case class DatasetConfig(
    enabled: Boolean,
    name: String,
    jars: Seq[JFile]
)

case class DatasetConfigurations(configs: List[DatasetConfig])

case class ExperimentConfig(
    iterations: Int,
    timeout: Int,
    runBuildAndStore: Boolean,
    runLiveUpdates: Boolean,
    runDisconnectedUpdates: Boolean,
    runFullBuilds: Boolean,
    runSootOnlyBuilds: Boolean,
)

object PlumeBenchmarkProtocol extends DefaultYamlProtocol {

  val PROGRAMS_PATH = "/programs"

  implicit object DatasetConfigFormat extends YamlFormat[DatasetConfig] {
    override def read(yaml: YamlValue): DatasetConfig =
      yaml.asYamlObject.getFields(
        YamlString("enabled"),
        YamlString("name"),
        YamlString("jars"),
      ) match {
        case Seq(
            YamlBoolean(enabled),
            YamlString(name),
            YamlArray(jars),
            ) =>
          DatasetConfig(
            enabled,
            name,
            jars
              .map(_.convertTo[String])
              .map(x => s"$PROGRAMS_PATH/$name/$x.jar")
              .map { jarPath =>
                new JFile(getClass.getResource(jarPath).getFile)
              }
              .reverse
          )

        case _ => deserializationError("DatasetConfig expected")
      }

    override def write(o: DatasetConfig): YamlValue = YamlObject()
  }

  implicit object DriverConfigFormat extends YamlFormat[DriverConfig] {

    override def read(yaml: YamlValue): DriverConfig =
      yaml.asYamlObject.getFields(
        YamlString("db"),
        YamlString("enabled"),
        YamlString("properties"),
      ) match {
        case Seq(
            YamlString(db),
            YamlBoolean(enabled),
            YamlObject(properties),
            ) =>
          db match {
            case "OverflowDB" =>
              OverflowDbConfig(
                enabled,
                properties.getOrElse(YamlString("storageLocation"), "cpg.odb").asInstanceOf[YamlString].value,
                properties.getOrElse(YamlString("setOverflow"), true).asInstanceOf[YamlBoolean].boolean,
                properties.getOrElse(YamlString("setHeapPercentageThreshold"), 80).asInstanceOf[YamlNumber].value.toInt,
                properties
                  .getOrElse(YamlString("setSerializationStatsEnabled"), true)
                  .asInstanceOf[YamlBoolean]
                  .boolean,
              )
            case "TinkerGraph" =>
              TinkerGraphConfig(
                enabled,
                properties.getOrElse(YamlString("storageLocation"), "cpg.kryo").asInstanceOf[YamlString].value
              )
            case "Neptune" =>
              NeptuneConfig(
                enabled,
                properties.getOrElse(YamlString("hostname"), "127.0.0.1").asInstanceOf[YamlString].value,
                properties.getOrElse(YamlString("port"), 8182).asInstanceOf[YamlNumber].value.toInt,
                properties
                  .getOrElse(YamlString("keyCertChainFile"), "src/main/resources/conf/SFSRootCAG2.pem")
                  .asInstanceOf[YamlString]
                  .value,
              )
            case "Neo4j" =>
              Neo4jConfig(
                enabled,
                properties.getOrElse(YamlString("hostname"), "127.0.0.1").asInstanceOf[YamlString].value,
                properties.getOrElse(YamlString("port"), 7687).asInstanceOf[YamlNumber].value.toInt,
                properties.getOrElse(YamlString("username"), "neo4j").asInstanceOf[YamlString].value,
                properties.getOrElse(YamlString("password"), "neo4j").asInstanceOf[YamlString].value,
                properties.getOrElse(YamlString("database"), "neo4j").asInstanceOf[YamlString].value,
              )
            case "TigerGraph" =>
              TigerGraphConfig(
                enabled,
                properties.getOrElse(YamlString("username"), "tigergraph").asInstanceOf[YamlString].value,
                properties.getOrElse(YamlString("password"), "tigergraph").asInstanceOf[YamlString].value,
                properties.getOrElse(YamlString("hostname"), "127.0.0.1").asInstanceOf[YamlString].value,
                properties.getOrElse(YamlString("restPpPort"), 9000).asInstanceOf[YamlNumber].value.toInt,
                properties.getOrElse(YamlString("restPpPort"), 14240).asInstanceOf[YamlNumber].value.toInt,
                properties.getOrElse(YamlString("secure"), false).asInstanceOf[YamlBoolean].boolean,
              )
            case _ => deserializationError("OverflowDB, TinkerGraph, Neptune, Neo4j, or TigerGraph expected")
          }
        case x => deserializationError(s"DriverConfig expected ${x.toSeq}")
      }

    override def write(o: DriverConfig): YamlValue = YamlObject()
  }

  implicit object ExperimentConfigFormat extends YamlFormat[ExperimentConfig] {
    override def read(yaml: YamlValue): ExperimentConfig =
      yaml.asYamlObject.getFields(
        YamlString("iterations"),
        YamlString("timeout"),
        YamlString("runBuildAndStore"),
        YamlString("runLiveUpdates"),
        YamlString("runDisconnectedUpdates"),
        YamlString("runFullBuilds"),
        YamlString("runSootOnlyBuilds")
      ) match {
        case Seq(
            YamlNumber(iterations),
            YamlNumber(timeout),
            YamlBoolean(runBuildAndStore),
            YamlBoolean(runLiveUpdates),
            YamlBoolean(runDisconnectedUpdates),
            YamlBoolean(runFullBuilds),
            YamlBoolean(runSootOnlyBuilds)
            ) =>
          ExperimentConfig(iterations.toInt,
                           timeout.toInt,
                           runBuildAndStore,
                           runLiveUpdates,
                           runDisconnectedUpdates,
                           runFullBuilds,
                           runSootOnlyBuilds)
        case _ => deserializationError("ExperimentConfig expected")
      }

    override def write(o: ExperimentConfig): YamlValue = YamlObject(
      YamlString("iterations") -> YamlNumber(o.iterations),
      YamlString("timeout") -> YamlNumber(o.timeout),
      YamlString("runBuildAndStore") -> YamlBoolean(o.runBuildAndStore),
      YamlString("runLiveUpdates") -> YamlBoolean(o.runLiveUpdates),
      YamlString("runDisconnectedUpdates") -> YamlBoolean(o.runDisconnectedUpdates),
      YamlString("runFullBuilds") -> YamlBoolean(o.runFullBuilds),
      YamlString("runSootOnlyBuilds") -> YamlBoolean(o.runSootOnlyBuilds),
    )
  }
  implicit val driverConfigsFormat = yamlFormat1(DriverConfigurations)
  implicit val datasetConfigsFormat = yamlFormat1(DatasetConfigurations)
}

case class Job(driver: DriverConfig, program: DatasetConfig, sootOnly: Boolean = false)

case class BenchmarkResult(
    fileName: String,
    phase: String,
    database: String,
    timedOut: Boolean = false,
    time: Long = -1L,
    connectDeserialize: Long = -1L,
    disconnectSerialize: Long = -1L
) {

  override def toString: String =
    s"BenchmarkResult { " +
      s"fileName=$fileName, " +
      s"database=$database, " +
      s"compilingAndUnpacking=${time * Math.pow(10, -9)}s, " +
      s"connectDeserialize=${connectDeserialize}s, " +
      s"disconnectSerialize=${disconnectSerialize}s " +
      "}"
}
