package com.teqbahn.actors.analytics

import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.teqbahn.caseclasses._
import com.teqbahn.bootstrap.StarterMain
import java.time.Instant

class AccumulatorsSpec 
    extends TestKit(ActorSystem("AccumulatorsSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Accumulators actor" should {
    "handle AddToAccumulationRequest correctly" in {
      // Create test probes for child actors
      val dayProbe = TestProbe()
      val monthProbe = TestProbe()
      val yearProbe = TestProbe()
      val ageProbe = TestProbe()
      val languageProbe = TestProbe()
      val genderProbe = TestProbe()
      val filterProbe = TestProbe()

      // Replace actual actor refs with test probes
      StarterMain.accumulatorDayActorRef = dayProbe.ref
      StarterMain.accumulatorMonthActorRef = monthProbe.ref
      StarterMain.accumulatorYearActorRef = yearProbe.ref
      StarterMain.accumulatorAgeActorRef = ageProbe.ref
      StarterMain.accumulatorLanguageActorRef = languageProbe.ref
      StarterMain.accumulatorGenderActorRef = genderProbe.ref
      StarterMain.accumulatorFilterActorRef = filterProbe.ref

      val accumulator = system.actorOf(Props[Accumulators])
      
      // Create test data with all required fields
      val timestamp = Instant.parse("2024-03-20T10:15:30.00Z").toEpochMilli
      val userAccumulation = UserAccumulation(
        createdAt = timestamp,
        userId = "test-user-1",
        ageOfChild = "5-7",
        genderOfChild = "male",
        language = "en",
        ip = "127.0.0.1",
        deviceInfo = "test-device"
      )
      
      val request = AddToAccumulationRequest(
        dataType = "Test",
        accumulation = Some(userAccumulation),
        createdAt = timestamp
      )

      // Send request to actor
      accumulator ! request

      // Verify messages sent to date-based accumulators
      dayProbe.expectMsg(AddToAccumulationWrapper(request, "2024-03-20"))
      monthProbe.expectMsg(AddToAccumulationWrapper(request, "2024-03"))
      yearProbe.expectMsg(AddToAccumulationWrapper(request, "2024"))

      // Verify messages sent to demographic accumulators
      ageProbe.expectMsg(AddToAccumulationWrapper(request, "5-7"))
      languageProbe.expectMsg(AddToAccumulationWrapper(request, "en"))
      genderProbe.expectMsg(AddToAccumulationWrapper(request, "male"))
      
      // Verify filter accumulator message
      filterProbe.expectMsgType[AddToFilterAccumulationWrapper]
    }

    "handle AddUserAttemptAccumulationRequest correctly" in {
      val dayProbe = TestProbe()
      val monthProbe = TestProbe()
      val yearProbe = TestProbe()

      StarterMain.accumulatorDayActorRef = dayProbe.ref
      StarterMain.accumulatorMonthActorRef = monthProbe.ref
      StarterMain.accumulatorYearActorRef = yearProbe.ref

      val accumulator = system.actorOf(Props[Accumulators])
      
      val timestamp = Instant.parse("2024-03-20T10:15:30.00Z").toEpochMilli
      val request = AddUserAttemptAccumulationRequest(
        dataType = "Test",
        userid = "test-user-1",
        createdAt = timestamp
      )

      accumulator ! request

      dayProbe.expectMsg(AddUserAttemptAccumulationWrapper(request, "2024-03-20"))
      monthProbe.expectMsg(AddUserAttemptAccumulationWrapper(request, "2024-03"))
      yearProbe.expectMsg(AddUserAttemptAccumulationWrapper(request, "2024"))
    }

  }
} 