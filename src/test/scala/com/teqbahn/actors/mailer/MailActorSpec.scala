import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.environment._
import zio.console.Console
import zio.test.mock._
import com.teqbahn.actors.mailer.MailActor
import com.teqbahn.actors.mailer.MailActor.SendEmail
import com.teqbahn.caseclasses.EmailRequest
import zio.test.mock.Proxy
import zio.test.mock.Mockable
import zio.test.mock.Mock

object MailActorSpec extends DefaultRunnableSpec {

  // Mocked service to replace real dependencies
  trait MockMailService {
    def sendEmail(request: EmailRequest): UIO[Unit]
  }

  object MockMailService {
    def mock: ULayer[MockMailService] = ZLayer.succeed(new MockMailService {
      def sendEmail(request: EmailRequest): UIO[Unit] = UIO.unit
    })
  }

  def spec: Spec[Environment with Console with TestEnvironment, Any] = suite("MailActorSpec")(
    testM("should send email correctly") {
      // Arrange
      val emailRequest = EmailRequest("to@example.com", "Subject", "Body")
      val mockMailService = MockMailService.mock
      val mailActor = new MailActor(mockMailService)

      // Act
      val result = mailActor.sendEmail(emailRequest).provideLayer(mockMailService)

      // Assert
      assertM(result)(equalTo(()))
    }
  )
}
