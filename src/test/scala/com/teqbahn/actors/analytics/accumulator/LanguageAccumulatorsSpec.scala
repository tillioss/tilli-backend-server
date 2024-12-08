package com.teqbahn.actors.analytics.accumulator

import org.apache.pekko.actor.{ActorSystem, Props, ReceiveTimeout}
import org.apache.pekko.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.teqbahn.caseclasses._
import com.teqbahn.bootstrap.StarterMain
import com.teqbahn.global.ZiRedisCons
import io.lettuce.core.api.sync.RedisCommands
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.anyString
import org.scalatest.BeforeAndAfterEach
import scala.concurrent.duration._

class LanguageAccumulatorsSpec
    extends TestKit(ActorSystem("LanguageAccumulatorsSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  var mockRedis: RedisCommands[String, String] = _

  override def beforeEach(): Unit = {
    mockRedis = mock(classOf[RedisCommands[String, String]])
    StarterMain.redisCommands = mockRedis
  }

  override def afterEach(): Unit = {
    reset(mockRedis)
  }

  val sampleAccumulator = AddToAccumulationRequest(
    dataType = "language",
    accumulation = None,
    createdAt = System.currentTimeMillis()
  )

  "LanguageAccumulators actor" should {
    "initialize counter when it doesn't exist" in {
      val languageActor = system.actorOf(Props[LanguageAccumulators])
      
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "english"))
        .thenReturn(null)
      when(mockRedis.set(anyString(), anyString()))
        .thenReturn("OK")
      when(mockRedis.incr(anyString()))
        .thenReturn(1L)

      languageActor ! AddToAccumulationWrapper(
        accumulator = sampleAccumulator,
        id = "english"
      )
      
      expectNoMessage(500.milliseconds)

      verify(mockRedis).get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "english")
      verify(mockRedis).set(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "english", "0")
      verify(mockRedis).incr(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "english")
    }

    "increment existing counter" in {
      val languageActor = system.actorOf(Props[LanguageAccumulators])
      
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "spanish"))
        .thenReturn("5")
      when(mockRedis.incr(anyString()))
        .thenReturn(6L)

      languageActor ! AddToAccumulationWrapper(
        accumulator = sampleAccumulator,
        id = "spanish"
      )
      
      expectNoMessage(500.milliseconds)

      verify(mockRedis).get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "spanish")
      verify(mockRedis).incr(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "spanish")
      verify(mockRedis, never()).set(anyString(), anyString())
    }

    "handle empty counter value" in {
      val languageActor = system.actorOf(Props[LanguageAccumulators])
      
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "french"))
        .thenReturn("")
      when(mockRedis.set(anyString(), anyString()))
        .thenReturn("OK")
      when(mockRedis.incr(anyString()))
        .thenReturn(1L)

      languageActor ! AddToAccumulationWrapper(
        accumulator = sampleAccumulator,
        id = "french"
      )
      
      expectNoMessage(500.milliseconds)

      verify(mockRedis).get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "french")
      verify(mockRedis).set(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "french", "0")
      verify(mockRedis).incr(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "french")
    }

    "handle 'null' string counter value" in {
      val languageActor = system.actorOf(Props[LanguageAccumulators])
      
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "german"))
        .thenReturn("null")
      when(mockRedis.set(anyString(), anyString()))
        .thenReturn("OK")
      when(mockRedis.incr(anyString()))
        .thenReturn(1L)

      languageActor ! AddToAccumulationWrapper(
        accumulator = sampleAccumulator,
        id = "german"
      )
      
      expectNoMessage(500.milliseconds)

      verify(mockRedis).get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "german")
      verify(mockRedis).set(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "german", "0")
      verify(mockRedis).incr(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "german")
    }

    "reuse existing check for multiple messages with same language" in {
      val languageActor = system.actorOf(Props[LanguageAccumulators])
      
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "hindi"))
        .thenReturn("10")
      when(mockRedis.incr(anyString()))
        .thenReturn(11L, 12L)

      languageActor ! AddToAccumulationWrapper(
        accumulator = sampleAccumulator,
        id = "hindi"
      )
      
      expectNoMessage(500.milliseconds)
      
      languageActor ! AddToAccumulationWrapper(
        accumulator = sampleAccumulator,
        id = "hindi"
      )
      
      expectNoMessage(500.milliseconds)

      verify(mockRedis, times(1)).get(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "hindi")
      verify(mockRedis, times(2)).incr(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "hindi")
    }

    "handle ReceiveTimeout message" in {
      val languageActor = system.actorOf(Props[LanguageAccumulators])
      
      watch(languageActor)
      languageActor ! ReceiveTimeout
      
      expectTerminated(languageActor)
    }
  }
} 