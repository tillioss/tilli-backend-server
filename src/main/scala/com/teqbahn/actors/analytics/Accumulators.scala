package com.teqbahn.actors.analytics

import zio._
import zio.redis._
import com.teqbahn.caseclasses._
import com.teqbahn.global.ZiRedisCons
import com.teqbahn.utils.ZiFunctions
import java.text.SimpleDateFormat

case class Accumulators(redis: Redis.Service) {

  def addToAccumulation(request: AddToAccumulationRequest): ZIO[Any, RedisError, Unit] = {
    val accumulationDate = getAccumulationDate(request.createdAt)
    
    // Handle accumulation based on day, month, year
    val accumulationActions = for {
      _ <- sendToAccumulator(StarterMain.accumulatorDayActorRef, request, accumulationDate.date)
      _ <- sendToAccumulator(StarterMain.accumulatorMonthActorRef, request, accumulationDate.month)
      _ <- sendToAccumulator(StarterMain.accumulatorYearActorRef, request, accumulationDate.year)
      _ <- handleUserAccumulation(request, accumulationDate)
    } yield ()

    accumulationActions
  }

  def handleUserAccumulation(request: AddToAccumulationRequest, accumulationDate: AccumulationDate): ZIO[Any, RedisError, Unit] = {
    request.accumulation match {
      case Some(user) =>
        val age        = user.ageOfChild
        val language   = user.language
        val gender     = user.genderOfChild

        // Handle age, language, gender-specific accumulation
        for {
          _ <- ZIO.when(age.isDefined && age.get.nonEmpty)(sendToAccumulator(StarterMain.accumulatorAgeActorRef, request, age.get))
          _ <- ZIO.when(language.isDefined && language.get.nonEmpty)(sendToAccumulator(StarterMain.accumulatorLanguageActorRef, request, language.get))
          _ <- ZIO.when(gender.isDefined && gender.get.nonEmpty)(sendToAccumulator(StarterMain.accumulatorGenderActorRef, request, gender.get))
          _ <- sendToAccumulatorFilter(request, accumulationDate)
        } yield ()
      
      case None => ZIO.unit
    }
  }

  def handleUserAttempt(request: AddUserAttemptAccumulationRequest): ZIO[Any, RedisError, Unit] = {
    val accumulationDate = getAccumulationDate(request.createdAt)

    // Send to day, month, and year accumulators
    for {
      _ <- sendToAccumulator(StarterMain.accumulatorDayActorRef, request, accumulationDate.date)
      _ <- sendToAccumulator(StarterMain.accumulatorMonthActorRef, request, accumulationDate.month)
      _ <- sendToAccumulator(StarterMain.accumulatorYearActorRef, request, accumulationDate.year)
    } yield ()
  }

  def updateUserDetails(request: UpdateUserDetailsAccumulationRequest): ZIO[Any, RedisError, Unit] = {
    val accumulationDate = getAccumulationDate(request.createdAt)
    
    request.accumulation match {
      case Some(user) =>
        val age        = user.ageOfChild
        val language   = user.language
        val gender     = user.genderOfChild

        val addToAccumulationRequest = AddToAccumulationRequest("User", request.accumulation, request.createdAt)

        for {
          _ <- checkAndAccumulate(ZiRedisCons.ACCUMULATOR_AgeUserCounter + "checkMap", user.userId, age, StarterMain.accumulatorAgeActorRef, addToAccumulationRequest)
          _ <- checkAndAccumulate(ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "checkMap", user.userId, language, StarterMain.accumulatorLanguageActorRef, addToAccumulationRequest)
          _ <- checkAndAccumulate(ZiRedisCons.ACCUMULATOR_GenderUserCounter + "checkMap", user.userId, gender, StarterMain.accumulatorGenderActorRef, addToAccumulationRequest)
          _ <- sendToAccumulatorFilter(addToAccumulationRequest, accumulationDate)
        } yield ()
      
      case None => ZIO.unit
    }
  }

  private def checkAndAccumulate(
    checkMapKey: String, 
    userId: String, 
    attribute: Option[String], 
    actorRef: ActorRef, 
    request: AddToAccumulationRequest
  ): ZIO[Any, RedisError, Unit] = {
    attribute match {
      case Some(value) if value.nonEmpty =>
        for {
          exists <- redis.hexists(checkMapKey, userId)
          _ <- ZIO.when(!exists) {
            sendToAccumulator(actorRef, request, value) *>
              redis.hset(checkMapKey, userId, "1")
          }
        } yield ()
      case _ => ZIO.unit
    }
  }

  private def sendToAccumulator(actorRef: ActorRef, request: Any, value: String): ZIO[Any, Nothing, Unit] = 
    ZIO.succeed(actorRef ! AddToAccumulationWrapper(request.asInstanceOf[AddToAccumulationRequest], value))

  private def sendToAccumulatorFilter(request: AddToAccumulationRequest, accumulationDate: AccumulationDate): ZIO[Any, Nothing, Unit] = 
    ZIO.succeed(StarterMain.accumulatorFilterActorRef ! AddToFilterAccumulationWrapper(request, ZiFunctions.getId(), accumulationDate))

  private def getAccumulationDate(time: Long): AccumulationDate = {
    val simpleDateFormat = new SimpleDateFormat("YYY-MM-dd")
    val format = simpleDateFormat.format(time).toString
    val formatArray = format.split("-")
    AccumulationDate(formatArray(0), formatArray(0) + "-" + formatArray(1), format)
  }

}

object Accumulators {

  def create: ZLayer[Redis, Nothing, Accumulators] = ZLayer.fromFunction { redis =>
    Accumulators(redis.get)
  }

}
