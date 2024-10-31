package com.teqbahn.bootstrap

import zio._
import zio.http._
import zio.http.model.Method._
import zio.http.model.Status
import zio.actors._
import zio.actors.interruption._
import zio.stream._
import com.typesafe.config.{Config, ConfigFactory}
import java.io.File
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import com.teqbahn.actors.admin.AdminActor
import com.teqbahn.actors.analytics.Accumulators
import com.teqbahn.actors.analytics.accumulator._
import com.teqbahn.actors.analytics.result.ResultAccumulator
import com.teqbahn.actors.logs.LogsActor
import com.teqbahn.actors.mailer.MailActor

object StarterMain extends ZIOAppDefault {

  // Configuration case class
  case class AppConfig(
    envServer: String,
    confFile: String,
    akkaPort: Int,
    httpPort: Int,
    http2Port: Int,
    akkaManagementPort: Int,
    httpHostName: String,
    akkaManagementHostName: String,
    projectName: String,
    fileSystemType: String,
    fileSystemPath: String,
    projectPrefix: String,
    frontEndPath: String,
    redisHostPath: String,
    fromMail: String,
    fromMailPassword: String,
    salt: String
  )

  // Service for Redis Commands
  trait RedisService {
    def commands: RedisCommands[String, String]
  }

  object RedisService {
    def make(redisHostPath: String): Task[RedisService] = ZIO.attempt {
      val client: RedisClient = RedisClient.create(s"redis://$redisHostPath")
      val connection: StatefulRedisConnection[String, String] = client.connect()
      new RedisService {
        override val commands: RedisCommands[String, String] = connection.sync()
      }
    }
  }

  // Layer for AppConfig
  val appConfigLayer: ZLayer[Any, Throwable, AppConfig] = ZLayer {
    for {
      args <- ZIO.args
      envServer = if (args.nonEmpty) args(0) else "local"
      config <- if (envServer.equalsIgnoreCase("local")) {
                  for {
                    _ <- ZIO.when(args.length < 8)(ZIO.fail(new IllegalArgumentException("Not enough arguments for local configuration")))
                    conf = AppConfig(
                      envServer = "local",
                      confFile = "application_local.conf",
                      akkaPort = args(1).toInt,
                      httpPort = args(2).toInt,
                      http2Port = 8081,
                      akkaManagementPort = 8558,
                      httpHostName = args(3),
                      akkaManagementHostName = "127.0.0.1",
                      projectName = "tilli",
                      fileSystemType = "Redis",
                      fileSystemPath = args(7),
                      projectPrefix = "tilli-api",
                      frontEndPath = "https://teqbahn.com/tilli-web/",
                      redisHostPath = args(4),
                      fromMail = args(5),
                      fromMailPassword = args(6),
                      salt = "jMhKlOuJnM34G6NHkqo9V010GhLAqOpF0BePojHgh1HgNg8^72k"
                    )
                  } yield config
                } else {
                  for {
                    conf = AppConfig(
                      envServer = "live",
                      confFile = "application_live.conf",
                      akkaPort = System.getenv("akkaPort").toInt,
                      httpPort = System.getenv("httpPort").toInt,
                      http2Port = System.getenv("http2Port").toInt,
                      akkaManagementPort = System.getenv("akkaManagementPort").toInt,
                      httpHostName = System.getenv("httpHostName"),
                      akkaManagementHostName = System.getenv("akkaManagementHostName"),
                      projectName = System.getenv("projectName"),
                      fileSystemType = System.getenv("fileSystemType"),
                      fileSystemPath = System.getenv("fileSystemPath"),
                      projectPrefix = System.getenv("projectPrefix"),
                      frontEndPath = System.getenv("frontEndPath"),
                      redisHostPath = System.getenv("redisHostPath"),
                      fromMail = System.getenv("fromMail"),
                      fromMailPassword = System.getenv("fromMailPassword"),
                      salt = "jMhKlOuJnM34G6NHkqo9V010GhLAqOpF0BePojHgh1HgNg8^72k" // Ideally, fetch from env
                    )
                  } yield config
                }
    } yield config
  }

  // Layer for RedisService
  val redisLayer: ZLayer[AppConfig, Throwable, RedisService] = ZLayer.fromFunctionM { config =>
    RedisService.make(config.redisHostPath)
  }

  // Define ZIO Actors for your services
  // Example for AdminActor
  object AdminActor {
    sealed trait Command
    case object DoSomething extends Command

    def behavior: Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case DoSomething =>
          context.log.info("AdminActor is doing something.")
          Behaviors.same
      }
    }
  }

  // Similarly define other actors like MailActor, LogsActor, etc.
  // For brevity, only AdminActor is shown here

  // Service Layer to hold ActorRefs
  trait ActorService {
    val adminActor: ActorRef[AdminActor.Command]
    // Add other actor refs here
  }

  object ActorService {
    def make: ZLayer[Any, Nothing, ActorService] = ZLayer {
      for {
        system <- ZIO.environment[ActorSystem.Service]
        adminActor <- system.make(AdminActor.behavior, name = "supervised-admin")
        // Initialize other actors similarly
      } yield new ActorService {
        override val adminActor: ActorRef[AdminActor.Command] = adminActor
        // Assign other actor refs here
      }
    }
  }

  // Layer for ActorSystem
  val actorSystemLayer: ZLayer[Any, Nothing, ActorSystem.Service] = ZLayer.scoped {
    ActorSystemSupervisor.live("tilli")
  }

  // HTTP Routes using ZIO-HTTP
  def httpApp(actorService: ActorService, config: AppConfig): HttpApp[Any, Throwable] = {
    val routes = Http.collectZIO[Request] {
      case Method.GET -> !! / "hello" =>
        for {
          _ <- actorService.adminActor ! AdminActor.DoSomething
          resp <- ZIO.succeed(Response.text("Hello, World!"))
        } yield resp
      // Define other routes here
    }

    routes
  }

  // Main application logic
  val program: ZIO[ActorService with RedisService with AppConfig, Throwable, Unit] = for {
    config <- ZIO.service[AppConfig]
    actorService <- ZIO.service[ActorService]
    redisService <- ZIO.service[RedisService]
    _ <- printEnv(config)
    // Initialize Redis Commands if needed
    _ <- ZIO.succeed {
      // Example usage of Redis
      val redisValue = redisService.commands.get("some_key")
      println(s"Value from Redis: $redisValue")
    }
    // Start HTTP server
    _ <- Server.serve(httpApp(actorService, config)).provide(Server.defaultWithPort(config.httpPort))
  } yield ()

  // Helper method to print environment
  def printEnv(config: AppConfig): UIO[Unit] = UIO {
    println("... Inside tilli ...")
    println(
      s"""
         | projectName --> ${config.projectName}
         | confFile --> ${config.confFile}
         | envServer --> ${config.envServer}
         | fileSystemType --> ${config.fileSystemType}
         | fileSystemPath --> ${config.fileSystemPath}
         | projectPrefix --> ${config.projectPrefix}
         | akkaPort --> ${config.akkaPort}
         | httpPort --> ${config.httpPort}
         | akkaManagementPort --> ${config.akkaManagementPort}
         | httpHostName --> ${config.httpHostName}
         | akkaManagementHostName --> ${config.akkaManagementHostName}
         | frontEndPath --> ${config.frontEndPath}
         |""".stripMargin)
  }

  // Define the application layers
  override def run: URIO[Any with ZIOAppArgs with Scope, ExitCode] = {
    program
      .provide(
        appConfigLayer,
        actorSystemLayer,
        ActorService.make,
        redisLayer
      )
      .exitCode
  }
}
