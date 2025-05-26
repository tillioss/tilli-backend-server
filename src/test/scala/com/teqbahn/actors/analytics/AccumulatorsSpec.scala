package com.teqbahn.actors.analytics

import com.teqbahn.bootstrap.StarterMain
import com.teqbahn.caseclasses._
import com.teqbahn.global.ZiRedisCons
import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit, TestProbe}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.text.SimpleDateFormat
import java.time.Instant

class AccumulatorsSpec extends TestKit(ActorSystem("AccumulatorsSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with MockitoSugar {

  private class DateFormatter {
    def getAccumulationDate(time: Long): AccumulationDate = {
      val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
      val format = simpleDateFormat.format(time)
      val formatArray = format.split("-")
      AccumulationDate(formatArray(0), formatArray(0) + "-" + formatArray(1), format)
    }
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Accumulators actor" should {
    "handle AddToAccumulationRequest with empty accumulation" in {
      val dayProbe = TestProbe()
      val monthProbe = TestProbe()
      val yearProbe = TestProbe()

      StarterMain.accumulatorDayActorRef = dayProbe.ref
      StarterMain.accumulatorMonthActorRef = monthProbe.ref
      StarterMain.accumulatorYearActorRef = yearProbe.ref

      val accumulator = system.actorOf(Props[Accumulators])

      val timestamp = Instant.parse("2024-03-20T10:15:30.00Z").toEpochMilli
      val request = AddToAccumulationRequest(
        dataType = "Test",
        accumulation = None,
        createdAt = timestamp
      )

      accumulator ! request

      dayProbe.expectMsg(AddToAccumulationWrapper(request, "2024-03-20"))
      monthProbe.expectMsg(AddToAccumulationWrapper(request, "2024-03"))
      yearProbe.expectMsg(AddToAccumulationWrapper(request, "2024"))
    }

    "handle AddToAccumulationRequest with partial demographic data" in {
      val dayProbe = TestProbe()
      val monthProbe = TestProbe()
      val yearProbe = TestProbe()
      val ageProbe = TestProbe()
      val genderProbe = TestProbe()
      val filterProbe = TestProbe()

      StarterMain.accumulatorDayActorRef = dayProbe.ref
      StarterMain.accumulatorMonthActorRef = monthProbe.ref
      StarterMain.accumulatorYearActorRef = yearProbe.ref
      StarterMain.accumulatorAgeActorRef = ageProbe.ref
      StarterMain.accumulatorGenderActorRef = genderProbe.ref
      StarterMain.accumulatorFilterActorRef = filterProbe.ref

      val accumulator = system.actorOf(Props[Accumulators])

      val timestamp = Instant.parse("2024-03-20T10:15:30.00Z").toEpochMilli
      val userAccumulation = UserAccumulation(
        createdAt = timestamp,
        userId = "test-user-2",
        ageOfChild = "8-10",
        genderOfChild = "",
        language = "",
        ip = "192.168.1.1",
        deviceInfo = "test-device-2"
      )

      val request = AddToAccumulationRequest(
        dataType = "Test",
        accumulation = Some(userAccumulation),
        createdAt = timestamp
      )

      accumulator ! request

      dayProbe.expectMsg(AddToAccumulationWrapper(request, "2024-03-20"))
      monthProbe.expectMsg(AddToAccumulationWrapper(request, "2024-03"))
      yearProbe.expectMsg(AddToAccumulationWrapper(request, "2024"))

      ageProbe.expectMsg(AddToAccumulationWrapper(request, "8-10"))

      filterProbe.expectMsgType[AddToFilterAccumulationWrapper]
    }

    "handle UpdateUserDetailsAccumulationRequest correctly with new user demographic data" in {
      val mockedRedisCommands = mock[io.lettuce.core.api.sync.RedisCommands[String, String]]
      StarterMain.redisCommands = mockedRedisCommands

      val ageProbe = TestProbe()
      val languageProbe = TestProbe()
      val genderProbe = TestProbe()
      val filterProbe = TestProbe()

      StarterMain.accumulatorAgeActorRef = ageProbe.ref
      StarterMain.accumulatorLanguageActorRef = languageProbe.ref
      StarterMain.accumulatorGenderActorRef = genderProbe.ref
      StarterMain.accumulatorFilterActorRef = filterProbe.ref

      when(mockedRedisCommands.hexists(s"${ZiRedisCons.ACCUMULATOR_AgeUserCounter}checkMap", "test-user-3")).thenReturn(false)
      when(mockedRedisCommands.hexists(s"${ZiRedisCons.ACCUMULATOR_LanguageUserCounter}checkMap", "test-user-3")).thenReturn(false)
      when(mockedRedisCommands.hexists(s"${ZiRedisCons.ACCUMULATOR_GenderUserCounter}checkMap", "test-user-3")).thenReturn(false)

      val accumulator = system.actorOf(Props[Accumulators])

      val timestamp = Instant.parse("2024-03-20T10:15:30.00Z").toEpochMilli
      val userAccumulation = UserAccumulation(
        createdAt = timestamp,
        userId = "test-user-3",
        ageOfChild = "13-15",
        genderOfChild = "female",
        language = "es",
        ip = "10.0.0.1",
        deviceInfo = "test-device-3"
      )

      val request = UpdateUserDetailsAccumulationRequest(
        dataType = "User",
        accumulation = Some(userAccumulation),
        createdAt = timestamp
      )

      accumulator ! request

      val expectedRequest = AddToAccumulationRequest("User", Some(userAccumulation), timestamp)

      ageProbe.expectMsg(AddToAccumulationWrapper(expectedRequest, "13-15"))
      languageProbe.expectMsg(AddToAccumulationWrapper(expectedRequest, "es"))
      genderProbe.expectMsg(AddToAccumulationWrapper(expectedRequest, "female"))

      filterProbe.expectMsgType[AddToFilterAccumulationWrapper]

      verify(mockedRedisCommands).hset(s"${ZiRedisCons.ACCUMULATOR_AgeUserCounter}checkMap", "test-user-3", "1")
      verify(mockedRedisCommands).hset(s"${ZiRedisCons.ACCUMULATOR_LanguageUserCounter}checkMap", "test-user-3", "1")
      verify(mockedRedisCommands).hset(s"${ZiRedisCons.ACCUMULATOR_GenderUserCounter}checkMap", "test-user-3", "1")
    }

    "not update demographics for existing users in UpdateUserDetailsAccumulationRequest" in {
      val mockedRedisCommands = mock[io.lettuce.core.api.sync.RedisCommands[String, String]]
      StarterMain.redisCommands = mockedRedisCommands

      val ageProbe = TestProbe()
      val languageProbe = TestProbe()
      val genderProbe = TestProbe()
      val filterProbe = TestProbe()

      StarterMain.accumulatorAgeActorRef = ageProbe.ref
      StarterMain.accumulatorLanguageActorRef = languageProbe.ref
      StarterMain.accumulatorGenderActorRef = genderProbe.ref
      StarterMain.accumulatorFilterActorRef = filterProbe.ref

      when(mockedRedisCommands.hexists(s"${ZiRedisCons.ACCUMULATOR_AgeUserCounter}checkMap", "test-user-4")).thenReturn(true)
      when(mockedRedisCommands.hexists(s"${ZiRedisCons.ACCUMULATOR_LanguageUserCounter}checkMap", "test-user-4")).thenReturn(true)
      when(mockedRedisCommands.hexists(s"${ZiRedisCons.ACCUMULATOR_GenderUserCounter}checkMap", "test-user-4")).thenReturn(true)

      val accumulator = system.actorOf(Props[Accumulators])

      val timestamp = Instant.parse("2024-03-20T10:15:30.00Z").toEpochMilli
      val userAccumulation = UserAccumulation(
        createdAt = timestamp,
        userId = "test-user-4",
        ageOfChild = "16-18",
        genderOfChild = "male",
        language = "fr",
        ip = "172.16.0.1",
        deviceInfo = "test-device-4"
      )

      val request = UpdateUserDetailsAccumulationRequest(
        dataType = "User",
        accumulation = Some(userAccumulation),
        createdAt = timestamp
      )

      accumulator ! request

      filterProbe.expectMsgType[AddToFilterAccumulationWrapper]

      ageProbe.expectNoMessage()
      languageProbe.expectNoMessage()
      genderProbe.expectNoMessage()

      verify(mockedRedisCommands, never).hset(s"${ZiRedisCons.ACCUMULATOR_AgeUserCounter}checkMap", "test-user-4", "1")
      verify(mockedRedisCommands, never).hset(s"${ZiRedisCons.ACCUMULATOR_LanguageUserCounter}checkMap", "test-user-4", "1")
      verify(mockedRedisCommands, never).hset(s"${ZiRedisCons.ACCUMULATOR_GenderUserCounter}checkMap", "test-user-4", "1")
    }

    "handle UpdateUserDetailsAccumulationRequest with empty accumulation" in {
      val filterProbe = TestProbe()
      StarterMain.accumulatorFilterActorRef = filterProbe.ref

      val accumulator = system.actorOf(Props[Accumulators])

      val timestamp = Instant.parse("2024-03-20T10:15:30.00Z").toEpochMilli
      val request = UpdateUserDetailsAccumulationRequest(
        dataType = "User",
        accumulation = None,
        createdAt = timestamp
      )

      accumulator ! request

      filterProbe.expectNoMessage()
    }

    "DateFormatter.getAccumulationDate" should {
      "format dates correctly" in {
        val formatter = new DateFormatter()

        val testCases = List(
          (Instant.parse("2024-01-01T00:00:00.00Z").toEpochMilli, AccumulationDate("2024", "2024-01", "2024-01-01")),
          (Instant.parse("2024-06-15T12:30:45.00Z").toEpochMilli, AccumulationDate("2024", "2024-06", "2024-06-15"))
        )

        testCases.foreach { case (timestamp, expected) =>
          val result = formatter.getAccumulationDate(timestamp)
          result.year shouldBe expected.year
          result.month shouldBe expected.month
          result.date shouldBe expected.date
        }
      }
    }
  }
}
