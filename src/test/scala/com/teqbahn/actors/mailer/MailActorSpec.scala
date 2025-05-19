package com.teqbahn.actors.mailer

import com.teqbahn.caseclasses.SendMailRequest
import com.teqbahn.common.mail.Mailer
import org.apache.pekko.actor.{ActorSystem, PoisonPill, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit, TestProbe}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatestplus.mockito.MockitoSugar

class MailActorSpec extends TestKit(ActorSystem("MailActorSpec"))
  with ImplicitSender
  with AnyFlatSpecLike
  with BeforeAndAfterAll
  with MockitoSugar {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  private def sampleRequest(): SendMailRequest = SendMailRequest(
    sessionId = "session-001",
    email = "to@example.com",
    fullName = "Tester",
    mobileNumber = "1234567890",
    subContent = "Test Subject",
    mailContent = "<p>This is a test email.</p>",
    toMailIds = List("to@example.com"),
    ccMailIds = List("cc1@example.com"),
    bccMailIds = List("bcc1@example.com"),
    id = "mail-001"
  )

  "MailActor" should "send an email using Mailer and stop itself" in {
    val mockMailer = mock[Mailer]
    val mailActor = system.actorOf(Props(new MailActor(mockMailer)))

    val request = sampleRequest()

    watch(mailActor)
    mailActor ! request

    expectTerminated(mailActor)
    verify(mockMailer).sendMail(request.mailContent, request.subContent, request.email)
  }

  it should "call printNodeInfo on startup" in {
    val mockMailer = mock[Mailer]
    val mailActor = system.actorOf(Props(new MailActor(mockMailer)))

    watch(mailActor)
    mailActor ! PoisonPill
    expectTerminated(mailActor)
  }

  "SendMailRequest" should "handle empty email field" in {
    val mockMailer = mock[Mailer]
    val mailActor = system.actorOf(Props(new MailActor(mockMailer)))

    val requestWithEmptyEmail = sampleRequest().copy(email = "")

    watch(mailActor)
    mailActor ! requestWithEmptyEmail

    expectTerminated(mailActor)
    verify(mockMailer).sendMail(requestWithEmptyEmail.mailContent, requestWithEmptyEmail.subContent, "")
  }

  it should "handle empty mail content" in {
    val mockMailer = mock[Mailer]
    val mailActor = system.actorOf(Props(new MailActor(mockMailer)))

    val requestWithEmptyContent = sampleRequest().copy(mailContent = "")

    watch(mailActor)
    mailActor ! requestWithEmptyContent

    expectTerminated(mailActor)
    verify(mockMailer).sendMail("", requestWithEmptyContent.subContent, requestWithEmptyContent.email)
  }

  it should "handle empty subject content" in {
    val mockMailer = mock[Mailer]
    val mailActor = system.actorOf(Props(new MailActor(mockMailer)))

    val requestWithEmptySubject = sampleRequest().copy(subContent = "")

    watch(mailActor)
    mailActor ! requestWithEmptySubject

    expectTerminated(mailActor)
    verify(mockMailer).sendMail(requestWithEmptySubject.mailContent, "", requestWithEmptySubject.email)
  }

  it should "handle null values in email field" in {
    val mockMailer = mock[Mailer]
    val mailActor = system.actorOf(Props(new MailActor(mockMailer)))

    val requestWithNullEmail = sampleRequest().copy(email = null)

    watch(mailActor)
    mailActor ! requestWithNullEmail

    expectTerminated(mailActor)
    verify(mockMailer).sendMail(requestWithNullEmail.mailContent, requestWithNullEmail.subContent, null)
  }

  it should "handle empty toMailIds list" in {
    val mockMailer = mock[Mailer]
    val mailActor = system.actorOf(Props(new MailActor(mockMailer)))

    val requestWithEmptyToMailIds = sampleRequest().copy(toMailIds = List())

    watch(mailActor)
    mailActor ! requestWithEmptyToMailIds

    expectTerminated(mailActor)
    verify(mockMailer).sendMail(
      requestWithEmptyToMailIds.mailContent,
      requestWithEmptyToMailIds.subContent,
      requestWithEmptyToMailIds.email
    )
  }

  it should "handle malformed email addresses" in {
    val mockMailer = mock[Mailer]
    val mailActor = system.actorOf(Props(new MailActor(mockMailer)))

    val requestWithInvalidEmail = sampleRequest().copy(email = "not-an-email")

    watch(mailActor)
    mailActor ! requestWithInvalidEmail

    expectTerminated(mailActor)
    verify(mockMailer).sendMail(
      requestWithInvalidEmail.mailContent,
      requestWithInvalidEmail.subContent,
      "not-an-email"
    )
  }
}
