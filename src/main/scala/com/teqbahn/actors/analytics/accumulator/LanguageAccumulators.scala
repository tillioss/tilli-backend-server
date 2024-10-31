import zio._
import zio.json._
import zio.redis._

import com.teqbahn.caseclasses.AddToAccumulationWrapper
import com.teqbahn.global.ZiRedisCons

object LanguageAccumulators {
  trait Service {
    def addToAccumulation(request: AddToAccumulationWrapper): Task[Unit]
  }

  case class State(indexExist: Boolean = false)

  class Live(redis: Redis, state: Ref[State]) extends Service {
    override def addToAccumulation(request: AddToAccumulationWrapper): Task[Unit] = {
      val index = ZiRedisCons.ACCUMULATOR_LanguageUserCounter + request.id
      for {
        s <- state.get
        _ <- if (!s.indexExist) initializeCounter(index) else ZIO.unit
        _ <- state.update(_.copy(indexExist = true))
        _ <- redis.incr(index)
      } yield ()
    }

    private def initializeCounter(index: String): Task[Unit] = {
      redis.get(index).flatMap {
        case Some(value) if value.nonEmpty && value != "null" => ZIO.unit
        case _ => redis.set(index, "0")
      }
    }
  }

  val live: ZLayer[Redis, Nothing, LanguageAccumulators.Service] = ZLayer.fromZIO(
    for {
      redis <- ZIO.service[Redis]
      state <- Ref.make(State())
    } yield new Live(redis, state)
  )

  def addToAccumulation(request: AddToAccumulationWrapper): ZIO[LanguageAccumulators.Service, Throwable, Unit] =
    ZIO.serviceWithZIO[Service](_.addToAccumulation(request))
}
