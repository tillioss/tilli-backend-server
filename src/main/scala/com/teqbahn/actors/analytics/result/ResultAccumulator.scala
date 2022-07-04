package com.teqbahn.actors.analytics.result

import akka.actor.{Actor, PoisonPill}
import com.fasterxml.jackson.databind.ObjectMapper
import com.teqbahn.bootstrap.StarterMain
import com.teqbahn.bootstrap.StarterMain.redisCommands
import com.teqbahn.caseclasses.{FetchAnalyticsRequest, FetchAnalyticsResponse, FetchFilterAnalyticsRequest, FetchFilterAnalyticsResponse, FetchFilterUserAttemptAnalyticsRequest, FetchFilterUserAttemptAnalyticsResponse}
import com.teqbahn.global.ZiRedisCons
import org.joda.time.{DateTime, Days}
import org.json4s.NoTypeHints
import org.json4s.native.Serialization

import java.time.LocalDateTime
import org.json4s.jackson.Serialization.write

class ResultAccumulator extends Actor {
  var actorSystem = this.context.system
  implicit val formats = Serialization.formats(NoTypeHints)

  override def postStop(): Unit = {
    println("At : " + LocalDateTime.now() + ", From node : " + StarterMain.akkaManagementHostName + ", Path : " + self.path.toString + ", Msg : " + "ResultAccumulator got PoisonPill")
  }

