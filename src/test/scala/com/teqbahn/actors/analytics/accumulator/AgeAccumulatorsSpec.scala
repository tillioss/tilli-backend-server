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

class AgeAccumulatorsSpec 
    extends TestKit(ActorSystem("AgeAccumulatorsSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with MockitoSugar {

  // Mock Redis commands
  val mockRedisCommands = mock[RedisCommands[String, String]]
  
  override def beforeAll(): Unit = {
    // Replace the real Redis commands with our mock
    StarterMain.redisCommands = mockRedisCommands
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "AgeAccumulators actor" should {
    "initialize counter and increment for new accumulation" in {
      // Arrange
      val createdAt = System.currentTimeMillis()
      val age = "5-7"
      val userId = "user123"
      val index = ZiRedisCons.ACCUMULATOR_AgeUserCounter + age
      
      // Mock Redis behavior
      when(mockRedisCommands.get(index)).thenReturn(null)
      when(mockRedisCommands.set(index, "0")).thenReturn("OK")
      when(mockRedisCommands.incr(index)).thenReturn(1L)
      
      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = age,
        genderOfChild = "male",
        language = "en",
        ip = "",
        deviceInfo = ""
      )
      val request = AddToAccumulationRequest(
        "User", 
        Some(userAccumulation), 
        createdAt
      )
      
      val ageAccumulator = system.actorOf(Props[AgeAccumulators])
      
      // Act
      ageAccumulator ! AddToAccumulationWrapper(request, age)
      
      // Assert
      Thread.sleep(100) // Give some time for actor processing
      verify(mockRedisCommands).incr(index)
    }

    "increment existing counter" in {
      // Arrange
      val createdAt = System.currentTimeMillis()
      val age = "8-10"
      val userId = "user456"
      val index = ZiRedisCons.ACCUMULATOR_AgeUserCounter + age
      
      // Mock Redis behavior
      when(mockRedisCommands.get(index)).thenReturn("5")
      when(mockRedisCommands.incr(index)).thenReturn(6L)
      
      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = age,
        genderOfChild = "female",
        language = "es",
        ip = "",
        deviceInfo = ""
      )
      val request = AddToAccumulationRequest(
        "User", 
        Some(userAccumulation), 
        createdAt
      )
      
      val ageAccumulator = system.actorOf(Props[AgeAccumulators])
      
      // Act
      ageAccumulator ! AddToAccumulationWrapper(request, age)
      
      // Assert
      Thread.sleep(100) // Give some time for actor processing
      verify(mockRedisCommands).incr(index)
    }

    "handle multiple increments" in {
      // Arrange
      val createdAt = System.currentTimeMillis()
      val age = "11-13"
      val userId = "user789"
      val index = ZiRedisCons.ACCUMULATOR_AgeUserCounter + age
      
      // Mock Redis behavior
      when(mockRedisCommands.get(index)).thenReturn("0")
      // Setup multiple return values for consecutive calls
      when(mockRedisCommands.incr(index))
        .thenReturn(1L) // first call
      when(mockRedisCommands.incr(index))
        .thenReturn(2L) // second call
      when(mockRedisCommands.incr(index))
        .thenReturn(3L) // third call
      
      val userAccumulation = UserAccumulation(
        createdAt = createdAt,
        userId = userId,
        ageOfChild = age,
        genderOfChild = "male",
        language = "fr",
        ip = "",
        deviceInfo = ""
      )
      val request = AddToAccumulationRequest(
        "User", 
        Some(userAccumulation), 
        createdAt
      )
      
      val ageAccumulator = system.actorOf(Props[AgeAccumulators])
      
      // Act
      (1 to 3).foreach(_ => ageAccumulator ! AddToAccumulationWrapper(request, age))
      
      // Assert
      Thread.sleep(100) // Give some time for actor processing
      verify(mockRedisCommands, times(3)).incr(index)
    }
  }
} 