package com.teqbahn.actors.mailer

import org.apache.pekko.actor.{Actor, PoisonPill}
import com.teqbahn.caseclasses._
import com.teqbahn.common.mail.Mailer
import com.teqbahn.utils.ZiFunctions;

class MailActor(mailer: Mailer) extends Actor {
  override def postStop(): Unit = {
    ZiFunctions.printNodeInfo(self, "MailActor got PoisonPill")
  }

  override def preStart(): Unit = {
    ZiFunctions.printNodeInfo(self, "MailActor Started")
  }

  def receive: Receive = {
    case sendMailRequest: SendMailRequest =>
      mailer.sendMail(sendMailRequest.mailContent, sendMailRequest.subContent, sendMailRequest.email);
      ZiFunctions.printNodeInfo(self, "After Send Mail")
      self ! PoisonPill
  }
}
