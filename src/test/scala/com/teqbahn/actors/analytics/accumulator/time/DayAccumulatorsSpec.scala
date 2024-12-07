package com.teqbahn.actors.analytics.accumulator.time

import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.teqbahn.caseclasses.{
  AddToAccumulationWrapper, 
  AddToAccumulationRequest,
  AddUserAttemptAccumulationWrapper,
  AddUserAttemptAccumulationRequest,
  UserAccumulation
}
import com.teqbahn.global.ZiRedisCons
import io.lettuce.core.api.sync.RedisCommands
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.{when, verify, never, timeout}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import scala.concurrent.duration._

class DayAccumulatorsSpec 
    extends TestKit(ActorSystem("DayAccumulatorsSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with MockitoSugar {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "DayAccumulators" should {
    "increment user counter for AddToAccumulationWrapper" in {
      // Mock Redis commands
      val mockRedis = mock[RedisCommands[String, String]]
      when(mockRedis.get(any[String])).thenReturn(null)
      when(mockRedis.set(any[String], any[String])).thenReturn("OK")
      when(mockRedis.incr(any[String])).thenReturn(1L)

      // Replace real Redis with mock
      com.teqbahn.bootstrap.StarterMain.redisCommands = mockRedis

      val actor = system.actorOf(Props[DayAccumulators])
      val testId = "test123"
      val request = AddToAccumulationRequest(
        dataType = "someData",
        accumulation = None,
        createdAt = System.currentTimeMillis()
      )
      val msg = AddToAccumulationWrapper(request, testId)

      // Send message and wait for processing
      actor ! msg
      Thread.sleep(100) // Give actor time to process

      // Verify Redis interactions with timeout
      verify(mockRedis, timeout(1000)).get(ZiRedisCons.ACCUMULATOR_DayUserCounter + testId)
      verify(mockRedis, timeout(1000)).set(ZiRedisCons.ACCUMULATOR_DayUserCounter + testId, "0")
      verify(mockRedis, timeout(1000)).incr(ZiRedisCons.ACCUMULATOR_DayUserCounter + testId)
    }

    "handle AddUserAttemptAccumulationWrapper correctly" in {
      val mockRedis = mock[RedisCommands[String, String]]
      when(mockRedis.get(any[String])).thenReturn(null)
      when(mockRedis.set(any[String], any[String])).thenReturn("OK")
      when(mockRedis.incr(any[String])).thenReturn(1L)
      when(mockRedis.sismember(any[String], any[String])).thenReturn(false)
      when(mockRedis.sadd(any[String], any[String])).thenReturn(1L)

      com.teqbahn.bootstrap.StarterMain.redisCommands = mockRedis

      val actor = system.actorOf(Props[DayAccumulators])
      val testId = "test456"
      val userId = "user789"
      val request = AddUserAttemptAccumulationRequest(
        dataType = "someData",
        userid = userId,
        createdAt = System.currentTimeMillis()
      )
      val msg = AddUserAttemptAccumulationWrapper(request, testId)

      // Send message and wait for processing
      actor ! msg
      Thread.sleep(100) // Give actor time to process

      // Verify Redis interactions with timeout
      verify(mockRedis, timeout(1000)).get(ZiRedisCons.ACCUMULATOR_DayUserAttemptCounter + testId)
      verify(mockRedis, timeout(1000)).set(ZiRedisCons.ACCUMULATOR_DayUserAttemptCounter + testId, "0")
      verify(mockRedis, timeout(1000)).incr(ZiRedisCons.ACCUMULATOR_DayUserAttemptCounter + testId)

      verify(mockRedis, timeout(1000)).sismember(ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptSet + testId, userId)
      verify(mockRedis, timeout(1000)).sadd(ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptSet + testId, userId)

      verify(mockRedis, timeout(1000)).get(ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptCounter + testId)
      verify(mockRedis, timeout(1000)).set(ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptCounter + testId, "0")
      verify(mockRedis, timeout(1000)).incr(ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptCounter + testId)
    }
  }
} 