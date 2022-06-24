package com.teqbahn.actors.analytics.accumulator.time

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorContext, ActorRef, PoisonPill, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.teqbahn.bootstrap.StarterMain.redisCommands
import com.teqbahn.caseclasses.{AddToAccumulationWrapper, AddUserAttemptAccumulationWrapper}
import com.teqbahn.global.ZiRedisCons
import com.teqbahn.utils.ZiFunctions
import org.json4s.NoTypeHints
import org.json4s.native.Serialization

class MonthAccumulators extends Actor {
  var actorSystem = this.context.system
  implicit val formats = Serialization.formats(NoTypeHints)
  var indexExist = false;
  var attemptIndexExist = false;
  var unquieUserindexExist = false;

  override def preStart(): Unit = {
    ZiFunctions.printNodeInfo(self, "MonthAccumulators Started")
  }

  def receive: Receive = {
    case request: AddToAccumulationWrapper =>
      val index = ZiRedisCons.ACCUMULATOR_MonthUserCounter + request.id
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
      val index = ZiRedisCons.ACCUMULATOR_MonthUserAttemptCounter + request.id
      if (!attemptIndexExist) {
        val counter = redisCommands.get(index)
        if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
        } else {
          redisCommands.set(index, "0")
        }
        attemptIndexExist = true
      }
      redisCommands.incr(index)

      if (!redisCommands.sismember(ZiRedisCons.ACCUMULATOR_MonthUniqueUserAttemptSet + request.id, request.accumulator.userid)) {
        redisCommands.sadd(ZiRedisCons.ACCUMULATOR_MonthUniqueUserAttemptSet + request.id, request.accumulator.userid)


        val uniqueUserIndex = ZiRedisCons.ACCUMULATOR_MonthUniqueUserAttemptCounter + request.id
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


    case ReceiveTimeout => context.parent ! Passivate(stopMessage = Stop)
  }

  private def passivate(context: ActorContext, self: ActorRef): Unit = {
    context.parent ! (new ShardRegion.Passivate(PoisonPill.getInstance), self)
  }
}
