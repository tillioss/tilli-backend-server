package com.teqbahn.common.mail

import com.teqbahn.bootstrap.StarterMain

import java.util.Properties
import javax.mail.{Message, Session}
import javax.mail.internet.{InternetAddress, MimeMessage}

object Mailer {
  val host = "smtp.gmail.com"
  val port = "587"


  def sendMail(text:String, subject:String, address : String) = {
    val properties = new Properties()
    properties.put("mail.smtp.port", port)
    properties.put("mail.smtp.auth", "true")
    properties.put("mail.smtp.starttls.enable", "true")

    val session = Session.getDefaultInstance(properties, null)
    val message = new MimeMessage(session)
    message.addRecipient(Message.RecipientType.TO, new InternetAddress(address));
    message.setSubject(subject)
    message.setContent(text, "text/html")

    val transport = session.getTransport("smtp")
    transport.connect(host, StarterMain.fromMail, StarterMain.fromMailPassword)
    transport.sendMessage(message, message.getAllRecipients)
  }
}
