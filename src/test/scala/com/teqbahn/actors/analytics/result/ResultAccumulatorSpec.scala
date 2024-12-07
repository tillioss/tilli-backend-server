package com.teqbahn.actors.analytics.result

import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.teqbahn.caseclasses._
import com.teqbahn.bootstrap.StarterMain
import com.teqbahn.global.ZiRedisCons
import java.time.LocalDateTime
import org.joda.time.DateTime
import io.lettuce.core.api.sync.RedisCommands
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.anyString

class ResultAccumulatorSpec
    extends TestKit(ActorSystem("ResultAccumulatorSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  // Mock Redis using Lettuce RedisCommands
  val mockRedis: RedisCommands[String, String] = mock(classOf[RedisCommands[String, String]])

  // Replace Redis commands with mock
  StarterMain.redisCommands = mockRedis

  "ResultAccumulator actor" should {
    "handle FetchAnalyticsRequest correctly" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])
      
      // Set up some test data in mock Redis
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_GenderUserCounter + "male")).thenReturn("10")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_GenderUserCounter + "female")).thenReturn("15")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "english")).thenReturn("20")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_AgeUserCounter + "7")).thenReturn("5")

      // Create and send request
      val request = FetchAnalyticsRequest(id = "test-request-1")
      resultAccumulator ! request

      // Verify response
      val response = expectMsgType[FetchAnalyticsResponse]
      val responseJson = response.response
      
      responseJson should include("genderBased")
      responseJson should include("languageBased")
      responseJson should include("ageBased")
      responseJson should include("\"x\":\"male\"")
      responseJson should include("\"y\":\"10\"")
    }

    "handle FetchFilterAnalyticsRequest correctly" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])
      
      val testDate = "2024-03-20"
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserCounter + testDate)).thenReturn("25")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::gender_male")).thenReturn("15")
      
      val request = FetchFilterAnalyticsRequest(
        sDate = Some(testDate),
        eDate = Some(testDate),
        filterGender = List("male"),
        filterLanguage = List("english"),
        filterAge = List("7"),
        requestType = "normal",
        id = "test-request-2"
      )
      
      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response
      
      responseJson should include("dateBased")
      responseJson should include("dateBasedGender")
      responseJson should include("dateBasedLanguage")
      responseJson should include(testDate)
    }

    "handle FetchFilterUserAttemptAnalyticsRequest correctly" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])
      
      val testDate = "2024-03-20"
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserAttemptCounter + testDate)).thenReturn("30")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptCounter + testDate)).thenReturn("20")
      
      val request = FetchFilterUserAttemptAnalyticsRequest(
        sDate = Some(testDate),
        eDate = Some(testDate),
        id = "test-request-3"
      )
      
      resultAccumulator ! request

      val response = expectMsgType[FetchFilterUserAttemptAnalyticsResponse]
      val responseJson = response.response
      
      responseJson should include("dateBasedAttempt")
      responseJson should include("dateBasedUniqueUserAttempt")
      responseJson should include(testDate)
      responseJson should include("\"y\":\"30\"")
      responseJson should include("\"y\":\"20\"")
    }

    "handle date range calculations correctly" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])
      
      val startDate = "2024-03-01"
      val endDate = "2024-03-03"
      
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserCounter + "2024-03-01")).thenReturn("10")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserCounter + "2024-03-02")).thenReturn("20")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserCounter + "2024-03-03")).thenReturn("30")
      
      val request = FetchFilterAnalyticsRequest(
        sDate = Some(startDate),
        eDate = Some(endDate),
        filterGender = List(),
        filterLanguage = List(),
        filterAge = List(),
        requestType = "normal",
        id = "test-request-4"
      )
      
      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response
      
      responseJson should include("2024-03-01")
      responseJson should include("2024-03-02")
      responseJson should include("2024-03-03")
      responseJson should include("\"y\":\"10\"")
      responseJson should include("\"y\":\"20\"")
      responseJson should include("\"y\":\"30\"")
    }

    "handle missing Redis data gracefully" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])
      
      // Don't set any Redis data - should default to "0"
      when(mockRedis.get(anyString())).thenReturn(null)
      
      val request = FetchAnalyticsRequest(id = "test-request-5")
      resultAccumulator ! request

      val response = expectMsgType[FetchAnalyticsResponse]
      val responseJson = response.response
      
      // Should contain default "0" values
      responseJson should include("\"y\":\"0\"")
    }
  }
} 