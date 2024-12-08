package com.teqbahn.actors.analytics.accumulator

import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.teqbahn.caseclasses.{AddToAccumulationRequest, AddToAccumulationWrapper, UserAccumulation}
import com.teqbahn.global.ZiRedisCons
import com.teqbahn.bootstrap.StarterMain
import org.mockito.MockitoSugar
import io.lettuce.core.api.sync.RedisCommands
import org.mockito.Mockito.{when, verify, times}

class GenderAccumulatorsSpec 
    extends TestKit(ActorSystem("GenderAccumulatorsSpec"))
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

  "GenderAccumulators actor" should {
    "initialize counter and increment for new gender accumulation" in {
      // Arrange
      val createdAt = System.currentTimeMillis()
      val gender = "male"
      val userId = "user123"
      val index = ZiRedisCons.ACCUMULATOR_GenderUserCounter + gender
      
      // Mock Redis behavior
      when(mockRedisCommands.get(index)).thenReturn(null)
      when(mockRedisCommands.set(index, "0")).thenReturn("OK")
      when(mockRedisCommands.incr(index)).thenReturn(1L)
      
      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = "5-7",
        genderOfChild = gender,
        language = "en",
        ip = "",
        deviceInfo = ""
      )
      val request = AddToAccumulationRequest(
        "User", 
        Some(userAccumulation), 
        createdAt
      )
      
      val genderAccumulator = system.actorOf(Props[GenderAccumulators])
      
      // Act
      genderAccumulator ! AddToAccumulationWrapper(request, gender)
      
      // Assert
      Thread.sleep(100) // Give some time for actor processing
      verify(mockRedisCommands).incr(index)
    }

    "increment existing gender counter" in {
      // Arrange
      val createdAt = System.currentTimeMillis()
      val gender = "female"
      val userId = "user456"
      val index = ZiRedisCons.ACCUMULATOR_GenderUserCounter + gender
      
      // Mock Redis behavior
      when(mockRedisCommands.get(index)).thenReturn("5")
      when(mockRedisCommands.incr(index)).thenReturn(6L)
      
      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = "8-10",
        genderOfChild = gender,
        language = "es",
        ip = "",
        deviceInfo = ""
      )
      val request = AddToAccumulationRequest(
        "User", 
        Some(userAccumulation), 
        createdAt
      )
      
      val genderAccumulator = system.actorOf(Props[GenderAccumulators])
      
      // Act
      genderAccumulator ! AddToAccumulationWrapper(request, gender)
      
      // Assert
      Thread.sleep(100) // Give some time for actor processing
      verify(mockRedisCommands).incr(index)
    }

    "handle multiple increments for same gender" in {
      // Arrange
      val createdAt = System.currentTimeMillis()
      val gender = "other"
      val userId = "user789"
      val index = ZiRedisCons.ACCUMULATOR_GenderUserCounter + gender
      
      // Mock Redis behavior
      when(mockRedisCommands.get(index)).thenReturn("0")
      when(mockRedisCommands.incr(index))
        .thenReturn(1L)
      when(mockRedisCommands.incr(index))
        .thenReturn(2L)
      when(mockRedisCommands.incr(index))
        .thenReturn(3L)
      
      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = "11-13",
        genderOfChild = gender,
        language = "fr",
        ip = "",
        deviceInfo = ""
      )
      val request = AddToAccumulationRequest(
        "User", 
        Some(userAccumulation), 
        createdAt
      )
      
      val genderAccumulator = system.actorOf(Props[GenderAccumulators])
      
      // Act
      (1 to 3).foreach(_ => genderAccumulator ! AddToAccumulationWrapper(request, gender))
      
      // Assert
      Thread.sleep(100) // Give some time for actor processing
      verify(mockRedisCommands, times(3)).incr(index)
    }

    "handle empty gender value" in {
      // Arrange
      val createdAt = System.currentTimeMillis()
      val gender = ""
      val userId = "user101"
      val index = ZiRedisCons.ACCUMULATOR_GenderUserCounter + gender
      
      // Mock Redis behavior
      when(mockRedisCommands.get(index)).thenReturn(null)
      when(mockRedisCommands.set(index, "0")).thenReturn("OK")
      when(mockRedisCommands.incr(index)).thenReturn(1L)
      
      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = "5-7",
        genderOfChild = gender,
        language = "en",
        ip = "",
        deviceInfo = ""
      )
      val request = AddToAccumulationRequest(
        "User", 
        Some(userAccumulation), 
        createdAt
      )
      
      val genderAccumulator = system.actorOf(Props[GenderAccumulators])
      
      // Act
      genderAccumulator ! AddToAccumulationWrapper(request, gender)
      
      // Assert
      Thread.sleep(100) // Give some time for actor processing
      verify(mockRedisCommands).incr(index)
    }
  }
} 