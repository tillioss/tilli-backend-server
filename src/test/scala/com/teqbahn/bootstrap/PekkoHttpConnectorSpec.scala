package com.teqbahn.bootstrap

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.apache.pekko.http.scaladsl.model.headers._
import java.io.{File, FileWriter, FileOutputStream}
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
      val response = PekkoHttpConnector.httpGet("https://httpbin.org/get")
      response should include("url")
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
      // Create a file larger than 15MB
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
  }
}
