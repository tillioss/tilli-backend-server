import zio._
import zio.redis._
import zio.json._
import zio.console._
import java.sql.Timestamp
import java.util.Date
import java.text.SimpleDateFormat

// Define case classes
case class LogsData(logsJson: String, createdAt: Long)
object LogsData {
  implicit val logsDataCodec: JsonCodec[LogsData] = DeriveJsonCodec.gen[LogsData]
}

case class CaptureLogsRequestWrapper(id: String, captureLogsRequest: CaptureLogsRequest)
case class CaptureLogsRequest(logsJson: String)
case class CaptureLogsResponse(response: String)
case class GetWebLogsRequest(auth: String, noOfPage: Int, pageLimit: Int)
case class GetWebLogsResponse(resultData: Map[String, LogsData], totalSize: Long)

object GlobalMessageConstants {
  val SUCCESS = "SUCCESS"
  val FAILURE = "FAILURE"
  val AUTH_TEXT = "AUTH_TEXT"
}

// LogsActor Service
trait LogsActor {
  def handleCaptureLogs(request: CaptureLogsRequestWrapper): UIO[CaptureLogsResponse]
  def handleGetWebLogs(request: GetWebLogsRequest): UIO[GetWebLogsResponse]
}

object LogsActor {

  def live(redis: Redis): ZLayer[Any, Throwable, LogsActor] =
    ZLayer.fromFunction { (redis: Redis) =>
      new LogsActorImpl(redis)
    }
}

class LogsActorImpl(redis: Redis) extends LogsActor {

  def handleCaptureLogs(request: CaptureLogsRequestWrapper): UIO[CaptureLogsResponse] = {
    val createdAt = new Timestamp(new Date().getTime).getTime
    val logsData = LogsData(request.captureLogsRequest.logsJson, createdAt)
    val dateFormat = getDateFormatStr(createdAt)

    for {
      _ <- redis.hset("webLog", request.id, logsData.toJson)
      _ <- redis.lpush("webLogList", request.id)
      _ <- redis.lpush("webLogBasedOnDate", dateFormat, request.id)
    } yield CaptureLogsResponse(GlobalMessageConstants.SUCCESS)
  }

  def handleGetWebLogs(request: GetWebLogsRequest): UIO[GetWebLogsResponse] = {
    if (request.auth.equalsIgnoreCase(GlobalMessageConstants.AUTH_TEXT)) {
      val fromIndex = (request.noOfPage - 1) * request.pageLimit

      for {
        totalSize <- redis.llen("webLogList")
        resultData <- if (totalSize > 0 && totalSize > fromIndex) {
          val toIndex = Math.min((request.noOfPage * request.pageLimit), totalSize) - 1
          for {
            ids <- redis.lrange("webLogList", fromIndex, toIndex)
            logsDataStrs <- ZIO.foreach(ids)(id => redis.hget("webLog", id))
            logsDataMap = logsDataStrs.zip(ids).collect {
              case (Some(dataStr), id) => id -> dataStr.fromJson[LogsData].getOrElse(LogsData("", 0))
            }.toMap
          } yield (logsDataMap, totalSize)
        } else {
          ZIO.succeed((Map.empty[String, LogsData], totalSize))
        }
      } yield GetWebLogsResponse(resultData, totalSize)
    } else {
      ZIO.succeed(GetWebLogsResponse(Map.empty, 0))
    }
  }

  private def getDateFormatStr(time: Long): String = {
    val simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd")
    simpleDateFormat.format(time)
  }
}
