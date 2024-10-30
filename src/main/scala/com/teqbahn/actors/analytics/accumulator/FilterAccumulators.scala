package com.teqbahn.actors.analytics.accumulator

import com.teqbahn.caseclasses._
import com.teqbahn.global.ZiRedisCons
import zio._
import zio.redis._
import zio.redis.api._
import zio.duration._

object FilterAccumulators {

  implicit val formats = org.json4s.native.Serialization.formats(org.json4s.NoTypeHints)

  def handleAddToFilterAccumulation(request: AddToFilterAccumulationWrapper): ZIO[Redis, RedisError, Unit] = {
    val accumulationDate = request.accumulationDate
    val accumulationRequest = request.accumulator
    val dataType = accumulationRequest.dataType

    if (Option(dataType).exists(_.equalsIgnoreCase("User"))) {
      accumulationRequest.accumulation match {
        case Some(user) =>
          val age = user.ageOfChild
          val language = user.language
          val genderOfChild = user.genderOfChild
          val userId = user.userId

          for {
            _ <- handleAgeFilter(accumulationDate, userId, age)
            _ <- handleLanguageFilter(accumulationDate, userId, language)
            _ <- handleGenderFilter(accumulationDate, userId, genderOfChild)
            _ <- handleAgeGenderFilter(accumulationDate, userId, age, genderOfChild)
            _ <- handleAgeLangFilter(accumulationDate, userId, age, language)
            _ <- handleGenderLangFilter(accumulationDate, userId, genderOfChild, language)
            _ <- handleAgeGenderLangFilter(accumulationDate, userId, age, genderOfChild, language)
          } yield ()
        case None => ZIO.unit
      }
    } else ZIO.unit
  }

  def handleAgeFilter(accumulationDate: AccumulationDate, userId: String, age: String): ZIO[Redis, RedisError, Unit] = {
    if (Option(age).exists(_.nonEmpty)) {
      val checkAgeMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_map"
      for {
        exists <- hExists(checkAgeMapKey, userId)
        _ <- if (!exists) {
          for {
            _ <- incrementKeysForDate(accumulationDate, "age", age)
            _ <- hSet(checkAgeMapKey, userId, "1")
          } yield ()
        } else ZIO.unit
      } yield ()
    } else ZIO.unit
  }

  def handleLanguageFilter(accumulationDate: AccumulationDate, userId: String, language: String): ZIO[Redis, RedisError, Unit] = {
    if (Option(language).exists(_.nonEmpty)) {
      val checkLangMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "lang_map"
      for {
        exists <- hExists(checkLangMapKey, userId)
        _ <- if (!exists) {
          for {
            _ <- incrementKeysForDate(accumulationDate, "lang", language)
            _ <- hSet(checkLangMapKey, userId, "1")
          } yield ()
        } else ZIO.unit
      } yield ()
    } else ZIO.unit
  }

  def handleGenderFilter(accumulationDate: AccumulationDate, userId: String, gender: String): ZIO[Redis, RedisError, Unit] = {
    if (Option(gender).exists(_.nonEmpty)) {
      val checkGenderMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "gender_map"
      for {
        exists <- hExists(checkGenderMapKey, userId)
        _ <- if (!exists) {
          for {
            _ <- incrementKeysForDate(accumulationDate, "gender", gender)
            _ <- hSet(checkGenderMapKey, userId, "1")
          } yield ()
        } else ZIO.unit
      } yield ()
    } else ZIO.unit
  }

  def handleAgeGenderFilter(accumulationDate: AccumulationDate, userId: String, age: String, gender: String): ZIO[Redis, RedisError, Unit] = {
    if (Option(age).exists(_.nonEmpty) && Option(gender).exists(_.nonEmpty)) {
      val checkAgeGenderMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_gender_map"
      for {
        exists <- hExists(checkAgeGenderMapKey, userId)
        _ <- if (!exists) {
          for {
            _ <- incrementKeysForDate(accumulationDate, s"age_${age}_gender", gender)
            _ <- hSet(checkAgeGenderMapKey, userId, "1")
          } yield ()
        } else ZIO.unit
      } yield ()
    } else ZIO.unit
  }

  def handleAgeLangFilter(accumulationDate: AccumulationDate, userId: String, age: String, language: String): ZIO[Redis, RedisError, Unit] = {
    if (Option(age).exists(_.nonEmpty) && Option(language).exists(_.nonEmpty)) {
      val checkAgeLangMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_lang_map"
      for {
        exists <- hExists(checkAgeLangMapKey, userId)
        _ <- if (!exists) {
          for {
            _ <- incrementKeysForDate(accumulationDate, s"age_${age}_lang", language)
            _ <- hSet(checkAgeLangMapKey, userId, "1")
          } yield ()
        } else ZIO.unit
      } yield ()
    } else ZIO.unit
  }

  def handleGenderLangFilter(accumulationDate: AccumulationDate, userId: String, gender: String, language: String): ZIO[Redis, RedisError, Unit] = {
    if (Option(gender).exists(_.nonEmpty) && Option(language).exists(_.nonEmpty)) {
      val checkGenderLangMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "gender_lang_map"
      for {
        exists <- hExists(checkGenderLangMapKey, userId)
        _ <- if (!exists) {
          for {
            _ <- incrementKeysForDate(accumulationDate, s"gender_${gender}_lang", language)
            _ <- hSet(checkGenderLangMapKey, userId, "1")
          } yield ()
        } else ZIO.unit
      } yield ()
    } else ZIO.unit
  }

  def handleAgeGenderLangFilter(accumulationDate: AccumulationDate, userId: String, age: String, gender: String, language: String): ZIO[Redis, RedisError, Unit] = {
    if (Option(age).exists(_.nonEmpty) && Option(gender).exists(_.nonEmpty) && Option(language).exists(_.nonEmpty)) {
      val checkAgeGenderLangMapKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + "age_gender_lang_map"
      for {
        exists <- hExists(checkAgeGenderLangMapKey, userId)
        _ <- if (!exists) {
          for {
            _ <- incrementKeysForDate(accumulationDate, s"age_${age}_gender_${gender}_lang", language)
            _ <- hSet(checkAgeGenderLangMapKey, userId, "1")
          } yield ()
        } else ZIO.unit
      } yield ()
    } else ZIO.unit
  }

  def incrementKeysForDate(accumulationDate: AccumulationDate, keyPrefix: String, keyValue: String): ZIO[Redis, RedisError, Unit] = {
    val yearKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.year + ZiRedisCons.SEPARATOR + keyPrefix + ZiRedisCons.SEPARATOR + keyValue
    val monthKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.month + ZiRedisCons.SEPARATOR + keyPrefix + ZiRedisCons.SEPARATOR + keyValue
    val dateKey = ZiRedisCons.ACCUMULATOR_FILTER_SEPARATOR + accumulationDate.date + ZiRedisCons.SEPARATOR + keyPrefix + ZiRedisCons.SEPARATOR + keyValue

    for {
      _ <- incr(yearKey)
      _ <- incr(monthKey)
      _ <- incr(dateKey)
    } yield ()
  }

  def onTimeout: ZIO[Any, Nothing, Unit] =
    ZIO.logInfo("FilterAccumulators stopping due to timeout")
}
