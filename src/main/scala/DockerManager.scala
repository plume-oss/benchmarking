package io.github.plume.oss

import Main.{DOCKER_PATH, logger}

import java.io.{File => JavaFile}
import scala.sys.process.{Process, ProcessLogger, stringSeqToProcess}

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
      Thread.sleep(2500)
      val healthCheck = Seq("docker", "inspect", "--format='{{json .State.Health}}'", dbName)
      try {
        val x: String = healthCheck.!!.trim
        val jsonRaw = x.substring(1, x.length - 1)
        io.circe.parser.parse(jsonRaw) match {
          case Left(failure) => logger.warn(failure.message, failure.underlying)
          case Right(json) =>
            json.\\("Status").head.asString match {
              case Some("unhealthy") => logger.info("Container is unhealthy")
              case Some("starting") => logger.info("Container is busy starting")
              case Some("healthy") => logger.info("Container is healthy! Proceeding..."); status = true
              case Some(x) =>  logger.info(s"Container is $x")
              case None => logger.warn("Unable to obtain container health.")
            }
        }
      } catch {
        case _: StringIndexOutOfBoundsException => // Usually happens just as the services have been created
        case e: IllegalAccessError => e.printStackTrace()
      }
    }
  }

  def getDockerComposeFiles: Array[JavaFile] = new JavaFile(getClass.getResource(DOCKER_PATH).getFile).listFiles()
}
