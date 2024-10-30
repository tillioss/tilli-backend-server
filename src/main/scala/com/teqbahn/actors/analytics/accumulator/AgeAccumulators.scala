package com.teqbahn.actors.analytics.accumulator

import com.teqbahn.caseclasses.AddToAccumulationWrapper
import com.teqbahn.global.ZiRedisCons
import org.json4s.{NoTypeHints, native}
import zio._
import zio.redis._
import zio.redis.api._
import zio.duration._

object AgeAccumulators {

  implicit val formats = native.Serialization.formats(NoTypeHints)

  def start: ZIO[Any, Nothing, Unit] =
    runAccumulatorLoop

  def runAccumulatorLoop: ZIO[Any, Nothing, Unit] =
    ZIO.never.catchAll(_ => ZIO.unit)

  def handleAddToAccumulation(request: AddToAccumulationWrapper): ZIO[Redis, RedisError, Unit] = {
    val index = ZiRedisCons.ACCUMULATOR_AgeUserCounter + request.id
    for {
      exists <- get(index)
      _ <- exists match {
        case Some(_) => ZIO.unit
        case None => set(index, "0")
      }
      _ <- incr(index)
    } yield ()
  }

  def onTimeout: ZIO[Any, Nothing, Unit] =
    ZIO.logInfo("AgeAccumulators stopping due to timeout")
}