  def receive: Receive = {

    case request: FetchAnalyticsRequest =>
      val objectMapper = new ObjectMapper();
      println("At : " + LocalDateTime.now() + " Received Msg ResultAccumulator : " + write(request))
      val responseObj = objectMapper.createObjectNode()

      val dateBasedArrayNode = objectMapper.createArrayNode()
      val dateBasedGenderFilterArrayNode = objectMapper.createArrayNode()
      val dateBasedLanguageFilterArrayNode = objectMapper.createArrayNode()
      val genderArray = Array("male", "female")
      val languageArray = Array("sinhala", "tamil", "english")

      //+++++++++++++++++++++++++++++++
      // For Gender Based Data Fetch :
      val genderBasedArrayNode = objectMapper.createArrayNode()
      for (gender <- genderArray) {
        val indexGender = ZiRedisCons.ACCUMULATOR_GenderUserCounter + gender
        val genderObjNode = objectMapper.createObjectNode()
        var genderCounter = redisCommands.get(indexGender)
        if (genderCounter != null && !genderCounter.equalsIgnoreCase("null") && !genderCounter.isEmpty) {
        } else {
          genderCounter = "0"
        }
        genderObjNode.put("x", gender)
        genderObjNode.put("y", genderCounter.toString)
        genderBasedArrayNode.add(genderObjNode)
      }

      //+++++++++++++++++++++++++++++++
      // For Gender Based Data Fetch :
      val languageBasedArrayNode = objectMapper.createArrayNode()
      for (language <- languageArray) {
        val indexLanguage = ZiRedisCons.ACCUMULATOR_LanguageUserCounter + language
        val languageObjNode = objectMapper.createObjectNode()
        var languageCounter = redisCommands.get(indexLanguage)
        if (languageCounter != null && !languageCounter.equalsIgnoreCase("null") && !languageCounter.isEmpty) {
        } else {
          languageCounter = "0"
        }
        languageObjNode.put("x", language)
        languageObjNode.put("y", languageCounter.toString)
        languageBasedArrayNode.add(languageObjNode)
      }

      //+++++++++++++++++++++++++++++++
      // For Age Based Data Fetch :
      val ageBasedArrayNode = objectMapper.createArrayNode()
      for (age <- 5 to 10) {
        val indexAge = ZiRedisCons.ACCUMULATOR_AgeUserCounter + age
        val ageObjNode = objectMapper.createObjectNode()
        var ageCounter = redisCommands.get(indexAge)
        if (ageCounter != null && !ageCounter.equalsIgnoreCase("null") && !ageCounter.isEmpty) {
        } else {
          ageCounter = "0"
        }
        ageObjNode.put("x", age.toString)
        ageObjNode.put("y", ageCounter.toString)
        ageBasedArrayNode.add(ageObjNode)
      }

      responseObj.set("genderBased", genderBasedArrayNode);
      responseObj.set("languageBased", languageBasedArrayNode);
      responseObj.set("ageBased", ageBasedArrayNode);

      sender() ! FetchAnalyticsResponse(responseObj.toString)
      context.stop(self)


    case request: FetchFilterUserAttemptAnalyticsRequest =>

      val objectMapper = new ObjectMapper();
      // println("At : " + LocalDateTime.now() + " Received Msg FetchFilterAnalyticsRequest : " + write(request))
      val sDateTuple = getOptValue(request.sDate)
      val eDateTuple = getOptValue(request.eDate)
      var sDateStr = "2021-08-01" // YYYY-MM-DD
      var eDateStr = "2021-08-10"

      val responseObj = objectMapper.createObjectNode()
      val dateBasedUserAttemptArrayNode = objectMapper.createArrayNode()
      val dateBasedUniqueUserAttemptArrayNode = objectMapper.createArrayNode()
      if (sDateTuple._1 && eDateTuple._1) {
        sDateStr = sDateTuple._2
        eDateStr = eDateTuple._2

        val sDate = DateTime.parse(sDateStr)
        val eDate = DateTime.parse(eDateStr)
        val daysCount = Days.daysBetween(sDate, eDate).getDays() + 1
        (0 until daysCount).map(sDate.plusDays(_)).foreach(d => {
          val dateStr = getDateStr(d)

          val userAttemptObjNode = objectMapper.createObjectNode()

          val attemptIndex = ZiRedisCons.ACCUMULATOR_DayUserAttemptCounter + dateStr
          var attemptCounter = redisCommands.get(attemptIndex)
          if (attemptCounter != null && !attemptCounter.equalsIgnoreCase("null") && !attemptCounter.isEmpty) {
          } else {
            attemptCounter = "0"
          }
          userAttemptObjNode.put("x", dateStr)
          userAttemptObjNode.put("y", attemptCounter.toString())
          dateBasedUserAttemptArrayNode.add(userAttemptObjNode)


          val uniqueUserAttemptObjNode = objectMapper.createObjectNode()

          val uniqueAttemptIndex = ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptCounter + dateStr
          var uniqueAttemptCounter = redisCommands.get(uniqueAttemptIndex)
          if (uniqueAttemptCounter != null && !uniqueAttemptCounter.equalsIgnoreCase("null") && !uniqueAttemptCounter.isEmpty) {
          } else {
            uniqueAttemptCounter = "0"
          }
          uniqueUserAttemptObjNode.put("x", dateStr)
          uniqueUserAttemptObjNode.put("y", uniqueAttemptCounter.toString())
          dateBasedUniqueUserAttemptArrayNode.add(uniqueUserAttemptObjNode)


        })


      }

      responseObj.set("dateBasedAttempt", dateBasedUserAttemptArrayNode);
      responseObj.set("dateBasedUniqueUserAttempt", dateBasedUniqueUserAttemptArrayNode);

      sender() ! FetchFilterUserAttemptAnalyticsResponse(responseObj.toString)
      context.stop(self)


    case request: FetchFilterAnalyticsRequest =>
      val objectMapper = new ObjectMapper();
      println("At : " + LocalDateTime.now() + " Received Msg ResultAccumulator : " + write(request))
      val sDateTuple = getOptValue(request.sDate)
      val eDateTuple = getOptValue(request.eDate)
      var sDateStr = "2021-08-01" // YYYY-MM-DD
      var eDateStr = "2021-08-10"

      val responseObj = objectMapper.createObjectNode()
      val dateBasedArrayNode = objectMapper.createArrayNode()
      val dateBasedGenderFilterArrayNode = objectMapper.createArrayNode()
      val dateBasedLanguageFilterArrayNode = objectMapper.createArrayNode()
      val genderArray = request.filterGender
      val languageArray = request.filterLanguage
      val ageArray = request.filterAge
      val requestType = request.requestType
      if (sDateTuple._1 && eDateTuple._1) {
        sDateStr = sDateTuple._2
        eDateStr = eDateTuple._2

        // For date Based Data Fetch :
        val sDate = DateTime.parse(sDateStr)
        val eDate = DateTime.parse(eDateStr)
        val daysCount = Days.daysBetween(sDate, eDate).getDays() + 1
        (0 until daysCount).map(sDate.plusDays(_)).foreach(d => {
          val dateStr = getDateStr(d)
          val objNode = objectMapper.createObjectNode()

          if (requestType != "filter") {
            val index = ZiRedisCons.ACCUMULATOR_DayUserCounter + dateStr
            var counter = redisCommands.get(index)
            if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
            } else {
              counter = "0"
            }
            objNode.put("x", dateStr)
            objNode.put("y", counter.toString())
            dateBasedArrayNode.add(objNode)


            val genderNode = objectMapper.createObjectNode()
            val languageNode = objectMapper.createObjectNode()
            genderNode.put("x", dateStr)
            for (gender <- genderArray) {
              val genderIndex = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + dateStr + "::gender_" + gender
              counter = redisCommands.get(genderIndex)
              if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
              } else {
                counter = "0"
              }
              genderNode.put("y-" + gender, counter.toString())
            }
            dateBasedGenderFilterArrayNode.add(genderNode)

            languageNode.put("x", dateStr)
            for (language <- languageArray) {
              val genderIndex = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + dateStr + "::lang_" + language
              counter = redisCommands.get(genderIndex)
              if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
              } else {
                counter = "0"
              }
              languageNode.put("y-" + language, counter.toString())
            }
            dateBasedLanguageFilterArrayNode.add(languageNode)
          } else {
            var totalCounter = 0L
            /*
            1. Age not empty, language not empty, gender not empty
            2. Age not empty, language not empty
            3. Age not empty, gender not empty
            4. language not empty, gender not empty
            5. Age not empty
            6. language not empty
            7. gender not empty
             */
            if (!ageArray.isEmpty && !languageArray.isEmpty && !genderArray.isEmpty) {
              for (age <- ageArray) {
                for (gender <- genderArray) {
                  for (language <- languageArray) {
                    val filterIndex = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + dateStr + "::age_" + age + "::gender_" + gender + "::lang_" + language
                    var counter = redisCommands.get(filterIndex)

                    if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
                    } else {
                      counter = "0"
                    }
                    totalCounter = totalCounter + counter.toLong
                  }
                }
              }
            } else if (!ageArray.isEmpty && !languageArray.isEmpty) {
              for (age <- ageArray) {
                for (language <- languageArray) {
                  val filterIndex = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + dateStr + "::age_" + age + "::lang_" + language
                  var counter = redisCommands.get(filterIndex)

                  if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
                  } else {
                    counter = "0"
                  }
                  totalCounter = totalCounter + counter.toLong
                }
              }
            } else if (!ageArray.isEmpty && !genderArray.isEmpty) {
              for (age <- ageArray) {
                for (gender <- genderArray) {
                  val filterIndex = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + dateStr + "::age_" + age + "::gender_" + gender
                  var counter = redisCommands.get(filterIndex)
                  if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
                  } else {
                    counter = "0"
                  }
                  totalCounter = totalCounter + counter.toLong
                }
              }
            } else if (!languageArray.isEmpty && !genderArray.isEmpty) {
              for (gender <- genderArray) {
                for (language <- languageArray) {
                  val filterIndex = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + dateStr + "::gender_" + gender + "::lang_" + language
                  var counter = redisCommands.get(filterIndex)

                  if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
                  } else {
                    counter = "0"
                  }
                  totalCounter = totalCounter + counter.toLong
                }
              }
            } else if (!ageArray.isEmpty) {
              for (age <- ageArray) {
                val filterIndex = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + dateStr + "::age_" + age
                var counter = redisCommands.get(filterIndex)
                if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
                } else {
                  counter = "0"
                }
                totalCounter = totalCounter + counter.toLong
              }
            } else if (!languageArray.isEmpty) {
              for (language <- languageArray) {
                val filterIndex = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + dateStr + "::lang_" + language
                var counter = redisCommands.get(filterIndex)

                if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
                } else {
                  counter = "0"
                }
                totalCounter = totalCounter + counter.toLong
              }
            } else if (!genderArray.isEmpty) {
              for (gender <- genderArray) {
                val filterIndex = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + dateStr + "::gender_" + gender
                var counter = redisCommands.get(filterIndex)

                if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
                } else {
                  counter = "0"
                }
                totalCounter = totalCounter + counter.toLong
              }
            }
            objNode.put("x", dateStr)
            objNode.put("y", totalCounter.toString())
            dateBasedArrayNode.add(objNode)
          }
        })
      }

      responseObj.set("dateBased", dateBasedArrayNode);
      responseObj.set("dateBasedGender", dateBasedGenderFilterArrayNode);
      responseObj.set("dateBasedLanguage", dateBasedLanguageFilterArrayNode);

      sender() ! FetchFilterAnalyticsResponse(responseObj.toString)
      context.stop(self)
  }

  def getOptValue(data: Option[String]): Tuple2[Boolean, String] = {
    if (data != None && data != null) {
      if (data.get != null && !data.isEmpty) {
        return (true, data.get)
      }
    }
    return (false, "")
  }

  def getDateStr(d: DateTime): String = {
    d.getYear + "-" + getDoubeDigit(d.getMonthOfYear) + "-" + getDoubeDigit(d.getDayOfMonth)
  }

  def getDoubeDigit(d: Int): String = {
    "%02d".format(d)
  }
}
