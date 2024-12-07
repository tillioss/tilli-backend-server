package com.teqbahn.actors.analytics.accumulator

import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.teqbahn.caseclasses._
import com.teqbahn.global.ZiRedisCons
import com.teqbahn.bootstrap.StarterMain
import org.mockito.MockitoSugar
import io.lettuce.core.api.sync.RedisCommands
import org.mockito.Mockito.{when, verify, times}

class FilterAccumulatorsSpec
    extends TestKit(ActorSystem("FilterAccumulatorsSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with MockitoSugar {

  // Mock Redis commands
  val mockRedisCommands = mock[RedisCommands[String, String]]
  
  override def beforeAll(): Unit = {
    StarterMain.redisCommands = mockRedisCommands
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "FilterAccumulators actor" should {
    "accumulate age filter data" in {
      // Arrange
      val createdAt = System.currentTimeMillis()
      val userId = "user123"
      val age = "5-7"
      val accumulationDate = AccumulationDate("2024", "2024-12", "2024-12-07")
      
      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = age,
        genderOfChild = "",
        language = "",
        ip = "",
        deviceInfo = ""
      )

      val request = AddToAccumulationRequest(
        "User",
        Some(userAccumulation),
        createdAt
      )

      val wrapper = AddToFilterAccumulationWrapper(request, userId, accumulationDate)
      
      // Mock Redis behavior
      val ageMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_map"
      when(mockRedisCommands.hexists(ageMapKey, userId)).thenReturn(false)
      
      val yearKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "age_" + age
      val monthKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "age_" + age
      val dateKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "age_" + age
      
      when(mockRedisCommands.incr(yearKey)).thenReturn(1L)
      when(mockRedisCommands.incr(monthKey)).thenReturn(1L)
      when(mockRedisCommands.incr(dateKey)).thenReturn(1L)
      when(mockRedisCommands.hset(ageMapKey, userId, "1")).thenReturn(true)
      
      val filterAccumulator = system.actorOf(Props[FilterAccumulators])
      
      // Act
      filterAccumulator ! wrapper
      
      // Assert
      Thread.sleep(100)
      verify(mockRedisCommands).incr(yearKey)
      verify(mockRedisCommands).incr(monthKey)
      verify(mockRedisCommands).incr(dateKey)
      verify(mockRedisCommands).hset(ageMapKey, userId, "1")
    }

    "accumulate language filter data" in {
      // Arrange
      val createdAt = System.currentTimeMillis()
      val userId = "user456"
      val language = "en"
      val accumulationDate = AccumulationDate("2024", "2024-12", "2024-12-07")
      
      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = "",
        genderOfChild = "",
        language = language,
        ip = "",
        deviceInfo = ""
      )

      val request = AddToAccumulationRequest(
        "User",
        Some(userAccumulation),
        createdAt
      )

      val wrapper = AddToFilterAccumulationWrapper(request, userId, accumulationDate)
      
      // Mock Redis behavior
      val langMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "lang_map"
      when(mockRedisCommands.hexists(langMapKey, userId)).thenReturn(false)
      
      val yearKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "lang_" + language
      val monthKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "lang_" + language
      val dateKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "lang_" + language
      
      when(mockRedisCommands.incr(yearKey)).thenReturn(1L)
      when(mockRedisCommands.incr(monthKey)).thenReturn(1L)
      when(mockRedisCommands.incr(dateKey)).thenReturn(1L)
      when(mockRedisCommands.hset(langMapKey, userId, "1")).thenReturn(true)
      
      val filterAccumulator = system.actorOf(Props[FilterAccumulators])
      
      // Act
      filterAccumulator ! wrapper
      
      // Assert
      Thread.sleep(100)
      verify(mockRedisCommands).incr(yearKey)
      verify(mockRedisCommands).incr(monthKey)
      verify(mockRedisCommands).incr(dateKey)
      verify(mockRedisCommands).hset(langMapKey, userId, "1")
    }

    "accumulate combined age and gender filter data" in {
      // Arrange
      val createdAt = System.currentTimeMillis()
      val userId = "user789"
      val age = "5-7"
      val gender = "male"
      val accumulationDate = AccumulationDate("2024", "2024-12", "2024-12-07")
      
      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = age,
        genderOfChild = gender,
        language = "",
        ip = "",
        deviceInfo = ""
      )

      val request = AddToAccumulationRequest(
        "User",
        Some(userAccumulation),
        createdAt
      )

      val wrapper = AddToFilterAccumulationWrapper(request, userId, accumulationDate)
      
      // Mock Redis behavior
      val ageGenderMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_gender_map"
      when(mockRedisCommands.hexists(ageGenderMapKey, userId)).thenReturn(false)
      
      val yearKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + gender
      val monthKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + gender
      val dateKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + gender
      
      when(mockRedisCommands.incr(yearKey)).thenReturn(1L)
      when(mockRedisCommands.incr(monthKey)).thenReturn(1L)
      when(mockRedisCommands.incr(dateKey)).thenReturn(1L)
      when(mockRedisCommands.hset(ageGenderMapKey, userId, "1")).thenReturn(true)
      
      val filterAccumulator = system.actorOf(Props[FilterAccumulators])
      
      // Act
      filterAccumulator ! wrapper
      
      // Assert
      Thread.sleep(100)
      verify(mockRedisCommands).incr(yearKey)
      verify(mockRedisCommands).incr(monthKey)
      verify(mockRedisCommands).incr(dateKey)
      verify(mockRedisCommands).hset(ageGenderMapKey, userId, "1")
    }
  }
} 