package com.teqbahn.actors.analytics.accumulator.time

import org.apache.pekko.actor.SupervisorStrategy.Stop
import org.apache.pekko.actor.{Actor, ActorContext, ActorRef, PoisonPill, ReceiveTimeout}
import com.teqbahn.bootstrap.StarterMain.redisCommands
import com.teqbahn.caseclasses.{AddToAccumulationWrapper, AddUserAttemptAccumulationWrapper}
import com.teqbahn.global.ZiRedisCons
import com.teqbahn.utils.ZiFunctions
import org.json4s.NoTypeHints
import org.json4s.native.Serialization

class DayAccumulators extends Actor {
  var actorSystem = this.context.system
  implicit val formats = Serialization.formats(NoTypeHints)
  var indexExist = false;
  var attemptIndexExist = false;
  var unquieUserindexExist = false;

  override def preStart(): Unit = {
    ZiFunctions.printNodeInfo(self, "DayAccumulators Started")
  }

  override def postStop(): Unit = {
    ZiFunctions.printNodeInfo(self, "DayAccumulators got PoisonPill")
  }

  def receive: Receive = {
    case request: AddToAccumulationWrapper =>
      val index = ZiRedisCons.ACCUMULATOR_DayUserCounter + request.id
      if (!indexExist) {
        val counter = redisCommands.get(index)
        if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
        } else {
          redisCommands.set(index, "0")
        }
        indexExist = true
      }
      redisCommands.incr(index)

    case request: AddUserAttemptAccumulationWrapper =>
      val index = ZiRedisCons.ACCUMULATOR_DayUserAttemptCounter + request.id
      if (!attemptIndexExist) {
        val counter = redisCommands.get(index)
        if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
        } else {
          redisCommands.set(index, "0")
        }
        attemptIndexExist = true
      }
      redisCommands.incr(index)

      if (!redisCommands.sismember(ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptSet + request.id, request.accumulator.userid)) {
        redisCommands.sadd(ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptSet + request.id, request.accumulator.userid)


        val uniqueUserIndex = ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptCounter + request.id
        if (!unquieUserindexExist) {
          val uniqueUsercounter = redisCommands.get(uniqueUserIndex)
          if (uniqueUsercounter != null && !uniqueUsercounter.equalsIgnoreCase("null") && !uniqueUsercounter.isEmpty) {
          } else {
            redisCommands.set(uniqueUserIndex, "0")
          }
          unquieUserindexExist = true
        }
        redisCommands.incr(uniqueUserIndex)


      }


    case ReceiveTimeout => context.stop(self)
  }



}
