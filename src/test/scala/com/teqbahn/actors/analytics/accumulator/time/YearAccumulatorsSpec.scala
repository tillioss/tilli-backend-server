package com.teqbahn.actors.analytics.accumulator.time

import org.apache.pekko.actor.{ActorSystem, Props, ReceiveTimeout}
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

class YearAccumulatorsSpec 
    extends TestKit(ActorSystem("YearAccumulatorsSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with MockitoSugar {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "YearAccumulators" should {
    "increment user counter for AddToAccumulationWrapper" in {
      // Mock Redis commands
      val mockRedis = mock[RedisCommands[String, String]]
      when(mockRedis.get(any[String])).thenReturn(null)
      when(mockRedis.set(any[String], any[String])).thenReturn("OK")
      when(mockRedis.incr(any[String])).thenReturn(1L)

      // Replace real Redis with mock
      com.teqbahn.bootstrap.StarterMain.redisCommands = mockRedis

      val actor = system.actorOf(Props[YearAccumulators])
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
      verify(mockRedis, timeout(1000)).get(ZiRedisCons.ACCUMULATOR_YearUserCounter + testId)
      verify(mockRedis, timeout(1000)).set(ZiRedisCons.ACCUMULATOR_YearUserCounter + testId, "0")
      verify(mockRedis, timeout(1000)).incr(ZiRedisCons.ACCUMULATOR_YearUserCounter + testId)
    }

    "handle AddUserAttemptAccumulationWrapper correctly for new user" in {
      val mockRedis = mock[RedisCommands[String, String]]
      when(mockRedis.get(any[String])).thenReturn(null)
      when(mockRedis.set(any[String], any[String])).thenReturn("OK")
      when(mockRedis.incr(any[String])).thenReturn(1L)
      when(mockRedis.sismember(any[String], any[String])).thenReturn(false)
      when(mockRedis.sadd(any[String], any[String])).thenReturn(1L)

      com.teqbahn.bootstrap.StarterMain.redisCommands = mockRedis

      val actor = system.actorOf(Props[YearAccumulators])
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
      verify(mockRedis, timeout(1000)).get(ZiRedisCons.ACCUMULATOR_YearUserAttemptCounter + testId)
      verify(mockRedis, timeout(1000)).set(ZiRedisCons.ACCUMULATOR_YearUserAttemptCounter + testId, "0")
      verify(mockRedis, timeout(1000)).incr(ZiRedisCons.ACCUMULATOR_YearUserAttemptCounter + testId)

      verify(mockRedis, timeout(1000)).sismember(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptSet + testId, userId)
      verify(mockRedis, timeout(1000)).sadd(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptSet + testId, userId)

      verify(mockRedis, timeout(1000)).get(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptCounter + testId)
      verify(mockRedis, timeout(1000)).set(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptCounter + testId, "0")
      verify(mockRedis, timeout(1000)).incr(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptCounter + testId)
    }

    "handle AddUserAttemptAccumulationWrapper correctly for existing user" in {
      val mockRedis = mock[RedisCommands[String, String]]
      when(mockRedis.get(any[String])).thenReturn(null)
      when(mockRedis.set(any[String], any[String])).thenReturn("OK")
      when(mockRedis.incr(any[String])).thenReturn(1L)
      when(mockRedis.sismember(any[String], any[String])).thenReturn(true) // User exists
      when(mockRedis.sadd(any[String], any[String])).thenReturn(0L)

      com.teqbahn.bootstrap.StarterMain.redisCommands = mockRedis

      val actor = system.actorOf(Props[YearAccumulators])
      val testId = "test456"
      val userId = "existingUser123"
      val request = AddUserAttemptAccumulationRequest(
        dataType = "someData",
        userid = userId,
        createdAt = System.currentTimeMillis()
      )
      val msg = AddUserAttemptAccumulationWrapper(request, testId)

      // Send message and wait for processing
      actor ! msg
      Thread.sleep(100)

      // Verify attempt counter is still incremented
      verify(mockRedis, timeout(1000)).get(ZiRedisCons.ACCUMULATOR_YearUserAttemptCounter + testId)
      verify(mockRedis, timeout(1000)).set(ZiRedisCons.ACCUMULATOR_YearUserAttemptCounter + testId, "0")
      verify(mockRedis, timeout(1000)).incr(ZiRedisCons.ACCUMULATOR_YearUserAttemptCounter + testId)

      // Verify user existence check
      verify(mockRedis, timeout(1000)).sismember(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptSet + testId, userId)

      // Verify that unique user counter is NOT incremented
      verify(mockRedis, never).get(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptCounter + testId)
      verify(mockRedis, never).set(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptCounter + testId, "0")
      verify(mockRedis, never).incr(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptCounter + testId)
    }

    "handle counter initialization when Redis returns non-null value" in {
      val mockRedis = mock[RedisCommands[String, String]]
      when(mockRedis.get(any[String])).thenReturn("10") // Return existing counter value
      when(mockRedis.incr(any[String])).thenReturn(11L)

      com.teqbahn.bootstrap.StarterMain.redisCommands = mockRedis

      val actor = system.actorOf(Props[YearAccumulators])
      val testId = "test789"
      val request = AddToAccumulationRequest(
        dataType = "someData",
        accumulation = None,
        createdAt = System.currentTimeMillis()
      )
      val msg = AddToAccumulationWrapper(request, testId)

      actor ! msg
      Thread.sleep(100)

      // Verify Redis interactions
      verify(mockRedis, timeout(1000)).get(ZiRedisCons.ACCUMULATOR_YearUserCounter + testId)
      verify(mockRedis, never).set(any[String], any[String]) // Should not set initial value
      verify(mockRedis, timeout(1000)).incr(ZiRedisCons.ACCUMULATOR_YearUserCounter + testId)
    }

    "handle ReceiveTimeout message" in {
      val actor = system.actorOf(Props[YearAccumulators])
      
      watch(actor) // Watch the actor for termination
      actor ! ReceiveTimeout
      
      expectTerminated(actor) // Verify the actor is terminated
    }
  }
}
