package com.teqbahn.common.mail

import jakarta.mail.{Message, Session, Transport}
import jakarta.mail.internet.{InternetAddress, MimeMessage}

import java.util.Properties

class Mailer(fromEmail: String, password: String) {
  protected val host = "smtp.gmail.com"
  protected val port = "587"

  def sendMail(text: String, subject: String, address: String): Unit = {
    val properties = new Properties()
    properties.put("mail.smtp.port", port)
    properties.put("mail.smtp.auth", "true")
    properties.put("mail.smtp.starttls.enable", "true")

    val session = Session.getDefaultInstance(properties, null)
    val message = new MimeMessage(session)

    message.addRecipient(Message.RecipientType.TO, new InternetAddress(address, true))
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
