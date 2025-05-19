package com.teqbahn.common.mail

import java.util.Properties
import javax.mail.{Message, Session, Transport}
import javax.mail.internet.{InternetAddress, MimeMessage}

class Mailer(fromEmail: String, password: String) {
  private val host = "smtp.gmail.com"
  private val port = "587"

  def sendMail(text: String, subject: String, address: String): Unit = {
    val properties = new Properties()
    properties.put("mail.smtp.port", port)
    properties.put("mail.smtp.auth", "true")
    properties.put("mail.smtp.starttls.enable", "true")

    val session = Session.getDefaultInstance(properties, null)
    val message = new MimeMessage(session)

    message.addRecipient(Message.RecipientType.TO, new InternetAddress(address))
    message.setSubject(subject)
    message.setContent(text, "text/html")

    val transport: Transport = session.getTransport("smtp")
    try {
      transport.connect(host, fromEmail, password)
      transport.sendMessage(message, message.getAllRecipients)
    } finally {
      transport.close()
    }
  }
}
