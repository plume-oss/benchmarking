package io.github.plume.oss

import Main.{DOCKER_PATH, logger}

import java.io.{File => JavaFile}
import scala.sys.process.{Process, ProcessLogger, stringSeqToProcess}
import scala.util.control.Breaks.{break, breakable}

object DockerManager {
  def hasDockerDependency(dbName: String): Boolean = getDockerComposeFiles.map {
    _.getName.contains(dbName)
  }.foldLeft(false)(_ || _)

  def toDockerComposeFile(dbName: String): JavaFile =
    new JavaFile(getClass.getResource(s"$DOCKER_PATH${JavaFile.separator}$dbName.yml").toURI)

  def closeAnyDockerContainers(dbName: String): Unit = {
    logger.info(s"Stopping Docker services for $dbName...")
    val dockerComposeUp = Process(Seq("docker-compose", "-f", toDockerComposeFile(dbName).getAbsolutePath, "down"))
    dockerComposeUp.run(ProcessLogger(_ => ()))
  }

  def startDockerFile(dbName: String): Unit = {
    logger.info(s"Docker Compose file found for $dbName, starting...")
    val dockerComposeUp = Process(Seq("docker-compose", "-f", toDockerComposeFile(dbName).getAbsolutePath, "up"))
    logger.info(s"Starting process $dockerComposeUp")
    dockerComposeUp.run(ProcessLogger(_ => ()))
    var status = false
    while (!status) {
      val healthCheck = Seq("docker", "inspect", "--format='{{json .State.Health}}'", "janusgraph-plume-benchmark")
      try {
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
      } catch {
        case e: StringIndexOutOfBoundsException => // Usually happens just as the services have been created
      }
      Thread.sleep(1000)
    }
  }

  def getDockerComposeFiles: Array[JavaFile] = new JavaFile(getClass.getResource(DOCKER_PATH).getFile).listFiles()
}
