package com.teqbahn.actors.analytics.result

import com.teqbahn.bootstrap.StarterMain
import com.teqbahn.caseclasses._
import com.teqbahn.global.ZiRedisCons
import io.lettuce.core.api.sync.RedisCommands
import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

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

      // Create and send a request
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

    "handle FetchFilterAnalyticsRequest with filter requestType correctly" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val testDate = "2024-03-20"
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_7")).thenReturn("10")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::gender_male::lang_english")).thenReturn("15")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_7::gender_male")).thenReturn("20")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_7::lang_english")).thenReturn("25")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_7::gender_male::lang_english")).thenReturn("30")

      val request = FetchFilterAnalyticsRequest(
        sDate = Some(testDate),
        eDate = Some(testDate),
        filterGender = List("male"),
        filterLanguage = List("english"),
        filterAge = List("7"),
        requestType = "filter",
        id = "test-filter-request"
      )

      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response

      responseJson should include("dateBased")
      responseJson should include(testDate)
      responseJson should include("\"y\":\"30\"")
    }

    "handle FetchFilterAnalyticsRequest with filter requestType and only age filter" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val testDate = "2024-03-20"
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_7")).thenReturn("10")

      val request = FetchFilterAnalyticsRequest(
        sDate = Some(testDate),
        eDate = Some(testDate),
        filterGender = List(),
        filterLanguage = List(),
        filterAge = List("7"),
        requestType = "filter",
        id = "test-age-only-request"
      )

      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response

      responseJson should include("dateBased")
      responseJson should include(testDate)
      responseJson should include("\"y\":\"10\"")
    }

    "handle FetchFilterAnalyticsRequest with filter requestType and only language filter" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val testDate = "2024-03-20"
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::lang_english")).thenReturn("15")

      val request = FetchFilterAnalyticsRequest(
        sDate = Some(testDate),
        eDate = Some(testDate),
        filterGender = List(),
        filterLanguage = List("english"),
        filterAge = List(),
        requestType = "filter",
        id = "test-language-only-request"
      )

      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response

      responseJson should include("dateBased")
      responseJson should include(testDate)
      responseJson should include("\"y\":\"15\"")
    }

    "handle FetchFilterAnalyticsRequest with filter requestType and only gender filter" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val testDate = "2024-03-20"
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::gender_male")).thenReturn("20")

      val request = FetchFilterAnalyticsRequest(
        sDate = Some(testDate),
        eDate = Some(testDate),
        filterGender = List("male"),
        filterLanguage = List(),
        filterAge = List(),
        requestType = "filter",
        id = "test-gender-only-request"
      )

      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response

      responseJson should include("dateBased")
      responseJson should include(testDate)
      responseJson should include("\"y\":\"20\"")
    }

    "handle FetchFilterAnalyticsRequest with filter requestType and combined age+language filter" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val testDate = "2024-03-20"
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_7::lang_english")).thenReturn("25")

      val request = FetchFilterAnalyticsRequest(
        sDate = Some(testDate),
        eDate = Some(testDate),
        filterGender = List(),
        filterLanguage = List("english"),
        filterAge = List("7"),
        requestType = "filter",
        id = "test-age-language-request"
      )

      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response

      responseJson should include("dateBased")
      responseJson should include(testDate)
      responseJson should include("\"y\":\"25\"")
    }

    "handle FetchFilterAnalyticsRequest with filter requestType and combined gender+language filter" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val testDate = "2024-03-20"
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::gender_male::lang_english")).thenReturn("30")

      val request = FetchFilterAnalyticsRequest(
        sDate = Some(testDate),
        eDate = Some(testDate),
        filterGender = List("male"),
        filterLanguage = List("english"),
        filterAge = List(),
        requestType = "filter",
        id = "test-gender-language-request"
      )

      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response

      responseJson should include("dateBased")
      responseJson should include(testDate)
      responseJson should include("\"y\":\"30\"")
    }

    "handle FetchFilterAnalyticsRequest with filter requestType and combined age+gender filter" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val testDate = "2024-03-20"
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_7::gender_male")).thenReturn("35")

      val request = FetchFilterAnalyticsRequest(
        sDate = Some(testDate),
        eDate = Some(testDate),
        filterGender = List("male"),
        filterLanguage = List(),
        filterAge = List("7"),
        requestType = "filter",
        id = "test-age-gender-request"
      )

      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response

      responseJson should include("dateBased")
      responseJson should include(testDate)
      responseJson should include("\"y\":\"35\"")
    }

    "test getOptValue utility method through result checking" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val validDate = "2024-03-20"
      when(mockRedis.get(anyString())).thenReturn("0")

      val requestWithValidDate = FetchFilterAnalyticsRequest(
        sDate = Some(validDate),
        eDate = Some(validDate),
        filterGender = List(),
        filterLanguage = List(),
        filterAge = List(),
        requestType = "normal",
        id = "test-valid-date"
      )

      resultAccumulator ! requestWithValidDate
      val validResponse = expectMsgType[FetchFilterAnalyticsResponse]

      validResponse.response should include("dateBased")
      validResponse.response should include(validDate)

      val requestWithNoneDate = FetchFilterAnalyticsRequest(
        sDate = None,
        eDate = Some(validDate),
        filterGender = List(),
        filterLanguage = List(),
        filterAge = List(),
        requestType = "normal",
        id = "test-none-date"
      )

      resultAccumulator ! requestWithNoneDate
      val responseWithNoneDate = expectMsgType[FetchFilterAnalyticsResponse]

      responseWithNoneDate.response should be("""{"dateBased":[],"dateBasedGender":[],"dateBasedLanguage":[]}""")
    }

    "test invalid date handling in requests" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val requestWithBothInvalid = FetchFilterAnalyticsRequest(
        sDate = None,
        eDate = None,
        filterGender = List(),
        filterLanguage = List(),
        filterAge = List(),
        requestType = "normal",
        id = "test-both-invalid"
      )

      resultAccumulator ! requestWithBothInvalid
      val responseWithBothInvalid = expectMsgType[FetchFilterAnalyticsResponse]

      responseWithBothInvalid.response should be("""{"dateBased":[],"dateBasedGender":[],"dateBasedLanguage":[]}""")
    }

    "test formatted dates in different months" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val startDate = "2024-01-15"
      val endDate = "2024-01-17"

      // Mock Redis for these dates
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserCounter + "2024-01-15")).thenReturn("10")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserCounter + "2024-01-16")).thenReturn("20")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserCounter + "2024-01-17")).thenReturn("30")
      when(mockRedis.get(anyString())).thenReturn(null)

      val request = FetchFilterAnalyticsRequest(
        sDate = Some(startDate),
        eDate = Some(endDate),
        filterGender = List(),
        filterLanguage = List(),
        filterAge = List(),
        requestType = "normal",
        id = "test-formatted-dates"
      )

      resultAccumulator ! request
      val response = expectMsgType[FetchFilterAnalyticsResponse]

      response.response should include("2024-01-15")
      response.response should include("2024-01-16")
      response.response should include("2024-01-17")
    }

    "test double digit formatting through months with single digits" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val dates = List(
        "2024-01-05", // Tests single-digit month
        "2024-05-07", // Tests single-digit day
        "2024-12-25"  // Tests double-digit month and day
      )

      // Mock Redis responses for these dates
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserCounter + "2024-01-05")).thenReturn("10")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserCounter + "2024-05-07")).thenReturn("20")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_DayUserCounter + "2024-12-25")).thenReturn("30")
      when(mockRedis.get(anyString())).thenReturn(null)

      for (date <- dates) {
        val request = FetchFilterAnalyticsRequest(
          sDate = Some(date),
          eDate = Some(date),
          filterGender = List(),
          filterLanguage = List(),
          filterAge = List(),
          requestType = "normal",
          id = s"test-double-digits-$date"
        )

        resultAccumulator ! request
        val response = expectMsgType[FetchFilterAnalyticsResponse]

        response.response should include(date)
      }
    }

    "handle invalid date range correctly" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val request = FetchFilterAnalyticsRequest(
        sDate = None,
        eDate = None,
        filterGender = List("male"),
        filterLanguage = List("english"),
        filterAge = List("7"),
        requestType = "normal",
        id = "test-invalid-dates"
      )

      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response

      responseJson shouldBe """{"dateBased":[],"dateBasedGender":[],"dateBasedLanguage":[]}"""
    }

    "handle multiple ages, languages and genders in filter request" in {
      val resultAccumulator = system.actorOf(Props[ResultAccumulator])

      val testDate = "2024-03-20"

      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_6::gender_male::lang_english")).thenReturn("10")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_7::gender_female::lang_tamil")).thenReturn("20")

      when(mockRedis.get(anyString())).thenReturn(null)

      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_6::gender_male::lang_english")).thenReturn("10")
      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_7::gender_female::lang_tamil")).thenReturn("20")

      when(mockRedis.get(ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + testDate + "::age_6::gender_female::lang_tamil")).thenReturn("30")

      val request = FetchFilterAnalyticsRequest(
        sDate = Some(testDate),
        eDate = Some(testDate),
        filterGender = List("male", "female"),
        filterLanguage = List("english", "tamil"),
        filterAge = List("6", "7"),
        requestType = "filter",
        id = "test-multiple-filters"
      )

      resultAccumulator ! request

      val response = expectMsgType[FetchFilterAnalyticsResponse]
      val responseJson = response.response

      responseJson should include("dateBased")
      responseJson should include(testDate)
      responseJson should include("\"y\":\"60\"")
    }
  }
}
