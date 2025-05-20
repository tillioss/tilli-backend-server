package com.teqbahn.common.mail

import com.icegreen.greenmail.util.{GreenMail, ServerSetup}
import jakarta.mail.internet.AddressException
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Try

class MailerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  private val smtpPort = 3025
  private val testFrom = "sender@example.com"
  private val testPassword = "password123"
  private val testHost = "127.0.0.1"

  private var greenMail: GreenMail = _
  private var mailer: TestMailer = _

  private class TestMailer(fromEmail: String, password: String)
    extends Mailer(fromEmail, password) {
    override protected val host: String = testHost
    override protected val port: String = smtpPort.toString
  }

  override def beforeEach(): Unit = {
    val serverSetup = new ServerSetup(smtpPort, testHost, ServerSetup.PROTOCOL_SMTP)
    greenMail = new GreenMail(serverSetup)
    greenMail.start()
    greenMail.setUser(testFrom, testPassword)
    mailer = new TestMailer(testFrom, testPassword)
  }

  override def afterEach(): Unit = {
    greenMail.stop()
  }

  "Mailer" should "successfully send an email" in {
    val to = "recipient@example.com"
    val subject = "Test Subject"
    val content = "Test Content"

    noException should be thrownBy {
      mailer.sendMail(content, subject, to)
    }

    val receivedMessages = greenMail.getReceivedMessages
    receivedMessages.length shouldBe 1
    val message = receivedMessages(0)

    message.getSubject shouldBe subject
    message.getAllRecipients()(0).toString shouldBe to
    message.getContent.toString.trim shouldBe content
  }

  it should "throw an exception for invalid email address" in {
    val invalidEmail = "not-an-email"
    val subject = "Test Subject"
    val content = "Test Content"

    val result = Try(mailer.sendMail(content, subject, invalidEmail))
    result.isFailure shouldBe true
    result.failed.get shouldBe a[AddressException]
  }

  it should "handle SMTP server unavailability" in {
    greenMail.stop()

    val to = "recipient@example.com"
    val subject = "Test Subject"
    val content = "Test Content"

    val result = Try(mailer.sendMail(content, subject, to))
    result.isFailure shouldBe true
    result.failed.get shouldBe a[jakarta.mail.MessagingException]
  }

  it should "handle empty subject and content" in {
    val to = "recipient@example.com"
    val subject = ""
    val content = ""

    noException should be thrownBy {
      mailer.sendMail(content, subject, to)
    }

    val receivedMessages = greenMail.getReceivedMessages
    receivedMessages.length shouldBe 1
    val message = receivedMessages(0)

    message.getSubject shouldBe ""
    message.getContent.toString.trim shouldBe ""
  }
}
