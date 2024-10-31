import zio._
import zio.redis._
import com.teqbahn.caseclasses.AddToAccumulationWrapper
import com.teqbahn.global.ZiRedisCons
import zio.json._
import zio.redis.api._

case class GenderAccumulators(redis: Redis.Service) {

  def handleRequest(request: AddToAccumulationWrapper): ZIO[Any, RedisError, Unit] = {
    val index = ZiRedisCons.ACCUMULATOR_GenderUserCounter + request.id

    // Check if index exists, if not set it to 0, then increment
    for {
      exists <- redis.get(index)
      _ <- exists match {
        case Some(_) => ZIO.unit // Key exists, do nothing
        case None => redis.set(index, "0") // Set the key if it doesn't exist
      }
      _ <- redis.incr(index)
    } yield ()
  }

  // Timeout handler could be represented by some scheduled ZIO operation or cleanup mechanism
  def handleTimeout: ZIO[Any, Nothing, Unit] = ZIO.succeed(println("Timeout occurred, stopping the actor"))

}

object GenderAccumulators {

  def create: ZLayer[Redis, Nothing, GenderAccumulators] = ZLayer.fromFunction { redis =>
    GenderAccumulators(redis.get)
  }

  // Example usage in a ZIO runtime
  def runProgram(request: AddToAccumulationWrapper): ZIO[GenderAccumulators, RedisError, Unit] = for {
    genderAccumulators <- ZIO.service[GenderAccumulators]
    _ <- genderAccumulators.handleRequest(request)
  } yield ()
}
