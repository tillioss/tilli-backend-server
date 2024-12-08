package com.teqbahn.actors.logs

import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import com.teqbahn.caseclasses._
import com.teqbahn.global.{GlobalMessageConstants, ZiRedisCons}
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, NoTypeHints}
import io.lettuce.core.api.sync.RedisCommands

import scala.collection.immutable.ListMap
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.matchers.should.Matchers

class LogsActorSpec 
    extends TestKit(ActorSystem("LogsActorSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  implicit val formats = Serialization.formats(NoTypeHints)

  // Mock Redis commands
  val mockRedis = mock(classOf[RedisCommands[String, String]])
  
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "LogsActor" must {
    
    "handle CaptureLogsRequestWrapper" in {
      // Arrange
      val logsJsonData = LogsJsonData(
        page = "test-page",
        action = "test-action",
        userAgent = "test-user-agent",
        ipAddress = "127.0.0.1",
        timestamp = "2024-01-01"
      )
      val id = "test-id-1"
      val captureLogsRequest = CaptureLogsRequest(
        reqId = id,
        logsJson = logsJsonData
      )
      val wrapper = CaptureLogsRequestWrapper(id, captureLogsRequest)
      
      // Mock Redis operations - Updated return types to Boolean for Lettuce
      when(mockRedis.hset(anyString(), anyString(), anyString())).thenReturn(true)
      when(mockRedis.lpush(anyString(), any[String]())).thenReturn(1L)
      
      val logsActor = system.actorOf(Props(new LogsActor {
        override protected def redisCommands: RedisCommands[String, String] = mockRedis
      }))

      // Act
      logsActor ! wrapper

      // Assert
      expectMsg(CaptureLogsResponse(GlobalMessageConstants.SUCCESS))
      
      verify(mockRedis).hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.LOGS_webLog),
        org.mockito.ArgumentMatchers.eq(id),
        anyString()
      )
      verify(mockRedis).lpush(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.LOGS_webLogList),
        org.mockito.ArgumentMatchers.eq(id)
      )
    }

    "handle GetWebLogsRequest with valid auth" in {
      // Arrange
      val request = GetWebLogsRequest(
        reqId = "test-req-id",
        auth = GlobalMessageConstants.AUTH_TEXT,
        noOfPage = 1,
        pageLimit = 10,
        totalResult = 0L
      )
      
      val mockIds = java.util.Arrays.asList("id1", "id2")
      val mockLogsData = LogsData(
        LogsJsonData(
          page = "test-page",
          action = "test-action",
          userAgent = "test-user-agent",
          ipAddress = "127.0.0.1",
          timestamp = "2024-01-01"
        ),
        System.currentTimeMillis()
      )
      val mockLogsDataStr = Serialization.write(mockLogsData)

      when(mockRedis.llen(ZiRedisCons.LOGS_webLogList)).thenReturn(2L)
      when(mockRedis.lrange(anyString(), anyLong(), anyLong())).thenReturn(mockIds)
      when(mockRedis.hget(anyString(), anyString())).thenReturn(mockLogsDataStr)

      val logsActor = system.actorOf(Props(new LogsActor {
        override protected def redisCommands: RedisCommands[String, String] = mockRedis
      }))

      // Act
      logsActor ! request

      // Assert
      expectMsgPF() {
        case GetWebLogsResponse(resultData, totalSize) =>
          totalSize shouldBe 2L
          resultData.size shouldBe 2
          resultData.keys should contain allOf("id1", "id2")
      }
    }

    "handle GetWebLogsRequest with invalid auth" in {
      // Arrange
      val request = GetWebLogsRequest(
        reqId = "test-req-id",
        auth = "invalid_auth",
        noOfPage = 1,
        pageLimit = 10,
        totalResult = 0L
      )

      val logsActor = system.actorOf(Props(new LogsActor {
        override protected def redisCommands: RedisCommands[String, String] = mockRedis
      }))

      // Act
      logsActor ! request

      // Assert
      expectMsg(GetWebLogsResponse(ListMap.empty, 0L))
    }
  }
}
