import zio._
import zio.redis._
import com.teqbahn.caseclasses.{AddToAccumulationWrapper, AddUserAttemptAccumulationWrapper}
import com.teqbahn.global.ZiRedisCons
import com.teqbahn.utils.ZiFunctions

case class DayAccumulators(redis: Redis.Service, indexExist: Ref[Boolean], attemptIndexExist: Ref[Boolean], uniqueUserIndexExist: Ref[Boolean]) {

  def addToAccumulation(request: AddToAccumulationWrapper): ZIO[Any, RedisError, Unit] = {
    val index = ZiRedisCons.ACCUMULATOR_DayUserCounter + request.id
    
    // Check if index exists and set initial value if necessary
    for {
      exists <- indexExist.get
      _ <- ZIO.when(!exists) {
        redis.get(index).flatMap {
          case Some(_) => ZIO.unit
          case None    => redis.set(index, "0")
        } *> indexExist.set(true)
      }
      _ <- redis.incr(index)
    } yield ()
  }

  def addUserAttempt(request: AddUserAttemptAccumulationWrapper): ZIO[Any, RedisError, Unit] = {
    val index = ZiRedisCons.ACCUMULATOR_DayUserAttemptCounter + request.id
    val uniqueUserIndex = ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptCounter + request.id
    val uniqueUserSet = ZiRedisCons.ACCUMULATOR_DayUniqueUserAttemptSet + request.id

    // Handle user attempt accumulation
    for {
      exists <- attemptIndexExist.get
      _ <- ZIO.when(!exists) {
        redis.get(index).flatMap {
          case Some(_) => ZIO.unit
          case None    => redis.set(index, "0")
        } *> attemptIndexExist.set(true)
      }
      _ <- redis.incr(index)

      // Handle unique user accumulation
      isMember <- redis.sismember(uniqueUserSet, request.accumulator.userid)
      _ <- ZIO.when(!isMember) {
        for {
          _ <- redis.sadd(uniqueUserSet, request.accumulator.userid)
          existsUnique <- uniqueUserIndexExist.get
          _ <- ZIO.when(!existsUnique) {
            redis.get(uniqueUserIndex).flatMap {
              case Some(_) => ZIO.unit
              case None    => redis.set(uniqueUserIndex, "0")
            } *> uniqueUserIndexExist.set(true)
          }
          _ <- redis.incr(uniqueUserIndex)
        } yield ()
      }
    } yield ()
  }

}

object DayAccumulators {

  def create: ZLayer[Redis, Nothing, DayAccumulators] = 
    ZLayer {
      for {
        redisService <- ZIO.service[Redis.Service]
        indexExist <- Ref.make(false)
        attemptIndexExist <- Ref.make(false)
        uniqueUserIndexExist <- Ref.make(false)
      } yield DayAccumulators(redisService, indexExist, attemptIndexExist, uniqueUserIndexExist)
    }

}
