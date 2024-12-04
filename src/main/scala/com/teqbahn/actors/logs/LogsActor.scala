package com.teqbahn.actors.logs

import org.apache.pekko.actor.{Actor, ActorRef}
import com.teqbahn.bootstrap.StarterMain.redisCommands
import com.teqbahn.caseclasses.{AddToAccumulationRequest, CaptureLogsRequestWrapper, CaptureLogsResponse, GetWebLogsRequest, GetWebLogsResponse, LogsData}
import com.teqbahn.global.{GlobalMessageConstants, ZiRedisCons}
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization.{read, write}
import org.json4s.native.Serialization

import java.sql.Timestamp
import java.util.Date
import scala.collection.immutable.ListMap
import scala.collection.mutable.ListBuffer

class LogsActor extends Actor {
  implicit val formats = Serialization.formats(NoTypeHints)

  import scala.collection.JavaConverters._

  def receive = {


    case captureLogsRequestWrapper: CaptureLogsRequestWrapper => {
      var captureLogsRequest = captureLogsRequestWrapper.captureLogsRequest

      var reponse = GlobalMessageConstants.FAILURE

      val createdAt = new Timestamp((new Date).getTime).getTime

      var logsData = LogsData(captureLogsRequest.logsJson, createdAt)
      redisCommands.hset(ZiRedisCons.LOGS_webLog, captureLogsRequestWrapper.id, write(logsData))
      redisCommands.lpush(ZiRedisCons.LOGS_webLogList, captureLogsRequestWrapper.id)
      redisCommands.lpush(ZiRedisCons.LOGS_webLogBasedOnDate, getDateFormateStr(createdAt), captureLogsRequestWrapper.id)

      reponse = GlobalMessageConstants.SUCCESS

      sender() ! CaptureLogsResponse(reponse)


    }
    case getWebLogsRequest: GetWebLogsRequest =>
      var resultData: ListMap[String, LogsData] = ListMap.empty
      var totalSize: Long = 0
      if (getWebLogsRequest.auth.equalsIgnoreCase(GlobalMessageConstants.AUTH_TEXT)) {
        val fromIndex = (getWebLogsRequest.noOfPage - 1) * getWebLogsRequest.pageLimit;
        totalSize = redisCommands.llen(ZiRedisCons.LOGS_webLogList)
        if (totalSize > 0) {
          if (totalSize > fromIndex) {
            val lisOfIds = redisCommands.lrange(ZiRedisCons.LOGS_webLogList, fromIndex, Math.min((getWebLogsRequest.noOfPage * getWebLogsRequest.pageLimit), totalSize) - 1).asScala.toList
            for (logId <- lisOfIds) {

              val logsDataStr = redisCommands.hget(ZiRedisCons.LOGS_webLog, logId)
              val logsData = read[LogsData](logsDataStr)
              resultData += (logId -> logsData)


            }
          }
        }
      }
      sender() ! GetWebLogsResponse(resultData, totalSize)


  }

  def getDateFormateStr(time: Long): String = {
    import java.text.SimpleDateFormat
    val simpleDateFormat = new SimpleDateFormat("YYY-MM-dd")
    val format = simpleDateFormat.format(time).toString
    format
    /* val formatArray = format.split("-")
     formatArray*/
    // AccumulationDate(formatArray(0), formatArray(0)+ "-"+ formatArray(1), format)
  }


}
