package com.teqbahn.actors.mailer

import org.apache.pekko.actor.{Actor, PoisonPill}
import com.teqbahn.caseclasses._
import com.teqbahn.common.mail.Mailer
import com.teqbahn.utils.ZiFunctions;

class MailActor extends Actor {
  override def postStop(): Unit = {
    ZiFunctions.printNodeInfo(self, "MailActor got PoisonPill")
  }

  override def preStart(): Unit = {
    ZiFunctions.printNodeInfo(self, "MailActor Started")
  }

  def receive: Receive = {
    case sendMailRequest: SendMailRequest =>
      val ccmailList = sendMailRequest.ccMailIds
      val mailContent = sendMailRequest.mailContent
      val mailSubject = sendMailRequest.subContent
      Mailer.sendMail(mailContent, mailSubject, sendMailRequest.email);
      ZiFunctions.printNodeInfo(self, "After Send Mail")
      self ! PoisonPill
  }
}
