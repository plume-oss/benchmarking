package io.github.plume.oss

import drivers.{ DriverFactory, GraphDatabase, IDriver, OverflowDbDriver, TinkerGraphDriver }

import org.yaml.snakeyaml.Yaml

import java.io.File
import scala.util.Using

object Main {

  val CONFIG_PATH = "../../../../config.yaml"
  val PROGRAMS_PATH = "../../../../programs"

  def main(args: Array[String]): Unit = {
    val config = parseConfig(CONFIG_PATH)
    getDrivers(config).foreach(println)
    val files = getFilesToBenchmarkAgainst(PROGRAMS_PATH)
    files.foreach(it => {
      println(it)
    })
  }

  def parseConfig(configPath: String): java.util.LinkedHashMap[String, Any] = {
    val config = new Yaml()
    Using.resource(getClass.getResourceAsStream(configPath)) { is =>
      return config.load(is).asInstanceOf[java.util.LinkedHashMap[String, Any]]
    }
  }

  def getDrivers(config: java.util.LinkedHashMap[String, Any]): List[IDriver] =
    config.getOrDefault("databases", { Map("tinkergraph" -> Map("enabled" -> "true")) }) match {
      case dbs: java.util.LinkedHashMap[String, Any] =>
        dbs
          .keySet()
          .toArray
          .map {
            case "tinkergraph" =>
              if (dbs.get("tinkergraph").asInstanceOf[java.util.LinkedHashMap[String, Any]].get("enabled") == true)
                DriverFactory.invoke(GraphDatabase.TINKER_GRAPH).asInstanceOf[TinkerGraphDriver]
            case "overflowdb" =>
              if (dbs.get("overflowdb").asInstanceOf[java.util.LinkedHashMap[String, Any]].get("enabled") == true)
                DriverFactory.invoke(GraphDatabase.OVERFLOWDB).asInstanceOf[OverflowDbDriver]
            case _ => null
          }
          .filterNot(_ == null)
          .toList
          .asInstanceOf[List[IDriver]]

      case _ => List.empty[IDriver]
    }

  def getFilesToBenchmarkAgainst(prefixPath: String): Array[File] =
    new File(getClass.getResource(prefixPath).getFile).listFiles()

}
