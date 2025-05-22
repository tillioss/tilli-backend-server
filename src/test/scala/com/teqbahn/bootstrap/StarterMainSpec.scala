package com.teqbahn.bootstrap

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.io.File
import java.nio.file.{Files, Paths}

class StarterMainSpec extends TestKit(ActorSystem("StarterMainSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with MockitoSugar {

  val testDir = "target/test-files"
  val tempDir = s"$testDir/temp"
  val testImageDir = s"$testDir/images"
  val testImageName = "test-image.jpg"

  override def beforeAll(): Unit = {
    new File(testDir).mkdirs()
    new File(tempDir).mkdirs()
    new File(testImageDir).mkdirs()
    Files.createFile(Paths.get(s"$testImageDir/$testImageName"))
  }

  override def afterAll(): Unit = {
    deleteDirectory(new File(testDir))
    TestKit.shutdownActorSystem(system)
  }

  private def deleteDirectory(dir: File): Unit = {
    if (dir.isDirectory) {
      val children = dir.listFiles()
      if (children != null) {
        children.foreach(deleteDirectory)
      }
    }
    dir.delete()
  }

  "StarterMain" should {
    "create directory if it doesn't exist" in {
      val newDir = s"$testDir/newDir"
      StarterMain.createDir(newDir)
      new File(newDir).exists() shouldBe true
    }

    "not throw exception when creating directory that already exists" in {
      StarterMain.createDir(tempDir)
      new File(tempDir).exists() shouldBe true
    }

    "delete file path if exists" in {
      val filePath = s"$testDir/fileToDelete.txt"
      Files.createFile(Paths.get(filePath))
      new File(filePath).exists() shouldBe true
      StarterMain.deleteFilePath(filePath)
      new File(filePath).exists() shouldBe false
    }

    "not throw exception when deleting non-existent file" in {
      val nonExistentFile = s"$testDir/nonexistent.txt"
      StarterMain.deleteFilePath(nonExistentFile)
    }

    "correctly check if file exists" in {
      val existingFile = s"$testDir/existing.txt"
      Files.createFile(Paths.get(existingFile))
      StarterMain.fileExistCheck(existingFile) shouldBe true
      StarterMain.fileExistCheck(s"$testDir/nonexistent.txt") shouldBe false
    }

    "initialize environment variables for local environment" in {
      val args = Array(
        "local",
        "2552",
        "8092",
        "127.0.0.1",
        "localhost:6379",
        "test@example.com",
        "password",
        "/tmp/test"
      )

      StarterMain.main(args)

      StarterMain.envServer shouldBe "local"
      StarterMain.pekkoPort shouldBe 2552
      StarterMain.httpPort shouldBe 8092
      StarterMain.httpHostName shouldBe "127.0.0.1"
      StarterMain.redisHostPath shouldBe "localhost:6379"
      StarterMain.fromMail shouldBe "test@example.com"
      StarterMain.fromMailPassword shouldBe "password"
      StarterMain.fileSystemPath shouldBe "/tmp/test"
    }

    "print environment variables" in {
      StarterMain.projectName = "test-project"
      StarterMain.confFile = "test.conf"
      StarterMain.envServer = "test"
      StarterMain.fileSystemType = "File"
      StarterMain.fileSystemPath = "/test/path"
      StarterMain.projectPrefix = "test-api"
      StarterMain.pekkoPort = 1234
      StarterMain.httpPort = 5678
      StarterMain.pekkoManagementPort = 9012
      StarterMain.httpHostName = "test-host"
      StarterMain.pekkoManagementHostName = "test-mgmt-host"
      StarterMain.frontEndPath = "https://test.com"

      StarterMain.printEnv()
    }

    "handle missing environment variables gracefully" in {
      intercept[NumberFormatException] {
        StarterMain.main(Array("live"))
      }
    }

    "handle invalid port numbers gracefully" in {
      intercept[NumberFormatException] {
        StarterMain.main(Array(
          "local",
          "invalid_port", // This should cause NumberFormatException
          "8092",
          "127.0.0.1",
          "localhost:6379",
          "test@example.com",
          "password",
          "/tmp/test"
        ))
      }
    }
  }
}
