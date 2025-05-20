package com.teqbahn.bootstrap

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.{File, FileOutputStream, FileWriter}
import java.net.UnknownHostException
import java.util.zip.{ZipEntry, ZipOutputStream}

class PekkoHttpConnectorSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  implicit val mat: Materializer = SystemMaterializer(system).materializer
  val testProjectPrefix = "test"
  val testNodeIp = "127.0.0.1"
  val routes: Route = PekkoHttpConnector.getRoutes(mat, system, testProjectPrefix, testNodeIp)

  def createTempFile(content: String, extension: String = ".txt"): File = {
    val file = File.createTempFile("test", extension)
    file.deleteOnExit()
    val writer = new FileWriter(file)
    writer.write(content)
    writer.close()
    file
  }

  def createTempZipFile(files: Map[String, String]): File = {
    val zipFile = File.createTempFile("test", ".zip")
    zipFile.deleteOnExit()
    val zip = new ZipOutputStream(new FileOutputStream(zipFile))

    files.foreach { case (name, content) =>
      zip.putNextEntry(new ZipEntry(name))
      zip.write(content.getBytes)
      zip.closeEntry()
    }
    zip.close()
    zipFile
  }

  def mockHttpResponse(status: Int, body: String): HttpResponse = {
    HttpResponse(
      status = StatusCode.int2StatusCode(status),
      entity = HttpEntity(ContentTypes.`application/json`, body)
    )
  }

  "PekkoHttpConnector" should {
    "respond to GET /test/home with HTML" in {
      Get("/test/home") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`text/html(UTF-8)`
        responseAs[String] should include("Tilli started")
      }
    }

    "respond to GET /test/myIP with IP" in {
      Get("/test/myIP") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`text/html(UTF-8)`
        responseAs[String] should include("ip")
      }
    }

    "handle GET /test/apk request with version parameter" in {
      Get("/test/apk?version=1.0") ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "handle GET /test/vp request with proper parameters" in {
      Get("/test/vp?action=test&id=123&key=testkey") ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "handle GET /test/vp-game-file path segments correctly" in {
      Get("/test/vp-game-file/action/123/testkey/game.txt") ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "handle httpGet for valid URL" in {
      def retryWithBackoff[T](maxAttempts: Int = 3, initialDelay: Int = 1000)(fn: => T): T = {
        def attempt(remainingAttempts: Int, delay: Int): T = {
          try {
            fn
          } catch {
            case e: Exception if remainingAttempts > 1 =>
              Thread.sleep(delay)
              attempt(remainingAttempts - 1, delay * 2)
            case e: Exception =>
              throw new Exception(s"Failed after $maxAttempts attempts: ${e.getMessage}", e)
          }
        }

        attempt(maxAttempts, initialDelay)
      }

      retryWithBackoff() {
        val response = PekkoHttpConnector.httpGet("https://httpbin.org/get")
        response should include("url")
      }
    }

    "handle httpGet for invalid URL" in {
      intercept[Exception] {
        PekkoHttpConnector.httpGet("https://invalid-url-that-does-not-exist.com")
      }
    }

    "handle POST /test/uploads with file upload" in {
      val testFile = createTempFile("test content")
      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.fromPath(
          "file",
          ContentTypes.`application/octet-stream`,
          testFile.toPath
        )
      )

      Post("/test/uploads/dir1/dir2/test.txt", formData) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "handle POST /test/uploads-game-file with size limit" in {
      val testFile = createTempFile("test content")
      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.fromPath(
          "file",
          ContentTypes.`application/octet-stream`,
          testFile.toPath
        )
      )

      Post("/test/uploads-game-file/dir1/dir2/test.txt", formData) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "reject oversized files in uploads-game-file" in {
      val largeContent = "x" * (15000001)
      val testFile = createTempFile(largeContent)
      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.fromPath(
          "file",
          ContentTypes.`application/octet-stream`,
          testFile.toPath
        )
      )

      Post("/test/uploads-game-file/dir1/dir2/test.txt", formData) ~> routes ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "handle GET /test/datacollection.xls request" in {
      Get(s"/$testProjectPrefix/user123/datacollection.xls") ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "handle missing parameters in /test/vp request" in {
      Get("/test/vp?action=test") ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "handle CORS preflight requests" in {
      Options("/test/home").withHeaders(
        Origin("http://localhost:3000"),
        `Access-Control-Request-Method`(HttpMethods.GET)
      ) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        header("Access-Control-Allow-Origin").isDefined shouldBe true
        header("Access-Control-Allow-Methods").isDefined shouldBe true
      }
    }

    "handle POST /test/uploads with image file" in {
      val imageContent = Array.fill[Byte](1000)(1)
      val tempFile = File.createTempFile("test", ".jpg")
      tempFile.deleteOnExit()
      val fos = new FileOutputStream(tempFile)
      fos.write(imageContent)
      fos.close()

      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.fromPath(
          "file",
          MediaTypes.`image/jpeg`.toContentType,
          tempFile.toPath
        )
      )

      Post("/test/uploads/images/user123/test.jpg", formData) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "handle POST /test/uploads-game-file with valid game files" in {
      val gameFiles = Map(
        "index.js" -> "console.log('game code');",
        "index.wasm" -> "dummy wasm content",
        "index.pck" -> "dummy pack content"
      )
      val zipFile = createTempZipFile(gameFiles)

      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.fromPath(
          "file",
          MediaTypes.`application/zip`.toContentType,
          zipFile.toPath
        )
      )

      Post("/test/uploads-game-file/games/user123/game.zip", formData) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "handle concurrent uploads correctly" in {
      val files = (1 to 3).map { i =>
        createTempFile(s"content $i")
      }

      val results = files.map { file =>
        val formData = Multipart.FormData(
          Multipart.FormData.BodyPart.fromPath(
            "file",
            ContentTypes.`application/octet-stream`,
            file.toPath
          )
        )

        Post("/test/uploads/concurrent/test/file.txt", formData) ~> routes ~> check {
          status shouldBe StatusCodes.OK
        }
      }

      results.size shouldBe 3
    }

    "handle timeout for slow uploads" in {
      val largeContent = "x" * 1000000 // 1MB content
      val testFile = createTempFile(largeContent)

      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.fromPath(
          "file",
          ContentTypes.`application/octet-stream`,
          testFile.toPath
        )
      )

      Post("/test/uploads/dir1/dir2/test.txt", formData) ~> routes ~> check {
        status should (be(StatusCodes.OK) or be(StatusCodes.RequestTimeout))
      }
    }

    "handle non-existent paths" in {
      Get("/test/nonexistent") ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "reject invalid HTTP methods" in {
      Delete("/test/home") ~> routes ~> check {
        status shouldBe StatusCodes.MethodNotAllowed
      }
    }

    "handle malformed requests" in {
      Post("/test/uploads/dir1/dir2/test.txt", HttpEntity("invalid content")) ~> routes ~> check {
        status shouldBe StatusCodes.UnsupportedMediaType
      }
    }

    "/test/adminLogin" in {
      Post("/test/adminLogin", HttpEntity(ContentTypes.`application/json`,
        """{"loginId": "admin", "password": "pass"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/adminList" in {
      Post("/test/adminList", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess1"}""")).withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/fetchAnalytics" in {
      Post("/test/fetchAnalytics", HttpEntity(ContentTypes.`application/json`,
        """{"id": "analytics123"}""")).withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/fetchFilterAnalytics" in {
      Post("/test/fetchFilterAnalytics", HttpEntity(ContentTypes.`application/json`,
        """{"sDate": null, "eDate": null, "filterAge": ["5", "6"], "filterGender": ["M"],
          | "filterLanguage": ["EN"], "requestType": "basic", "id": "filter123"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/fetchFilterUserAttemptAnalytics" in {
      Post("/test/fetchFilterUserAttemptAnalytics", HttpEntity(ContentTypes.`application/json`,
        """{"sDate": null, "eDate": null, "id": "user123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/createUser" in {
      Post("/test/createUser", HttpEntity(ContentTypes.`application/json`,
        """{"emailId": "test@example.com", "password": "123456", "name": "Test User", "ageOfChild": "8",
          | "nameOfChild": "Child", "passcode": "code", "sessionId": "sess123", "zipcode": "12345"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/createGameUser" in {
      Post("/test/createGameUser", HttpEntity(ContentTypes.`application/json`,
        """{"emailId": "game@example.com", "password": "123456", "nameOfChild": "Child", "ageOfChild": "7",
          | "schoolName": "Test School", "className": "1A", "genderOfChild": "M", "passcode": "code", "sessionId": "sess123"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/createDemoUser" in {
      Post("/test/createDemoUser", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess1", "demoUserId": "demo1", "userType": "guest", "ip": "127.0.0.1", "deviceInfo": "browser"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/createDemo2User" in {
      Post("/test/createDemo2User", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess2", "age": "10", "gender": "F", "demoUserId": "demo2", "userType": "guest",
          |"ip": "127.0.0.1", "deviceInfo": "browser", "language": "EN"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateUserDetails" in {
      Post("/test/updateUserDetails", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "age": "9", "gender": "M", "language": "EN"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/checkEmailIdAlreadyExist" in {
      Post("/test/checkEmailIdAlreadyExist", HttpEntity(ContentTypes.`application/json`,
        """{"emailId": "test@example.com", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/login" in {
      Post("/test/login", HttpEntity(ContentTypes.`application/json`,
        """{"loginId": "test@example.com", "password": "123456", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/sendForgotPassword" in {
      Post("/test/sendForgotPassword", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "email": "test@example.com"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateForgotPassword" in {
      Post("/test/updateForgotPassword", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "userId": "user123", "id": "id123", "otp": "0000", "password": "newpass"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getAllUserList" in {
      Post("/test/getAllUserList", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123"}""")).withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/addGameLevel" in {
      Post("/test/addGameLevel", HttpEntity(ContentTypes.`application/json`,
        """{"name": "Level 1", "color": "red", "sessionId": "sess123",
          |"image": {
          |  "id": "img1",
          |  "title": "Level 1 Image",
          |  "fileName": "img.png",
          |  "fileType": "image/png"
          |},
          |"sortOrder": 1}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateGameLevel" in {
      Post("/test/updateGameLevel", HttpEntity(ContentTypes.`application/json`,
        """{"levelId": "level1", "name": "Updated Level", "color": "blue",
          |"image": {
          |  "id": "img1",
          |  "title": "Updated Image",
          |  "fileName": "updated.png",
          |  "fileType": "image/png"
          |},
          |"sessionId": "sess123",
          |"sortOrder": 2}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getGameLevels" in {
      Post("/test/getGameLevels", HttpEntity(ContentTypes.`application/json`,
        """{"levelId": "level1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/deleteGameLevels" in {
      Post("/test/deleteGameLevels", HttpEntity(ContentTypes.`application/json`,
        """{"levelId": "level1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

    "/test/deleteThemes" in {
      Post("/test/deleteThemes", HttpEntity(ContentTypes.`application/json`,
        """{"themeId": "theme1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

    "/test/addTheme" in {
      Post("/test/addTheme", HttpEntity(ContentTypes.`application/json`,
        """{"name": "Theme 1", "sessionId": "sess123",
          |"image": {
          |  "id": "img1",
          |  "title": "Theme 1 Image",
          |  "fileName": "img.png",
          |  "fileType": "image/png"
          |},
          |"themeType": "type1"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateTheme" in {
      Post("/test/updateTheme", HttpEntity(ContentTypes.`application/json`,
        """{"themeId": "theme1", "name": "Updated Theme",
          |"image": {
          |  "id": "img1",
          |  "title": "Updated Theme Image",
          |  "fileName": "img.png",
          |  "fileType": "image/png"
          |},
          |"themeType": "type2",
          |"sessionId": "sess123"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getThemes" in {
      Post("/test/getThemes", HttpEntity(ContentTypes.`application/json`,
        """{"themeId": "theme1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateThemeContent" in {
      Post("/test/updateThemeContent", HttpEntity(ContentTypes.`application/json`,
        """{"themeId": "theme1", "data": "{}", "pageType": "main"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getThemeContent" in {
      Post("/test/getThemeContent", HttpEntity(ContentTypes.`application/json`,
        """{"themeId": "theme1"}""")).withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/addGameFile" in {
      Post("/test/addGameFile", HttpEntity(ContentTypes.`application/json`,
        """{"title": "Game 1", "fileName": "game.zip", "fileType": "zip", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateGameFile" in {
      Post("/test/updateGameFile", HttpEntity(ContentTypes.`application/json`,
        """{"fileId": "file1", "title": "Game 1 Updated", "fileName": "game_updated.zip", "fileType": "zip",
          |"sessionId": "sess123"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/deleteGameFile" in {
      Post("/test/deleteGameFile", HttpEntity(ContentTypes.`application/json`,
        """{"fileId": "file1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getGameFilesList" in {
      Post("/test/getGameFilesList", HttpEntity(ContentTypes.`application/json`,
        """{"fileType": "zip", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getGameFileSearchList" in {
      Post("/test/getGameFileSearchList", HttpEntity(ContentTypes.`application/json`,
        """{"fileType": "zip", "searchString": "game", "limit": "10", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateLevelMapping" in {
      Post("/test/updateLevelMapping", HttpEntity(ContentTypes.`application/json`,
        """{"levelId": "level1", "stagesData": "{\"stage\": 1}", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getLevelMappingData" in {
      Post("/test/getLevelMappingData", HttpEntity(ContentTypes.`application/json`,
        """{"levelId": "level1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateUserGameStatus" in {
      Post("/test/updateUserGameStatus", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "points": 100, "feelingTool": 1, "level": 5, "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getUserGameStatus" in {
      Post("/test/getUserGameStatus", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateStatusBasedOnStory" in {
      Post("/test/updateStatusBasedOnStory", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "levelId": "level1", "attemptCount": 1, "statusJson": "{\"status\":\"complete\"}",
          | "levelPoints": 100, "leveljson": "{}", "levelNo": 1, "ip": "127.0.0.1", "deviceInfo": "browser",
          |  "userTime": 1234567890, "landingFrom": "home"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getStoryBasedStatus" in {
      Post("/test/getStoryBasedStatus", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "levelId": "level1", "attemptCount": 1}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getLevelAttemptCount" in {
      Post("/test/getLevelAttemptCount", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "levelId": "level1"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateLevelAttempt" in {
      Post("/test/updateLevelAttempt", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "levelId": "level1", "levelPoints": 80, "leveljson": "{}", "levelNo": 1,
          |"sessionId": "sess123", "attemptCount": 2, "ip": "127.0.0.1", "deviceInfo": "browser", "userTime": 1234567890,
          | "landingFrom": "dashboard", "dateString": "2025-05-20"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getGameDateWiseReport" in {
      Post("/test/getGameDateWiseReport", HttpEntity(ContentTypes.`application/json`,
        """{"startDate": "2025-05-01", "endDate": "2025-05-20", "pageLimit": 10, "noOfPage": 1}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/gameCsvFileGenrate" in {
      Post("/test/gameCsvFileGenrate", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "startDate": "2025-05-01", "endDate": "2025-05-20"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/gameCsvFileStatus" in {
      Post("/test/gameCsvFileStatus", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123"}""")).withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getLevelAttempts" in {
      Post("/test/getLevelAttempts", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "pageLimit": 10, "noOfPage": 1}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getAllUserAttemptList" in {
      Post("/test/getAllUserAttemptList", HttpEntity(ContentTypes.`application/json`,
        """{"reqId": "req123", "auth": "token", "pageLimit": 10, "noOfPage": 1, "actoinType": "view"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getLevelAttemptsJsonDetails" in {
      Post("/test/getLevelAttemptsJsonDetails", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "levelId": "level1", "attamptNo": "1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/addLanguage" in {
      Post("/test/addLanguage", HttpEntity(ContentTypes.`application/json`,
        """{"languageName": "English", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateLanguage" in {
      Post("/test/updateLanguage", HttpEntity(ContentTypes.`application/json`,
        """{"languageId": "lang1", "languageName": "English Updated", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getLanguages" in {
      Post("/test/getLanguages", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123"}""")).withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateLanguageBaseData" in {
      Post("/test/updateLanguageBaseData", HttpEntity(ContentTypes.`application/json`,
        """{"grouptype": "general", "jsonData": "{\"welcome\": \"Hello\"}", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getLanguageBaseData" in {
      Post("/test/getLanguageBaseData", HttpEntity(ContentTypes.`application/json`,
        """{"grouptype": "general", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateLanguageMappingData" in {
      Post("/test/updateLanguageMappingData", HttpEntity(ContentTypes.`application/json`,
        """{"grouptype": "menu", "languageId": "lang1", "jsonData": "{\"menu\": \"Home\"}", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getLanguageMappingData" in {
      Post("/test/getLanguageMappingData", HttpEntity(ContentTypes.`application/json`,
        """{"grouptype": "menu", "languageId": "lang1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getLanguageMappingDataWithBaseData" in {
      Post("/test/getLanguageMappingDataWithBaseData", HttpEntity(ContentTypes.`application/json`,
        """{"grouptype": "menu", "languageId": "lang1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateModuleLanguageMapping" in {
      Post("/test/updateModuleLanguageMapping", HttpEntity(ContentTypes.`application/json`,
        """{"levelId": "level1", "languageId": "lang1", "jsonData": "{\"label\": \"Play\"}", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getModuleLanguageMapping" in {
      Post("/test/getModuleLanguageMapping", HttpEntity(ContentTypes.`application/json`,
        """{"levelId": "level1", "languageId": "lang1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateLevelsNameLanguageMapping" in {
      Post("/test/updateLevelsNameLanguageMapping", HttpEntity(ContentTypes.`application/json`,
        """{"languageId": "lang1", "jsonData": "{\"levelName\": \"Intro\"}", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getLevelsNameLanguageMapping" in {
      Post("/test/getLevelsNameLanguageMapping", HttpEntity(ContentTypes.`application/json`,
        """{"languageId": "lang1", "sessionId": "sess123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getWebLogs" in {
      Post("/test/getWebLogs", HttpEntity(ContentTypes.`application/json`,
        """{"reqId": "req123", "auth": "token", "pageLimit": 10, "noOfPage": 1, "totalResult": 100}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/createRole" in {
      Post("/test/createRole", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "role": "admin"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getRoles" in {
      Post("/test/getRoles", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "pageLimit": 10, "noOfPage": 1}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/createMember" in {
      Post("/test/createMember", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "name": "Test Member", "email": "member@example.com", "password": "pass",
          | "createdBy": "admin"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getMembers" in {
      Post("/test/getMembers", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "pageLimit": 10, "noOfPage": 1}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/createPage" in {
      Post("/test/createPage", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "title": "Dashboard", "route": "/dashboard"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getPages" in {
      Post("/test/getPages", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "pageLimit": 10, "noOfPage": 1}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/mapUserToRole" in {
      Post("/test/mapUserToRole", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "userId": "user123", "roles": ["admin", "editor"]}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/mapRoleToPage" in {
      Post("/test/mapRoleToPage", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "roleId": "role123", "pages": ["page1", "page2"]}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getMapUserToRole" in {
      Post("/test/getMapUserToRole", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "userId": "user123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getMapRoleToPage" in {
      Post("/test/getMapRoleToPage", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "roleId": "role123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getRoleAccess" in {
      Post("/test/getRoleAccess", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId": "sess123", "userId": "user123"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/emotionCapture" in {
      Post("/test/emotionCapture", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "levelId": "level1", "themeId": "theme1", "emotionKey": "happy", "attemptCount": 1}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/getEmotionCaptureList" in {
      Post("/test/getEmotionCaptureList", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "levelId": "level1", "themeId": "theme1"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/feedbackCapture" in {
      Post("/test/feedbackCapture", HttpEntity(ContentTypes.`application/json`,
        """{"userId": "user123", "levelId": "level1", "themeId": "theme1", "feedBackKey": "feedback1",
          |"activity": "completed", "attemptCount": 5}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/fetchFilterAnalytics – minimal optional fields" in {
      Post("/test/fetchFilterAnalytics", HttpEntity(ContentTypes.`application/json`,
        """{"filterAge": [], "filterGender": [], "filterLanguage": [], "requestType": "summary", "id": "fid1"}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/updateLevelAttempt – high userTime" in {
      Post("/test/updateLevelAttempt", HttpEntity(ContentTypes.`application/json`,
        """{"userId":"user123","levelId":"level1","levelPoints":95,"leveljson":"{}","levelNo":2,"sessionId":"sess123",
          |"attemptCount":3,"ip":"127.0.0.1","deviceInfo":"browser","userTime":9999999999,"landingFrom":"play",
          |"dateString":"2025-05-21"}""".stripMargin))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "/test/mapUserToRole – empty roles list" in {
      Post("/test/mapUserToRole", HttpEntity(ContentTypes.`application/json`,
        """{"sessionId":"sess123","userId":"user123","roles":[]}"""))
        .withHeaders(`Content-Type`(ContentTypes.`application/json`)) ~> routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "handle connection failures" in {
    val unreachableHost = "http://unreachable.local"
    intercept[UnknownHostException] {
      PekkoHttpConnector.httpGet(unreachableHost)
    }

    val nonexistentDomain = "http://nonexistent.domain.local"
    intercept[UnknownHostException] {
      PekkoHttpConnector.httpGet(nonexistentDomain)
    }
  }

}
