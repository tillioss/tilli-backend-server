package com.teqbahn.bootstrap

import com.teqbahn.actors.admin.AdminActor
import com.teqbahn.actors.analytics.Accumulators
import com.teqbahn.actors.analytics.accumulator.time.{DayAccumulators, MonthAccumulators, YearAccumulators}
import com.teqbahn.actors.analytics.accumulator.{AgeAccumulators, FilterAccumulators, GenderAccumulators, LanguageAccumulators}
import com.teqbahn.actors.analytics.result.ResultAccumulator
import com.teqbahn.actors.logs.LogsActor
import com.teqbahn.actors.mailer.MailActor
import com.teqbahn.common.mail.Mailer
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.stream.ActorMaterializer

import java.io.File

object StarterMain {
  var envServer = "local" // "live"
  var confFile = "application_local.conf"

  var pekkoPort = 2551
  var httpPort = 8091
  var http2Port = 8081
  var pekkoManagementPort = 8558
  var httpHostName = "0.0.0.0" // "0.0.0.0"
  var pekkoManagementHostName = "127.0.0.1" // "127.0.0.1"

  // Project specific data

  var projectName = "tilli"
  var fileSystemType = "Redis" // All
  var fileSystemPath = ""
  var projectPrefix = "tilli-api"
  var frontEndPath = "https://teqbahn.com/tilli-web/"
  var redisHostPath = "127.0.0.1:6379"

  var fromMail = ""
  var fromMailPassword = ""
  var SALT = "jMhKlOuJnM34G6NHkqo9V010GhLAqOpF0BePojHgh1HgNg8^72k" //replace your encryption key

  var adminSupervisorActorRef: ActorRef = null
  var mailActorRef: ActorRef = null
  var logsActorRef: ActorRef = null

  var accumulatorsActorRef: ActorRef = null
  var accumulatorDayActorRef: ActorRef = null
  var accumulatorMonthActorRef: ActorRef = null
  var accumulatorYearActorRef: ActorRef = null
  var accumulatorAgeActorRef: ActorRef = null
  var accumulatorLanguageActorRef: ActorRef = null
  var accumulatorGenderActorRef: ActorRef = null
  var accumulatorFilterActorRef: ActorRef = null
  var accumulatorResultActorRef: ActorRef = null

  var redisCommands: RedisCommands[String, String] = null

  def main(args: Array[String]): Unit = {

    if (args.length > 0) {
      envServer = args(0)
    }

    if (envServer.equalsIgnoreCase("local")) {

      pekkoPort = args(1).toInt
      httpPort = args(2).toInt
      httpHostName = args(3)
      redisHostPath = args(4)
      fromMail = args(5)
      fromMailPassword = args(6)
      fileSystemPath = args(7)

      // fileSystemPath = "/efs/tilli/"
    } else {
      confFile = "application_live.conf"
      pekkoPort = System.getenv("pekkoPort").toInt
      httpPort = System.getenv("httpPort").toInt
      http2Port = System.getenv("http2Port").toInt
      pekkoManagementPort = System.getenv("pekkoManagementPort").toInt
      httpHostName = System.getenv("httpHostName")
      pekkoManagementHostName = System.getenv("pekkoManagementHostName")


      projectName = System.getenv("projectName")
      fileSystemType = System.getenv("fileSystemType")
      fileSystemPath = System.getenv("fileSystemPath")
      projectPrefix = System.getenv("projectPrefix")
      frontEndPath = System.getenv("frontEndPath")
      redisHostPath = System.getenv("redisHostPath")
      fromMail = System.getenv("fromMail")
      fromMailPassword = System.getenv("fromMailPassword")
    }

    printEnv()

    //    createDir(fileSystemPath + projectName)

    implicit val actorSystem = ActorSystem("tilli")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = actorSystem.dispatcher


    import io.lettuce.core.RedisClient
    val client: RedisClient = RedisClient.create("redis://" + redisHostPath)
    val connection: StatefulRedisConnection[String, String] = client.connect()
    redisCommands = connection.sync()


    adminSupervisorActorRef = actorSystem.actorOf(Props.create(classOf[AdminActor]), "supervised-admin")
    val mailer = new Mailer(fromMail, fromMailPassword);
    mailActorRef = actorSystem.actorOf(Props(new MailActor(mailer)), "actor-mail")
    logsActorRef = actorSystem.actorOf(Props.create(classOf[LogsActor]), "actor-logs")
    // Accumulator

    accumulatorsActorRef = actorSystem.actorOf(Props.create(classOf[Accumulators]), "actor-accumulator")
    accumulatorDayActorRef = actorSystem.actorOf(Props.create(classOf[DayAccumulators]), "actor-accumulator-day")
    accumulatorMonthActorRef = actorSystem.actorOf(Props.create(classOf[MonthAccumulators]), "actor-accumulator-month")
    accumulatorYearActorRef = actorSystem.actorOf(Props.create(classOf[YearAccumulators]), "actor-accumulator-year")
    accumulatorAgeActorRef = actorSystem.actorOf(Props.create(classOf[AgeAccumulators]), "actor-accumulator-age")
    accumulatorLanguageActorRef = actorSystem.actorOf(Props.create(classOf[LanguageAccumulators]), "actor-accumulator-language")
    accumulatorGenderActorRef = actorSystem.actorOf(Props.create(classOf[GenderAccumulators]), "actor-accumulator-gender")
    accumulatorFilterActorRef = actorSystem.actorOf(Props.create(classOf[FilterAccumulators]), "actor-accumulator-filter")
    accumulatorResultActorRef = actorSystem.actorOf(Props.create(classOf[ResultAccumulator]), "actor-accumulator-result")


    Http().bindAndHandle(PekkoHttpConnector.getRoutes(materializer, actorSystem, projectPrefix, pekkoManagementHostName), httpHostName, httpPort)
    println(s"Server online at http://" + httpHostName + ":" + httpPort + "/\nPress RETURN to stop...")

  }

  def printEnv(): Unit = {
    println("... Inside tilli ...")
    println(
      "\n projectName --> " + projectName +
        "\n confFile --> " + confFile +
        "\n envServer --> " + envServer +
        "\n fileSystemType --> " + fileSystemType +
        "\n fileSystemPath --> " + fileSystemPath +
        "\n projectPrefix --> " + projectPrefix +
        "\n pekkoPort --> " + pekkoPort +
        "\n httpPort --> " + httpPort +
        "\n pekkoManagementPort --> " + pekkoManagementPort +
        "\n httpHostName --> " + httpHostName +
        "\n pekkoManagementHostName --> " + pekkoManagementHostName +
        "\n frontEndPath --> " + frontEndPath
    )
  }

  def createDir(folderPath: String): Unit = {
    val existDir = new File(folderPath)
    if (!existDir.exists) existDir.mkdirs()
  }


  def getImages(subDir: String, fileName: String): File = {
    val path1 = StarterMain.fileSystemPath + "/" + subDir + "/" + fileName
    val file = new File(path1)
    return file
  }

  def deleteFilePath(path: String) = {
    val fileTemp = new File(path)
    if (fileTemp.exists) {
      fileTemp.delete()
    }
  }

  def fileExistCheck(text: String): Boolean = {
    var bool = false
    val fileTemp = new File(text)
    if (fileTemp.exists) {
      bool = true
    }
    bool
  }

}


class StarterMain {

}
