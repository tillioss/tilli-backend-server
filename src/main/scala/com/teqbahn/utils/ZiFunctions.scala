import zio._
import zio.clock.Clock
import zio.random.Random
import java.sql.Timestamp
import java.util.{Date, UUID}
import org.json4s._
import org.json4s.jackson.JsonMethods._

object ZiFunctions {

  // ZIO Effect to get a UUID
  def getId: UIO[String] = UIO.effectTotal(UUID.randomUUID.toString)

  // ZIO Effect to get the current time
  def getCreatedAt: UIO[Long] = ZIO.effectTotal(new Timestamp(new Date().getTime).getTime)

  // ZIO Effect to convert JSON string to Map
  def jsonStrToMap(jsonStr: String): UIO[Map[String, Any]] = ZIO.effectTotal {
    implicit val formats: Formats = DefaultFormats
    parse(jsonStr).extract[Map[String, Any]]
  }

  // Example usage of ZIO's clock to print node info (customized as needed)
  def printNodeInfo(message: String): UIO[Unit] = for {
    currentTime <- zio.clock.currentDateTime
    _ <- console.putStrLn(s"At: $currentTime, Msg: $message")
  } yield ()
}
