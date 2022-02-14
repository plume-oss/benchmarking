package com.github.plume.oss

import javax.mail.{ Message, Session }
import javax.mail.internet.{ InternetAddress, MimeMessage }
import scala.util.{ Try, Using }

object MailUtil {

  private val mailConfig = YamlDeserializer.emailConfig("/email_conf.yaml")
  println(mailConfig)

  private val props = System.getProperties
  props.put("mail.smtp.starttls.enable", "true")
  props.put("mail.smtp.host", mailConfig.host)
  props.put("mail.smtp.user", mailConfig.user)
  props.put("mail.smtp.password", mailConfig.password)
  props.put("mail.smtp.port", mailConfig.port)
  props.put("mail.smtp.auth", "true")
  private val session = Session.getDefaultInstance(props)

  def sendNotification(messageBody: String): Try[Unit] =
    Try({
      if (mailConfig.enabled) {
        val message = new MimeMessage(session)
        message.setFrom(new InternetAddress(session.getProperty("mail.smtp.user")))
        message.addRecipients(Message.RecipientType.TO, mailConfig.recipient)
        message.setSubject("Plume Benchmark Complete")
        message.setContent(messageBody, "text/plain")

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

}
