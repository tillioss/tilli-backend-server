package com.teqbahn.actors.analytics.accumulator.time

import com.teqbahn.caseclasses.{AddToAccumulationWrapper, AddUserAttemptAccumulationWrapper}
import com.teqbahn.global.ZiRedisCons
import com.teqbahn.utils.ZiFunctions
import org.json4s.{NoTypeHints, native}
import zio._
import zio.redis._
import zio.redis.api._
import zio.duration._

object YearAccumulators {

  implicit val formats = native.Serialization.formats(NoTypeHints)

  def start: ZIO[Any, Nothing, Unit] =
    ZiFunctions.printNodeInfo("YearAccumulators Started") *> runAccumulatorLoop

  def runAccumulatorLoop: ZIO[Any, Nothing, Unit] =
    ZIO.never.catchAll(_ => ZIO.unit)

  def handleAddToAccumulation(request: AddToAccumulationWrapper): ZIO[Redis, RedisError, Unit] = {
    val index = ZiRedisCons.ACCUMULATOR_YearUserCounter + request.id
    for {
      exists <- get(index)
      _ <- exists match {
        case Some(_) => ZIO.unit
        case None => set(index, "0")
      }
      _ <- incr(index)
    } yield ()
  }

  def handleAddUserAttemptAccumulation(request: AddUserAttemptAccumulationWrapper): ZIO[Redis, RedisError, Unit] = {
    val index = ZiRedisCons.ACCUMULATOR_YearUserAttemptCounter + request.id
    val uniqueUserIndex = ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptCounter + request.id
    for {
      attemptExists <- get(index)
      _ <- attemptExists match {
        case Some(_) => ZIO.unit
        case None => set(index, "0")
      }
      _ <- incr(index)

      isUnique <- sismember(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptSet + request.id, request.accumulator.userid)
      _ <- if (!isUnique) {
        for {
          _ <- sadd(ZiRedisCons.ACCUMULATOR_YearUniqueUserAttemptSet + request.id, request.accumulator.userid)
          uniqueExists <- get(uniqueUserIndex)
          _ <- uniqueExists match {
            case Some(_) => ZIO.unit
            case None => set(uniqueUserIndex, "0")
          }
          _ <- incr(uniqueUserIndex)
        } yield ()
      } else ZIO.unit
    } yield ()
  }

  def onTimeout: ZIO[Any, Nothing, Unit] =
    ZIO.logInfo("YearAccumulators stopping due to timeout")
}
