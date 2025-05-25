package com.teqbahn.actors.analytics.accumulator

import com.teqbahn.bootstrap.StarterMain
import com.teqbahn.caseclasses._
import com.teqbahn.global.ZiRedisCons
import io.lettuce.core.api.sync.RedisCommands
import org.apache.pekko.actor.{ActorSystem, Props, ReceiveTimeout}
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class FilterAccumulatorsSpec
    extends TestKit(ActorSystem("FilterAccumulatorsSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with MockitoSugar {

  // Mock Redis commands
  val mockRedisCommands: RedisCommands[String, String] = mock[RedisCommands[String, String]]
  
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

    "accumulate gender filter data" in {
      val createdAt = System.currentTimeMillis()
      val userId = "user234"
      val gender = "female"
      val accumulationDate = AccumulationDate("2024", "2024-12", "2024-12-07")

      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = "",
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

      val genderMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "gender_map"
      when(mockRedisCommands.hexists(genderMapKey, userId)).thenReturn(false)

      val yearKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "gender_" + gender
      val monthKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "gender_" + gender
      val dateKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "gender_" + gender

      when(mockRedisCommands.incr(yearKey)).thenReturn(1L)
      when(mockRedisCommands.incr(monthKey)).thenReturn(1L)
      when(mockRedisCommands.incr(dateKey)).thenReturn(1L)
      when(mockRedisCommands.hset(genderMapKey, userId, "1")).thenReturn(true)

      val filterAccumulator = system.actorOf(Props[FilterAccumulators])

      filterAccumulator ! wrapper

      Thread.sleep(100)
      verify(mockRedisCommands).incr(yearKey)
      verify(mockRedisCommands).incr(monthKey)
      verify(mockRedisCommands).incr(dateKey)
      verify(mockRedisCommands).hset(genderMapKey, userId, "1")
    }

    "accumulate combined age and language filter data" in {
      val createdAt = System.currentTimeMillis()
      val userId = "user345"
      val age = "8-10"
      val language = "es"
      val accumulationDate = AccumulationDate("2024", "2024-12", "2024-12-07")

      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = age,
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

      val ageLangMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_lang_map"
      when(mockRedisCommands.hexists(ageLangMapKey, userId)).thenReturn(false)

      val yearKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "lang_" + language
      val monthKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "lang_" + language
      val dateKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "lang_" + language

      when(mockRedisCommands.incr(yearKey)).thenReturn(1L)
      when(mockRedisCommands.incr(monthKey)).thenReturn(1L)
      when(mockRedisCommands.incr(dateKey)).thenReturn(1L)
      when(mockRedisCommands.hset(ageLangMapKey, userId, "1")).thenReturn(true)

      val filterAccumulator = system.actorOf(Props[FilterAccumulators])

      filterAccumulator ! wrapper

      Thread.sleep(100)
      verify(mockRedisCommands).incr(yearKey)
      verify(mockRedisCommands).incr(monthKey)
      verify(mockRedisCommands).incr(dateKey)
      verify(mockRedisCommands).hset(ageLangMapKey, userId, "1")
    }

    "accumulate combined gender and language filter data" in {
      val createdAt = System.currentTimeMillis()
      val userId = "user456"
      val gender = "male"
      val language = "fr"
      val accumulationDate = AccumulationDate("2024", "2024-12", "2024-12-07")

      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = "",
        genderOfChild = gender,
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

      val genderLangMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "gender_lang_map"
      when(mockRedisCommands.hexists(genderLangMapKey, userId)).thenReturn(false)

      val yearKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "gender_" + gender + ZiRedisCons.SEPARATOR + "lang_" + language
      val monthKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "gender_" + gender + ZiRedisCons.SEPARATOR + "lang_" + language
      val dateKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "gender_" + gender + ZiRedisCons.SEPARATOR + "lang_" + language

      when(mockRedisCommands.incr(yearKey)).thenReturn(1L)
      when(mockRedisCommands.incr(monthKey)).thenReturn(1L)
      when(mockRedisCommands.incr(dateKey)).thenReturn(1L)
      when(mockRedisCommands.hset(genderLangMapKey, userId, "1")).thenReturn(true)

      val filterAccumulator = system.actorOf(Props[FilterAccumulators])

      filterAccumulator ! wrapper

      Thread.sleep(100)
      verify(mockRedisCommands).incr(yearKey)
      verify(mockRedisCommands).incr(monthKey)
      verify(mockRedisCommands).incr(dateKey)
      verify(mockRedisCommands).hset(genderLangMapKey, userId, "1")
    }

    "accumulate combined age, gender, and language filter data" in {
      val createdAt = System.currentTimeMillis()
      val userId = "user567"
      val age = "11-13"
      val gender = "female"
      val language = "de"
      val accumulationDate = AccumulationDate("2024", "2024-12", "2024-12-07")

      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = age,
        genderOfChild = gender,
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

      val ageGenderLangMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_gender_lang_map"
      when(mockRedisCommands.hexists(ageGenderLangMapKey, userId)).thenReturn(false)

      val yearKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + gender + ZiRedisCons.SEPARATOR + "lang_" + language
      val monthKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + gender + ZiRedisCons.SEPARATOR + "lang_" + language
      val dateKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + gender + ZiRedisCons.SEPARATOR + "lang_" + language

      when(mockRedisCommands.incr(yearKey)).thenReturn(1L)
      when(mockRedisCommands.incr(monthKey)).thenReturn(1L)
      when(mockRedisCommands.incr(dateKey)).thenReturn(1L)
      when(mockRedisCommands.hset(ageGenderLangMapKey, userId, "1")).thenReturn(true)

      val filterAccumulator = system.actorOf(Props[FilterAccumulators])

      filterAccumulator ! wrapper

      Thread.sleep(100)
      verify(mockRedisCommands).incr(yearKey)
      verify(mockRedisCommands).incr(monthKey)
      verify(mockRedisCommands).incr(dateKey)
      verify(mockRedisCommands).hset(ageGenderLangMapKey, userId, "1")
    }

    "not accumulate data when userId already exists in map" in {
      val createdAt = System.currentTimeMillis()
      val userId = "user678"
      val age = "14-16"
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

      val ageMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_map"
      when(mockRedisCommands.hexists(ageMapKey, userId)).thenReturn(true)

      val yearKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "age_" + age
      val monthKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "age_" + age
      val dateKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "age_" + age

      val filterAccumulator = system.actorOf(Props[FilterAccumulators])

      filterAccumulator ! wrapper

      Thread.sleep(100)
      verify(mockRedisCommands, never).incr(yearKey)
      verify(mockRedisCommands, never).incr(monthKey)
      verify(mockRedisCommands, never).incr(dateKey)
      verify(mockRedisCommands, never).hset(ageMapKey, userId, "1")
    }

    "handle non-User data type" in {
      val createdAt = System.currentTimeMillis()
      val userId = "user789"
      val accumulationDate = AccumulationDate("2024", "2024-12", "2024-12-07")

      val mockRedisCommandsForTest = mock[RedisCommands[String, String]]
      StarterMain.redisCommands = mockRedisCommandsForTest

      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = "3-5",
        genderOfChild = "",
        language = "",
        ip = "",
        deviceInfo = ""
      )

      val request = AddToAccumulationRequest(
        "Content", // Different data type than "User"
        Some(userAccumulation),
        createdAt
      )

      val wrapper = AddToFilterAccumulationWrapper(request, userId, accumulationDate)

      val filterAccumulator = system.actorOf(Props[FilterAccumulators])

      filterAccumulator ! wrapper

      Thread.sleep(100)

      verifyZeroInteractions(mockRedisCommandsForTest)

      StarterMain.redisCommands = mockRedisCommands
    }

    "handle null accumulation" in {
      val createdAt = System.currentTimeMillis()
      val userId = "user890"
      val accumulationDate = AccumulationDate("2024", "2024-12", "2024-12-07")

      val mockRedisCommandsForTest = mock[RedisCommands[String, String]]
      StarterMain.redisCommands = mockRedisCommandsForTest

      val request = AddToAccumulationRequest(
        "User",
        None, // No accumulation
        createdAt
      )

      val wrapper = AddToFilterAccumulationWrapper(request, userId, accumulationDate)

      val filterAccumulator = system.actorOf(Props[FilterAccumulators])

      filterAccumulator ! wrapper

      Thread.sleep(100)
      verifyZeroInteractions(mockRedisCommandsForTest)

      StarterMain.redisCommands = mockRedisCommands
    }

    "handle ReceiveTimeout message" in {
      val filterAccumulator = system.actorOf(Props[FilterAccumulators])

      watch(filterAccumulator)
      filterAccumulator ! ReceiveTimeout

      expectTerminated(filterAccumulator)
    }
  }
} 