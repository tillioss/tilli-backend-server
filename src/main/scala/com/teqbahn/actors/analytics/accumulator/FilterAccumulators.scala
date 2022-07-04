package com.teqbahn.actors.analytics.accumulator

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorContext, ActorRef, PoisonPill, ReceiveTimeout}
import com.teqbahn.bootstrap.StarterMain.redisCommands
import com.teqbahn.caseclasses._
import com.teqbahn.global.ZiRedisCons
import org.json4s.NoTypeHints
import org.json4s.native.Serialization

class FilterAccumulators extends Actor {
  var actorSystem = this.context.system
  implicit val formats = Serialization.formats(NoTypeHints)

  def receive: Receive = {
    case request: AddToFilterAccumulationWrapper =>
      val accumulationDate: AccumulationDate = request.accumulationDate
      val accumulationRequest: AddToAccumulationRequest = request.accumulator
      val dataType = accumulationRequest.dataType
      if (dataType != null && dataType.equalsIgnoreCase("User")) {
        if (accumulationRequest.accumulation != None && accumulationRequest.accumulation != null) {
          val user: UserAccumulation = accumulationRequest.accumulation.get
          val age = user.ageOfChild
          val language = user.language
          val genderOfChild = user.genderOfChild
          val userId = user.userId

          if (age != null && age != None && !age.isEmpty) {
            val checkAgeMapkey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_" + "map"
            if (!redisCommands.hexists(checkAgeMapkey, userId)) {
              val key1 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "age_" + age
              redisCommands.incr(key1)
              val key2 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "age_" + age
              redisCommands.incr(key2)
              val key3 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "age_" + age
              redisCommands.incr(key3)

              redisCommands.hset(checkAgeMapkey, userId, "1")
            }

          }
          if (language != null && language != None && !language.isEmpty) {
            val checkLangMapkey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "lang_" + "map"
            if (!redisCommands.hexists(checkLangMapkey, userId)) {
              val key1 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key1)
              val key2 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key2)
              val key3 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key3)

              redisCommands.hset(checkLangMapkey, userId, "1")

            }
          }
          if (genderOfChild != null && genderOfChild != None && !genderOfChild.isEmpty) {
            val checkGenderMapkey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "gender_" + "map"
            if (!redisCommands.hexists(checkGenderMapkey, userId)) {
              val key1 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild
              redisCommands.incr(key1)
              val key2 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild
              redisCommands.incr(key2)
              val key3 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild
              redisCommands.incr(key3)

              redisCommands.hset(checkGenderMapkey, userId, "1")

            }
          }

          if (age != null && age != None && !age.isEmpty && genderOfChild != null && genderOfChild != None && !genderOfChild.isEmpty) {
            val checkAgeGenderMapkey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_gender_" + "map"
            if (!redisCommands.hexists(checkAgeGenderMapkey, userId)) {

              val key1 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild
              redisCommands.incr(key1)
              val key2 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild
              redisCommands.incr(key2)
              val key3 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild
              redisCommands.incr(key3)
              redisCommands.hset(checkAgeGenderMapkey, userId, "1")

            }
          }
          if (age != null && age != None && !age.isEmpty && language != null && language != None && !language.isEmpty) {
            val checkAgeLangMapkey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_lang_" + "map"
            if (!redisCommands.hexists(checkAgeLangMapkey, userId)) {
              val key1 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key1)
              val key2 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key2)
              val key3 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key3)
              redisCommands.hset(checkAgeLangMapkey, userId, "1")

            }
          }
          if (genderOfChild != null && genderOfChild != None && !genderOfChild.isEmpty && language != null && language != None && !language.isEmpty) {
            val checkGenderLangMapkey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "gender_lang_" + "map"
            if (!redisCommands.hexists(checkGenderLangMapkey, userId)) {
              val key1 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key1)
              val key2 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key2)
              val key3 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key3)
              redisCommands.hset(checkGenderLangMapkey, userId, "1")

            }
          }
          if (age != null && age != None && !age.isEmpty && genderOfChild != null && genderOfChild != None && !genderOfChild.isEmpty && language != null && language != None && !language.isEmpty) {
            val checkAgeGenderLangMapkey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_gender_lang_" + "map"
            if (!redisCommands.hexists(checkAgeGenderLangMapkey, userId)) {
              val key1 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key1)
              val key2 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key2)
              val key3 = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + "age_" + age + ZiRedisCons.SEPARATOR + "gender_" + genderOfChild + ZiRedisCons.SEPARATOR + "lang_" + language
              redisCommands.incr(key3)
              redisCommands.hset(checkAgeGenderLangMapkey, userId, "1")
            }
          }
        }
      }

    case ReceiveTimeout =>  context.stop(self)
  }



}
