package com.github.plume.oss
import Main.logger
import PlumeBenchmarkProtocol._

import net.jcazevedo.moultingyaml._

import scala.io.Source
import scala.util.{Failure, Success, Try}

object YamlDeserializer {

  def datasetConfig(config: String): DatasetConfigurations =
    fileToContents(config).parseYaml.convertTo[DatasetConfigurations]

  def experimentConfig(config: String): ExperimentConfig =
    fileToContents(config).parseYaml.convertTo[ExperimentConfig]

  def driverConfig(config: String): DriverConfigurations =
    fileToContents(config).parseYaml.convertTo[DriverConfigurations]

  def emailConfig(config: String): EmailConfig =
    fileToContents(config).parseYaml.convertTo[EmailConfig]

  def taintDefsConfig(config: String): TaintConfig =
    fileToContents(config).parseYaml.convertTo[TaintConfig]

  private def fileToContents(path: String): String =
    Try(getClass.getResourceAsStream(path)) match {
      case Failure(exception) => logger.error("Unable to load file contents!"); throw exception
      case Success(resource)  => Source.fromInputStream(resource).getLines.mkString("\n")
    }

}
