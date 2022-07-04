package com.teqbahn.actors.analytics

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorContext, ActorRef, PoisonPill, ReceiveTimeout}
import com.teqbahn.bootstrap.StarterMain
import com.teqbahn.bootstrap.StarterMain.redisCommands
import com.teqbahn.caseclasses.{AccumulationDate, AddToAccumulationRequest, AddToAccumulationWrapper, AddToFilterAccumulationWrapper, AddUserAttemptAccumulationRequest, AddUserAttemptAccumulationWrapper, UpdateUserDetailsAccumulationRequest, User, UserAccumulation}
import com.teqbahn.global.ZiRedisCons
import com.teqbahn.utils.ZiFunctions
import org.json4s.NoTypeHints
import org.json4s.native.Serialization

class Accumulators extends Actor {
  var actorSystem = this.context.system
  implicit val formats = Serialization.formats(NoTypeHints)

  override def preStart(): Unit = {
    ZiFunctions.printNodeInfo(self, "Accumulators Started")
  }

  override def postStop(): Unit = {
    ZiFunctions.printNodeInfo(self, "Accumulators got PoisonPill")
  }

  def receive: Receive = {
    case request: AddToAccumulationRequest =>
      val accumulationDate = getAccumulationDate(request.createdAt)

      StarterMain.accumulatorDayActorRef ! AddToAccumulationWrapper(request, accumulationDate.date)
      StarterMain.accumulatorMonthActorRef ! AddToAccumulationWrapper(request, accumulationDate.month)
      StarterMain.accumulatorYearActorRef ! AddToAccumulationWrapper(request, accumulationDate.year)

      if (request.accumulation != null && request.accumulation != None) {
        val user: UserAccumulation = request.accumulation.get
        val age = user.ageOfChild
        val language = user.language
        val genderOfChild = user.genderOfChild

        if (age != null && age != None && !age.isEmpty) {
          StarterMain.accumulatorAgeActorRef ! AddToAccumulationWrapper(request, age)
        }
        if (language != null && language != None && !language.isEmpty) {
          StarterMain.accumulatorLanguageActorRef ! AddToAccumulationWrapper(request, language)
        }
        if (genderOfChild != null && genderOfChild != None && !genderOfChild.isEmpty) {
          StarterMain.accumulatorGenderActorRef ! AddToAccumulationWrapper(request, genderOfChild)
        }

        StarterMain.accumulatorFilterActorRef ! AddToFilterAccumulationWrapper(request, ZiFunctions.getId(), accumulationDate)

      }
    case request: AddUserAttemptAccumulationRequest =>

      val accumulationDate = getAccumulationDate(request.createdAt)

      StarterMain.accumulatorDayActorRef ! AddUserAttemptAccumulationWrapper(request, accumulationDate.date)
      StarterMain.accumulatorMonthActorRef ! AddUserAttemptAccumulationWrapper(request, accumulationDate.month)
      StarterMain.accumulatorYearActorRef ! AddUserAttemptAccumulationWrapper(request, accumulationDate.year)


    case request: UpdateUserDetailsAccumulationRequest =>
      val accumulationDate = getAccumulationDate(request.createdAt)

      if (request.accumulation != null && request.accumulation != None) {
        val user: UserAccumulation = request.accumulation.get
        val age = user.ageOfChild
        val language = user.language
        val genderOfChild = user.genderOfChild

        var addToAccumulationRequest = AddToAccumulationRequest("User", request.accumulation, request.createdAt)

        if (age != null && age != None && !age.isEmpty) {
          val ageCheckMapKey = ZiRedisCons.ACCUMULATOR_AgeUserCounter + "checkMap"
          if (!redisCommands.hexists(ageCheckMapKey, user.userId)) {
            StarterMain.accumulatorAgeActorRef ! AddToAccumulationWrapper(addToAccumulationRequest, age)
            redisCommands.hset(ageCheckMapKey, user.userId, "1")
          }
        }
        if (language != null && language != None && !language.isEmpty) {
          val langCheckMapKey = ZiRedisCons.ACCUMULATOR_LanguageUserCounter + "checkMap"
          if (!redisCommands.hexists(langCheckMapKey, user.userId)) {
            StarterMain.accumulatorLanguageActorRef ! AddToAccumulationWrapper(addToAccumulationRequest, language)
            redisCommands.hset(langCheckMapKey, user.userId, "1")

          }
        }
        if (genderOfChild != null && genderOfChild != None && !genderOfChild.isEmpty) {
          val genderCheckMapKey = ZiRedisCons.ACCUMULATOR_GenderUserCounter + "checkMap"
          if (!redisCommands.hexists(genderCheckMapKey, user.userId)) {
            StarterMain.accumulatorGenderActorRef ! AddToAccumulationWrapper(addToAccumulationRequest, genderOfChild)
            redisCommands.hset(genderCheckMapKey, user.userId, "1")
          }
        }

        StarterMain.accumulatorFilterActorRef ! AddToFilterAccumulationWrapper(addToAccumulationRequest, ZiFunctions.getId(), accumulationDate)

      }


    case ReceiveTimeout =>  context.stop(self)
  }



  def getAccumulationDate(time: Long): AccumulationDate = {
    import java.text.SimpleDateFormat
    val simpleDateFormat = new SimpleDateFormat("YYY-MM-dd")
    val format = simpleDateFormat.format(time).toString
    val formatArray = format.split("-")
    AccumulationDate(formatArray(0), formatArray(0) + "-" + formatArray(1), format)
  }
}