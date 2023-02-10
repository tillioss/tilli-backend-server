package com.teqbahn.bootstrap

import java.io.{File,FileInputStream, FileOutputStream,IOException}
import java.util.concurrent.TimeUnit
import akka.actor.{ActorRef, ActorSystem}
import akka.http.javadsl.model.BodyPartEntity
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.fasterxml.jackson.databind.ObjectMapper
import com.teqbahn.bootstrap.StarterMain.envServer
import com.teqbahn.caseclasses._
import com.teqbahn.converter.CompressImageFiles
import com.teqbahn.global.GlobalMessageConstants
import com.teqbahn.utils.ZiFunctions
import scalaj.http.{Http, HttpOptions}
import org.json4s.DefaultFormats
import scala.concurrent.duration._
import org.json4s.native.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import akka.pattern.Patterns
import scala.concurrent.Await
import java.util.zip.{ZipEntry, ZipInputStream}


object AkkaHttpConnector {

  var timeout = new Timeout(100, TimeUnit.SECONDS)

  def getRoutes(materializer1: ActorMaterializer, actorSystem: ActorSystem, projectPrefix: String, nodeIp: String): Route = {

    implicit val materializer = materializer1
    implicit val executionContext = actorSystem.dispatcher

    val rejectionHandler = corsRejectionHandler.withFallback(RejectionHandler.default)

    val exceptionHandler = ExceptionHandler {
      case e: NoSuchElementException => complete(StatusCodes.NotFound -> e.getMessage)
    }

    val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)
    return cors() {
      handleErrors {
        concat(
          get {
            path(projectPrefix / "myIP") {
              extractClientIP { ip =>
                var objectMapper = new ObjectMapper()
                var objNode = objectMapper.createObjectNode();
                objNode.put("ip", ip.toOption.map(_.getHostAddress).getOrElse("unknown").toString)
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, objNode.toString))
              }
            }
          },
          get {
            path(projectPrefix / "home") {
              get {
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>  Tilli started </h1>"))
              }
            }
          },
          get {
            path(projectPrefix / "apk") {
              parameterMap { params =>
                val version = params.get("version").get
                val f: File = StarterMain.getImages("apk", version + ".apk")
                getFromFile(f)
              }
            }
          },
          get {
            path(projectPrefix / "vp") {
              parameterMap { params =>
                val action = params.get("action").get
                val id = params.get("id").get
                val key = params.get("key").get
                val f: File = StarterMain.getImages(action + "/" + id, key)
                getFromFile(f)
              }
            }
          },
           get  {
            path(projectPrefix / "vp-game-file" / Segment / Segment / Segment/ Segment) { (action, id,key,fileName) =>
             val firstSubPath=action + "/" + id+"/"+key;
             val f: File = StarterMain.getImages(firstSubPath,fileName);
             getFromFile(f)  
            }
          },
          get {
            path(projectPrefix / Segment/"datacollection.xls") {
               { (userId) =>
                var file: File = null
                val desFilePath = StarterMain.fileSystemPath + "/excel/" + userId+"/"+userId+".xls"
                file = new File(desFilePath)
                getFromFile(file)
              }
            }
          },
          post {
            path(projectPrefix / "uploads" / Segment / Segment / Segment) { (dir1, dir2, fileName) =>
              toStrictEntity(10.seconds) {
                (post & entity(as[Multipart.FormData])) { fileData => {
                  val start = System.currentTimeMillis
                  fileData.parts.mapAsync(1) {
                    bodyPart ⇒
                      def writeFileOnLocal(array: Array[Byte], byteString: ByteString): Array[Byte] = {
                        var dispositionMap: Map[String, String] = bodyPart.additionalDispositionParams
                        if (dispositionMap.size > 0 && bodyPart.getName().contains("file") && !dispositionMap.get("filename").isEmpty) {
                          var temp = System.getProperty("java.io.tmpdir")
                          var pb: BodyPartEntity = bodyPart.getEntity()

                          temp = StarterMain.fileSystemPath 
                          temp = temp + "/" + dir1
                          StarterMain.createDir(temp)
                          temp += "/" + dir2
                          StarterMain.createDir(temp)


                          /*  val fileName = dispositionMap.get("filename");
                            var fileExt = fileName.get.toString.split("\\.").last
                            val uploadedFileName = docsId + "." + fileExt*/

                          val awsPath = dir1 + "/" + dir2

                          val filePath = temp + "/" + fileName

                          val intermediateDir = StarterMain.fileSystemPath  + "/intermediates"
                          StarterMain.createDir(intermediateDir)
                          val srcConversionPath = intermediateDir + "/" + fileName
                          val fileOutput = new FileOutputStream(srcConversionPath)
                          val byteArray: Array[Byte] = byteString.toArray
                          fileOutput.write(byteArray)

                          CompressImageFiles.doImageCompression(srcConversionPath, filePath)

                          val fileDelete = new File(srcConversionPath)
                          if (fileDelete.exists()) {
                            fileDelete.delete()
                          }


                          array ++ byteArray
                        }
                        array
                      }

                      bodyPart.entity.dataBytes.runFold(Array[Byte]())(writeFileOnLocal)
                  }.runFold(0)(_ + _.length)
                  val finish = System.currentTimeMillis
                  println(System.currentTimeMillis + "- TimeElapsed on HTTP  - uploads : " + (finish - start) + ", For Key : " + dir1 + "/" + fileName + "/" + dir2)
                  complete(GlobalMessageConstants.SUCCESS)
                }
                }
              }
            }
          },
          post {
            path(projectPrefix / "uploads-game-file" / Segment / Segment / Segment) { (dir1, dir2,fileName) =>
             withSizeLimit(15000000) {
              toStrictEntity(10.seconds) {
                (post & entity(as[Multipart.FormData])) { fileData => {
                    val start = System.currentTimeMillis
                    fileData.parts.mapAsync(1) {
                      bodyPart ⇒
                        def writeFileOnLocal(array: Array[Byte], byteString: ByteString): Array[Byte] = {
                          var dispositionMap: Map[String, String] = bodyPart.additionalDispositionParams
                          if (dispositionMap.size > 0 && bodyPart.getName().contains("file") && !dispositionMap.get("filename").isEmpty) {
                            var temp = System.getProperty("java.io.tmpdir")
                            temp = StarterMain.fileSystemPath 
                            temp = temp + "/" + dir1
                            StarterMain.createDir(temp)
                            temp += "/zip/" + dir2
                            StarterMain.createDir(temp)

                            val awsPath = dir1 + "/" + dir2
                            val intermediateDir = StarterMain.fileSystemPath  + "/intermediates"
                            StarterMain.createDir(intermediateDir)
                            val srcConversionPath = intermediateDir + "/" + fileName
                            val fileOutput = new FileOutputStream(srcConversionPath)
                            val byteArray: Array[Byte] = byteString.toArray
                            fileOutput.write(byteArray)

                            val outputFolder: String = intermediateDir + "/"
                            val buffer = new Array[Byte](1024)

                            try {
                              //output directory
                              val folder = new File(outputFolder);
                              if (!folder.exists()) {
                                folder.mkdir();
                              }

                              //zip file content
                              val zis: ZipInputStream = new ZipInputStream(new FileInputStream(srcConversionPath));
                              //get the zipped file list entry
                              var ze: ZipEntry = zis.getNextEntry();

                              while (ze != null) {
                                val zipFileName = ze.getName();
                                if(zipFileName.contains("index.js") || zipFileName.contains("index.wasm") || zipFileName.contains("index.pck")) {
                                  val filePath = temp + "/" +zipFileName
                                  val newFile = new File(filePath)

                                  //create folders
                                  new File(newFile.getParent()).mkdirs();
                                  val fos = new FileOutputStream(newFile);
                                  var len: Int = zis.read(buffer);
                                  while (len > 0) {
                                    fos.write(buffer, 0, len)
                                    len = zis.read(buffer)
                                  }
                                  fos.close()
                                }
                                ze = zis.getNextEntry()
                              }

                              zis.closeEntry()
                              zis.close()

                            } catch {
                              case e: IOException => println("exception caught: " + e.getMessage)
                            }

                            val fileDelete = new File(srcConversionPath)
                            if (fileDelete.exists()) {
                              fileDelete.delete()
                            }
                            array ++ byteArray
                          }
                          array
                        }
                        bodyPart.entity.dataBytes.runFold(Array[Byte]())(writeFileOnLocal)
                    }.runFold(0)(_ + _.length)
                    val finish = System.currentTimeMillis
                    println(System.currentTimeMillis + "- TimeElapsed on HTTP  - uploads : " + (finish - start) + ", For Key : " + dir1 + "/" + fileName + "/" + dir2)
                    complete(GlobalMessageConstants.SUCCESS)
                  }
                }
              }
             }
            }
          },
          path(projectPrefix / "adminLogin") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getAdminLoginRequest = jValue.extract[GetAdminLoginRequest]
                var getAdminLoginResponse: GetAdminLoginResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getAdminLoginRequest, timeout)
                try {
                  getAdminLoginResponse = Await.result(future, timeout.duration).asInstanceOf[GetAdminLoginResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(getAdminLoginResponse))
              }
            }
          },
          path(projectPrefix / "adminList") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getAdminListRequest = jValue.extract[GetAdminListRequest]
                var getAdminListResponse: GetAdminListResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getAdminListRequest, timeout)
                try {
                  getAdminListResponse = Await.result(future, timeout.duration).asInstanceOf[GetAdminListResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(getAdminListResponse))
              }
            }
          },
          path(projectPrefix / "fetchAnalytics") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val fetchAnalyticsRequest = jValue.extract[FetchAnalyticsRequest]
                var response: FetchAnalyticsResponse = null
                val future = Patterns.ask(getResultAccumulatorActorRef(actorSystem), fetchAnalyticsRequest, timeout)
                try {
                  val fetchAnalyticsResponse = Await.result(future, timeout.duration).asInstanceOf[FetchAnalyticsResponse]
                  response = fetchAnalyticsResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "fetchFilterAnalytics") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val fetchFilterAnalyticsRequest = jValue.extract[FetchFilterAnalyticsRequest]
                var response: FetchFilterAnalyticsResponse = null
                val future = Patterns.ask(getResultAccumulatorActorRef(actorSystem), fetchFilterAnalyticsRequest, timeout)
                try {
                  val fetchFilterAnalyticsResponse = Await.result(future, timeout.duration).asInstanceOf[FetchFilterAnalyticsResponse]
                  response = fetchFilterAnalyticsResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "fetchFilterUserAttemptAnalytics") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val fetchFilterUserAttemptAnalyticsRequest = jValue.extract[FetchFilterUserAttemptAnalyticsRequest]
                var response: FetchFilterUserAttemptAnalyticsResponse = null
                val future = Patterns.ask(getResultAccumulatorActorRef(actorSystem), fetchFilterUserAttemptAnalyticsRequest, timeout)
                try {
                  val fetchFilterUserAttemptAnalyticsResponse = Await.result(future, timeout.duration).asInstanceOf[FetchFilterUserAttemptAnalyticsResponse]
                  response = fetchFilterUserAttemptAnalyticsResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "createUser") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val createUserRequest = jValue.extract[CreateUserRequest]
                var response: CreateUserResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, createUserRequest, timeout)
                try {
                  val createUserResponse = Await.result(future, timeout.duration).asInstanceOf[CreateUserResponse]
                  response = createUserResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
           path(projectPrefix / "createGameUser") {
             post {
              entity(as[String]) { data =>
             val start  = System.currentTimeMillis
             implicit val formats = DefaultFormats
             val jValue = parse(data)
             val createGameUserRequest = jValue.extract[CreateGameUserRequest]
             var response: CreateGameUserResponse = null
             val future = Patterns.ask(StarterMain.adminSupervisorActorRef, createGameUserRequest, timeout)
             try {
                  val createGameUserResponse = Await.result(future, timeout.duration).asInstanceOf[CreateGameUserResponse]
                  response = createGameUserResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
              complete(write(response))
              }
            }
            },
          path(projectPrefix / "createDemoUser") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val createDemoUserRequest = jValue.extract[CreateDemoUserRequest]
                var response: CreateDemoUserResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, createDemoUserRequest, timeout)
                try {
                  val createDemoUserResponse = Await.result(future, timeout.duration).asInstanceOf[CreateDemoUserResponse]
                  response = createDemoUserResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "createDemo2User") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val createDemo2UserRequest = jValue.extract[CreateDemo2UserRequest]
                var response: CreateDemo2UserResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, createDemo2UserRequest, timeout)
                try {
                  val createDemo2UserResponse = Await.result(future, timeout.duration).asInstanceOf[CreateDemo2UserResponse]
                  response = createDemo2UserResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateUserDetails") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateUserDetailsRequest = jValue.extract[UpdateUserDetailsRequest]
                var response: UpdateUserDetailsResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateUserDetailsRequest, timeout)
                try {
                  val updateUserDetailsResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateUserDetailsResponse]
                  response = updateUserDetailsResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "checkEmailIdAlreadyExist") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val checkEmailIdAlreadyExistRequest = jValue.extract[CheckEmailIdAlreadyExistRequest]
                var response: CheckEmailIdAlreadyExistResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, checkEmailIdAlreadyExistRequest, timeout)
                try {
                  val checkEmailIdAlreadyExistResponse = Await.result(future, timeout.duration).asInstanceOf[CheckEmailIdAlreadyExistResponse]
                  response = checkEmailIdAlreadyExistResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "login") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLoginRequest = jValue.extract[GetLoginRequest]
                var response: GetLoginResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getLoginRequest, timeout)
                try {
                  val userAccountResponse = Await.result(future, timeout.duration).asInstanceOf[GetLoginResponse]
                  response = userAccountResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "logout") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLogoutRequest = jValue.extract[GetLogoutRequest]

                StarterMain.adminSupervisorActorRef ! getLogoutRequest
                complete(write(GetLogoutResponse("Success")))
              }
            }
          },
          path(projectPrefix / "sendForgotPassword") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val sendForgotPasswordRequest = jValue.extract[SendForgotPasswordRequest]
                var response: SendForgotPasswordResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, sendForgotPasswordRequest, timeout)
                try {
                  response = Await.result(future, timeout.duration).asInstanceOf[SendForgotPasswordResponse]

                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateForgotPassword") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateForgotPasswordRequest = jValue.extract[UpdateForgotPasswordRequest]
                var response: UpdateForgotPasswordResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateForgotPasswordRequest, timeout)
                try {
                  response = Await.result(future, timeout.duration).asInstanceOf[UpdateForgotPasswordResponse]

                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getAllUserList") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getAllUserListRequest = jValue.extract[GetAllUserListRequest]
                var response: GetAllUserListResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getAllUserListRequest, timeout)
                try {
                  val getAllUserListResponse = Await.result(future, timeout.duration).asInstanceOf[GetAllUserListResponse]
                  response = getAllUserListResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "addGameLevel") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val addGameLevelRequest = jValue.extract[AddGameLevelRequest]
                var response: AddGameLevelResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, addGameLevelRequest, timeout)
                try {
                  val addGameLevelResponse = Await.result(future, timeout.duration).asInstanceOf[AddGameLevelResponse]
                  response = addGameLevelResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateGameLevel") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateGameLevelRequest = jValue.extract[UpdateGameLevelRequest]
                var response: UpdateGameLevelResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateGameLevelRequest, timeout)
                try {
                  val updateGameLevelResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateGameLevelResponse]
                  response = updateGameLevelResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getGameLevels") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getGameLevelsRequest = jValue.extract[GetGameLevelsRequest]
                var response: GetGameLevelsResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getGameLevelsRequest, timeout)
                try {
                  val getGameLevelsResponse = Await.result(future, timeout.duration).asInstanceOf[GetGameLevelsResponse]
                  response = getGameLevelsResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "deleteGameLevels") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val deleteGameLevelsRequest = jValue.extract[DeleteGameLevelsRequest]
                StarterMain.adminSupervisorActorRef ! deleteGameLevelsRequest
                complete(write(DeleteGameLevelResponse("Success")))
              }
            }
          },
          path(projectPrefix / "deleteThemes") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val deleteThemesRequest = jValue.extract[DeleteThemesRequest]
                StarterMain.adminSupervisorActorRef ! deleteThemesRequest
                complete(write(DeleteThemesResponse("Success")))
              }
            }
          },
          path(projectPrefix / "addTheme") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val addThemeRequest = jValue.extract[AddThemeRequest]
                var response: AddThemeResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, addThemeRequest, timeout)
                try {
                  val addThemeResponse = Await.result(future, timeout.duration).asInstanceOf[AddThemeResponse]
                  response = addThemeResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateTheme") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateThemeRequest = jValue.extract[UpdateThemeRequest]
                var response: UpdateThemeResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateThemeRequest, timeout)
                try {
                  val updateThemeResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateThemeResponse]
                  response = updateThemeResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getThemes") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getThemesRequest = jValue.extract[GetThemesRequest]
                var response: GetThemesResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getThemesRequest, timeout)
                try {
                  val getThemesResponse = Await.result(future, timeout.duration).asInstanceOf[GetThemesResponse]
                  response = getThemesResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateThemeContent") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateThemeContentRequest = jValue.extract[UpdateThemeContentRequest]
                var response = "{}"
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateThemeContentRequest, timeout)
                try {
                  val updateThemeContentResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateThemeContentResponse]
                  response = write(updateThemeContentResponse)
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getThemeContent") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getThemeContentRequest = jValue.extract[GetThemeContentRequest]
                var response = "{}"
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getThemeContentRequest, timeout)
                try {
                  val getThemeContentResponse = Await.result(future, timeout.duration).asInstanceOf[GetThemeContentResponse]
                  response = write(getThemeContentResponse)
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "addGameFile") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val addGameFileRequest = jValue.extract[AddGameFileRequest]
                var response: AddGameFileResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, addGameFileRequest, timeout)
                try {
                  val addGameFileResponse = Await.result(future, timeout.duration).asInstanceOf[AddGameFileResponse]
                  response = addGameFileResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateGameFile") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateGameFileRequest = jValue.extract[UpdateGameFileRequest]
                var response: UpdateGameFileResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateGameFileRequest, timeout)
                try {
                  val updateGameFileResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateGameFileResponse]
                  response = updateGameFileResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "deleteGameFile") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val deleteGameFileSearchListRequest = jValue.extract[DeleteGameFileSearchListRequest]
                var response: DeleteGameFileSearchListResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, deleteGameFileSearchListRequest, timeout)
                try {
                  val deleteGameFileSearchListResponse = Await.result(future, timeout.duration).asInstanceOf[DeleteGameFileSearchListResponse]
                  response = deleteGameFileSearchListResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getGameFilesList") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getGameFilesListRequest = jValue.extract[GetGameFilesListRequest]
                var response: GetGameFilesListResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getGameFilesListRequest, timeout)
                try {
                  val getGameFilesListResponse = Await.result(future, timeout.duration).asInstanceOf[GetGameFilesListResponse]
                  response = getGameFilesListResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getGameFileSearchList") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getGameFileSearchListRequest = jValue.extract[GetGameFileSearchListRequest]
                var response: GetGameFileSearchListResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getGameFileSearchListRequest, timeout)
                try {
                  val getGameFileSearchListResponse = Await.result(future, timeout.duration).asInstanceOf[GetGameFileSearchListResponse]
                  response = getGameFileSearchListResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateLevelMapping") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateLevelMappingRequest = jValue.extract[UpdateLevelMappingRequest]
                var response: UpdateLevelMappingResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateLevelMappingRequest, timeout)
                try {
                  val updateLevelMappingResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateLevelMappingResponse]
                  response = updateLevelMappingResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getLevelMappingData") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLevelMappingDataRequest = jValue.extract[GetLevelMappingDataRequest]
                var response: GetLevelMappingDataResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getLevelMappingDataRequest, timeout)
                try {
                  val getLevelMappingDataResponse = Await.result(future, timeout.duration).asInstanceOf[GetLevelMappingDataResponse]
                  response = getLevelMappingDataResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateUserGameStatus") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateUserGameStatusRequest = jValue.extract[UpdateUserGameStatusRequest]
                var response: UpdateUserGameStatusResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateUserGameStatusRequest, timeout)
                try {
                  val updateUserGameStatusResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateUserGameStatusResponse]
                  response = updateUserGameStatusResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getUserGameStatus") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getUserGameStatusRequest = jValue.extract[GetUserGameStatusRequest]
                var response: GetUserGameStatusResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getUserGameStatusRequest, timeout)
                try {
                  val getUserGameStatusResponse = Await.result(future, timeout.duration).asInstanceOf[GetUserGameStatusResponse]
                  response = getUserGameStatusResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateStatusBasedOnStory") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateStatusBasedOnStoryRequest = jValue.extract[UpdateStatusBasedOnStoryRequest]
                var response: UpdateStatusBasedOnStoryResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateStatusBasedOnStoryRequest, timeout)
                try {
                  response = Await.result(future, timeout.duration).asInstanceOf[UpdateStatusBasedOnStoryResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getStoryBasedStatus") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getStoryBasedStatusRequest = jValue.extract[GetStoryBasedStatusRequest]
                var response: GetStoryBasedStatusResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getStoryBasedStatusRequest, timeout)
                try {
                  response = Await.result(future, timeout.duration).asInstanceOf[GetStoryBasedStatusResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getLevelAttemptCount") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLevelAttemptCountRequest = jValue.extract[GetLevelAttemptCountRequest]
                var response: GetLevelAttemptCountResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getLevelAttemptCountRequest, timeout)
                try {
                  response = Await.result(future, timeout.duration).asInstanceOf[GetLevelAttemptCountResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateLevelAttempt") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateLevelAttemptRequest = jValue.extract[UpdateLevelAttemptRequest]
                var response: UpdateLevelAttemptResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateLevelAttemptRequest, timeout)
                try {
                  val updateLevelAttemptResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateLevelAttemptResponse]
                  response = updateLevelAttemptResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getGameDateWiseReport") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getGameDateWiseReportRequest = jValue.extract[GetGameDateWiseReportRequest]
                var response: GetGameDateWiseResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getGameDateWiseReportRequest, timeout)
                try {
                  val getGameDateWiseResponse = Await.result(future, timeout.duration).asInstanceOf[GetGameDateWiseResponse]
                  response = getGameDateWiseResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "gameCsvFileGenrate") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val gameCsvFileGenrateRequest = jValue.extract[GameCsvFileGenrateRequest]
                var response: GameCsvFileGenrateResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, gameCsvFileGenrateRequest, timeout)
                try {
                  val gameCsvFileGenrateResponse = Await.result(future, timeout.duration).asInstanceOf[GameCsvFileGenrateResponse]
                  response = gameCsvFileGenrateResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "gameCsvFileStatus") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val gameFileStatusRequest = jValue.extract[GameFileStatusRequest]
                var response: GameFileStatusResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, gameFileStatusRequest, timeout)
                try {
                  val gameFileStatusResponse = Await.result(future, timeout.duration).asInstanceOf[GameFileStatusResponse]
                  response = gameFileStatusResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getLevelAttempts") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLevelAttemptsRequest = jValue.extract[GetLevelAttemptsRequest]
                var response: GetLevelAttemptsRequestResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getLevelAttemptsRequest, timeout)
                try {
                  val getLevelAttemptsRequestResponse = Await.result(future, timeout.duration).asInstanceOf[GetLevelAttemptsRequestResponse]
                  response = getLevelAttemptsRequestResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getAllUserAttemptList") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getAllUserAttemptListRequest = jValue.extract[GetAllUserAttemptListRequest]
                var response: GetAllUserAttemptListResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getAllUserAttemptListRequest, timeout)
                try {
                  val getAllUserAttemptListResponse = Await.result(future, timeout.duration).asInstanceOf[GetAllUserAttemptListResponse]
                  response = getAllUserAttemptListResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getLevelAttemptsJsonDetails") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLevelAttemptsJsonDetailsRequest = jValue.extract[GetLevelAttemptsJsonDetailsRequest]
                var response: GetLevelAttemptsJsonDetailsResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getLevelAttemptsJsonDetailsRequest, timeout)
                try {
                  val getLevelAttemptsJsonDetailsResponse = Await.result(future, timeout.duration).asInstanceOf[GetLevelAttemptsJsonDetailsResponse]
                  response = getLevelAttemptsJsonDetailsResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "addLanguage") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val addLanguageRequest = jValue.extract[AddLanguageRequest]
                var response: AddLanguageResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, addLanguageRequest, timeout)
                try {
                  val addLanguageResponse = Await.result(future, timeout.duration).asInstanceOf[AddLanguageResponse]
                  response = addLanguageResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateLanguage") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateLanguageRequest = jValue.extract[UpdateLanguageRequest]
                var response: UpdateLanguageResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateLanguageRequest, timeout)
                try {
                  val updateLanguageResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateLanguageResponse]
                  response = updateLanguageResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getLanguages") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLanguagesRequest = jValue.extract[GetLanguagesRequest]
                var response: GetLanguagesResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getLanguagesRequest, timeout)
                try {
                  val getLanguagesResponse = Await.result(future, timeout.duration).asInstanceOf[GetLanguagesResponse]
                  response = getLanguagesResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateLanguageBaseData") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateLanguageBaseDataRequest = jValue.extract[UpdateLanguageBaseDataRequest]
                var response: UpdateLanguageBaseDataResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateLanguageBaseDataRequest, timeout)
                try {
                  val updateLanguageBaseDataResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateLanguageBaseDataResponse]
                  response = updateLanguageBaseDataResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getLanguageBaseData") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLanguageBaseDataRequest = jValue.extract[GetLanguageBaseDataRequest]
                var response: GetLanguageBaseDataResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getLanguageBaseDataRequest, timeout)
                try {
                  val getLanguageBaseDataResponse = Await.result(future, timeout.duration).asInstanceOf[GetLanguageBaseDataResponse]
                  response = getLanguageBaseDataResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateLanguageMappingData") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateLanguageMappingDataRequest = jValue.extract[UpdateLanguageMappingDataRequest]
                var response: UpdateLanguageMappingDataResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateLanguageMappingDataRequest, timeout)
                try {
                  val updateLanguageMappingDataResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateLanguageMappingDataResponse]
                  response = updateLanguageMappingDataResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getLanguageMappingData") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLanguageMappingDataRequest = jValue.extract[GetLanguageMappingDataRequest]
                var response: GetLanguageMappingDataResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getLanguageMappingDataRequest, timeout)
                try {
                  val getLanguageMappingDataResponse = Await.result(future, timeout.duration).asInstanceOf[GetLanguageMappingDataResponse]
                  response = getLanguageMappingDataResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getLanguageMappingDataWithBaseData") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLanguageMappingDataWithBaseDataRequest = jValue.extract[GetLanguageMappingDataWithBaseDataRequest]
                var response: GetLanguageMappingDataWithBaseDataResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getLanguageMappingDataWithBaseDataRequest, timeout)
                try {
                  val getLanguageMappingDataWithBaseDataResponse = Await.result(future, timeout.duration).asInstanceOf[GetLanguageMappingDataWithBaseDataResponse]
                  response = getLanguageMappingDataWithBaseDataResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateModuleLanguageMapping") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateModuleLanguageMappingRequest = jValue.extract[UpdateModuleLanguageMappingRequest]
                var response: UpdateModuleLanguageMappingResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateModuleLanguageMappingRequest, timeout)
                try {
                  val updateModuleLanguageMappingResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateModuleLanguageMappingResponse]
                  response = updateModuleLanguageMappingResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getModuleLanguageMapping") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getModuleLanguageMappingRequest = jValue.extract[GetModuleLanguageMappingRequest]
                var response: GetModuleLanguageMappingResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getModuleLanguageMappingRequest, timeout)
                try {
                  val getModuleLanguageMappingResponse = Await.result(future, timeout.duration).asInstanceOf[GetModuleLanguageMappingResponse]
                  response = getModuleLanguageMappingResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "updateLevelsNameLanguageMapping") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val updateLevelsNameLanguageMappingRequest = jValue.extract[UpdateLevelsNameLanguageMappingRequest]
                var response: UpdateLevelsNameLanguageMappingResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, updateLevelsNameLanguageMappingRequest, timeout)
                try {
                  val updateLevelsNameLanguageMappingResponse = Await.result(future, timeout.duration).asInstanceOf[UpdateLevelsNameLanguageMappingResponse]
                  response = updateLevelsNameLanguageMappingResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getLevelsNameLanguageMapping") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getLevelsNameLanguageMappingRequest = jValue.extract[GetLevelsNameLanguageMappingRequest]
                var response: GetLevelsNameLanguageMappingResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getLevelsNameLanguageMappingRequest, timeout)
                try {
                  val getLevelsNameLanguageMappingResponse = Await.result(future, timeout.duration).asInstanceOf[GetLevelsNameLanguageMappingResponse]
                  response = getLevelsNameLanguageMappingResponse
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "captureLogs") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val captureLogsRequest = jValue.extract[CaptureLogsRequest]
                getLogsActorRef(actorSystem) ! CaptureLogsRequestWrapper(ZiFunctions.getId(), captureLogsRequest)
                complete(write(CaptureLogsResponse(GlobalMessageConstants.SUCCESS)))
              }
            }
          },
          path(projectPrefix / "getWebLogs") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getWebLogsRequest = jValue.extract[GetWebLogsRequest]
                var response = "{}"
                val future = Patterns.ask(getLogsActorRef(actorSystem), getWebLogsRequest, timeout)
                try {
                  val getWebLogsResponse = Await.result(future, timeout.duration).asInstanceOf[GetWebLogsResponse]
                  response = write(getWebLogsResponse)
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "getWebLogs") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getWebLogsRequest = jValue.extract[GetWebLogsRequest]
                var response = "{}"
                val future = Patterns.ask(getLogsActorRef(actorSystem), getWebLogsRequest, timeout)
                try {
                  val getWebLogsResponse = Await.result(future, timeout.duration).asInstanceOf[GetWebLogsResponse]
                  response = write(getWebLogsResponse)
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(response))
              }
            }
          },
          path(projectPrefix / "createRole") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val createRoleRequest = jValue.extract[CreateRoleRequest]
                var createRoleResponse: CreateRoleResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, createRoleRequest, timeout)
                try {
                  createRoleResponse = Await.result(future, timeout.duration).asInstanceOf[CreateRoleResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(createRoleResponse))
              }
            }
          },
          path(projectPrefix / "getRoles") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getRolesRequest = jValue.extract[GetRolesRequest]
                var getRolesResponse: GetRolesResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getRolesRequest, timeout)
                try {
                  getRolesResponse = Await.result(future, timeout.duration).asInstanceOf[GetRolesResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(getRolesResponse))
              }
            }
          },
          path(projectPrefix / "createMember") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val createMemberRequest = jValue.extract[CreateMemberRequest]
                var createMemberResponse: CreateMemberResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, createMemberRequest, timeout)
                try {
                  createMemberResponse = Await.result(future, timeout.duration).asInstanceOf[CreateMemberResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(createMemberResponse))
              }
            }
          },
          path(projectPrefix / "getMembers") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getMembersRequest = jValue.extract[GetMembersRequest]
                var getMembersResponse: GetMembersResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getMembersRequest, timeout)
                try {
                  getMembersResponse = Await.result(future, timeout.duration).asInstanceOf[GetMembersResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(getMembersResponse))
              }
            }
          },
          path(projectPrefix / "createPage") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val createPageRequest = jValue.extract[CreatePageRequest]
                var createPageResponse: CreatePageResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, createPageRequest, timeout)
                try {
                  createPageResponse = Await.result(future, timeout.duration).asInstanceOf[CreatePageResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(createPageResponse))
              }
            }
          },
          path(projectPrefix / "getPages") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getPagesRequest = jValue.extract[GetPagesRequest]
                var getPagesResponse: GetPagesResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getPagesRequest, timeout)
                try {
                  getPagesResponse = Await.result(future, timeout.duration).asInstanceOf[GetPagesResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(getPagesResponse))
              }
            }
          },
          path(projectPrefix / "getPages") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getPagesRequest = jValue.extract[GetPagesRequest]
                var getPagesResponse: GetPagesResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getPagesRequest, timeout)
                try {
                  getPagesResponse = Await.result(future, timeout.duration).asInstanceOf[GetPagesResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(getPagesResponse))
              }
            }
          },
          path(projectPrefix / "mapUserToRole") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val mapUserToRoleRequest = jValue.extract[MapUserToRoleRequest]
                var mapUserToRoleResponse: MapUserToRoleResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, mapUserToRoleRequest, timeout)
                try {
                  mapUserToRoleResponse = Await.result(future, timeout.duration).asInstanceOf[MapUserToRoleResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(mapUserToRoleResponse))
              }
            }
          },
          path(projectPrefix / "getMapUserToRole") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getMapUserToRoleRequest = jValue.extract[GetMapUserToRoleRequest]
                var getMapUserToRoleResponse: GetMapUserToRoleResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getMapUserToRoleRequest, timeout)
                try {
                  getMapUserToRoleResponse = Await.result(future, timeout.duration).asInstanceOf[GetMapUserToRoleResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(getMapUserToRoleResponse))
              }
            }
          },
          path(projectPrefix / "mapRoleToPage") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val mapRoleToPageRequest = jValue.extract[MapRoleToPageRequest]
                var mapRoleToPageResponse: MapRoleToPageResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, mapRoleToPageRequest, timeout)
                try {
                  mapRoleToPageResponse = Await.result(future, timeout.duration).asInstanceOf[MapRoleToPageResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(mapRoleToPageResponse))
              }
            }
          },
          path(projectPrefix / "getMapRoleToPage") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getMapRoleToPageRequest = jValue.extract[GetMapRoleToPageRequest]
                var getMapRoleToPageResponse: GetMapRoleToPageResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getMapRoleToPageRequest, timeout)
                try {
                  getMapRoleToPageResponse = Await.result(future, timeout.duration).asInstanceOf[GetMapRoleToPageResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(getMapRoleToPageResponse))
              }
            }
          },
          path(projectPrefix / "getRoleAccess") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getRoleAccessRequest = jValue.extract[GetRoleAccessRequest]
                var getRoleAccessResponse: GetRoleAccessResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getRoleAccessRequest, timeout)
                try {
                  getRoleAccessResponse = Await.result(future, timeout.duration).asInstanceOf[GetRoleAccessResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(getRoleAccessResponse))
              }
            }
          },
          path(projectPrefix / "getRoleAccess") {
            post {
              entity(as[String]) { data =>
                implicit val formats = DefaultFormats
                val jValue = parse(data)
                val getRoleAccessRequest = jValue.extract[GetRoleAccessRequest]
                var getRoleAccessResponse: GetRoleAccessResponse = null
                val future = Patterns.ask(StarterMain.adminSupervisorActorRef, getRoleAccessRequest, timeout)
                try {
                  getRoleAccessResponse = Await.result(future, timeout.duration).asInstanceOf[GetRoleAccessResponse]
                } catch {
                  case e: Exception =>
                    e.printStackTrace()
                }
                complete(write(getRoleAccessResponse))
              }
            }
          },

        )
      }
    }
  }

  def httpGet(url: String): String = {
    val result = Http(url)
      .option(HttpOptions.readTimeout(10000)).asString

    result.body
  }



  private def getResultAccumulatorActorRef(actorSystem: ActorSystem): ActorRef = {
    StarterMain.accumulatorResultActorRef
  }

  private def getLogsActorRef(actorSystem: ActorSystem): ActorRef = {
    StarterMain.logsActorRef

  }

  private def checkNull(input: String): Boolean = {
    if (input != null && !input.isEmpty) {
      return true
    }
    return false
  }
}

class AkkaHttpConnector {
}
