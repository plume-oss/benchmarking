package com.github.plume.oss

import javax.mail.{ Message, Session }
import javax.mail.internet.{ InternetAddress, MimeMessage }
import scala.util.{ Try, Using }

object MailUtil {

  private val mailConfig = YamlDeserializer.emailConfig("/email_conf.yaml")

  private val props = System.getProperties
  props.put("mail.smtp.starttls.enable", "true")
  props.put("mail.smtp.host", mailConfig.host)
  props.put("mail.smtp.user", mailConfig.user)
  props.put("mail.smtp.password", mailConfig.password)
  props.put("mail.smtp.port", mailConfig.port)
  props.put("mail.smtp.auth", "true")
  private val session = Session.getDefaultInstance(props)

  def sendNotification(subject: String, messageBody: String): Try[Unit] =
    Try({
      if (mailConfig.enabled) {
        val message = new MimeMessage(session)
        message.setFrom(new InternetAddress(session.getProperty("mail.smtp.user")))
        message.addRecipients(Message.RecipientType.TO, mailConfig.recipient)
        message.setSubject(subject)
        message.setContent(s"$messageBody\nMachine ID: ${mailConfig.machineId}", "text/plain")

        Using.resource(session.getTransport) { t =>
          t.connect(
            session.getProperty("mail.smtp.host"),
            session.getProperty("mail.smtp.user"),
            session.getProperty("mail.smtp.password")
          )
          t.sendMessage(message, message.getAllRecipients)
        }
      }
    })

  def sendError(e: Exception): Try[Unit] =
    sendNotification(s"Plume Benchmark Error: ${e.getMessage}", e.getStackTrace.mkString("\n"))

}
