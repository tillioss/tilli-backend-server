package com.teqbahn.actors.analytics.accumulator

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorContext, ActorRef, PoisonPill, ReceiveTimeout}
import com.teqbahn.bootstrap.StarterMain.redisCommands
import com.teqbahn.caseclasses.AddToAccumulationWrapper
import com.teqbahn.global.ZiRedisCons
import org.json4s.NoTypeHints
import org.json4s.native.Serialization

class LanguageAccumulators extends Actor {
  var actorSystem = this.context.system
  implicit val formats = Serialization.formats(NoTypeHints)
  var indexExist = false;

  def receive: Receive = {
    case request: AddToAccumulationWrapper =>
      val index = ZiRedisCons.ACCUMULATOR_LanguageUserCounter + request.id
      if (!indexExist) {
        val counter = redisCommands.get(index)
        if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
        } else {
          redisCommands.set(index, "0")
        }
        indexExist = true
      }
      redisCommands.incr(index)

    case ReceiveTimeout =>  context.stop(self)
  }




}
