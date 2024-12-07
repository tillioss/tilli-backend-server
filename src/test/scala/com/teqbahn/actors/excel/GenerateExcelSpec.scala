package com.teqbahn.actors.excel

import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.teqbahn.caseclasses._
import com.teqbahn.global.{ZiRedisCons, GlobalConstants}
import com.teqbahn.bootstrap.StarterMain
import org.mockito.MockitoSugar
import io.lettuce.core.api.sync.RedisCommands
import org.mockito.Mockito.{when, verify, times}
import java.io.File
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.util.Date
import java.sql.Timestamp
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import org.json4s.NoTypeHints

class GenerateExcelSpec 
    extends TestKit(ActorSystem("GenerateExcelSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with MockitoSugar {

  implicit val formats = Serialization.formats(NoTypeHints)
  
  // Mock Redis commands
  val mockRedisCommands = mock[RedisCommands[String, String]]
  
  override def beforeAll(): Unit = {
    StarterMain.redisCommands = mockRedisCommands
    StarterMain.fileSystemPath = "target/test-files"
    new File(StarterMain.fileSystemPath + "/excel").mkdirs()
  }

  override def afterAll(): Unit = {
    deleteTestFiles()
    TestKit.shutdownActorSystem(system)
  }

  private def deleteTestFiles(): Unit = {
    def deleteRecursively(file: File): Unit = {
      if (file.isDirectory) {
        file.listFiles.foreach(deleteRecursively)
      }
      file.delete()
    }
    deleteRecursively(new File("target/test-files"))
  }

  "GenerateExcel actor" should {
    "generate excel file with user game status" in {
      // Arrange
      val userId = "user123"
      val startDate = "2024-01-01"
      val endDate = "2024-01-31"
      val timestamp = new Timestamp(new Date().getTime).getTime
      
      val request = GameCsvFileGenrateRequest(
        userId = userId,
        startDate = startDate,
        endDate = endDate
      )

      // Mock user game status data
      val userGameStatus = UserGameStatus(
        points = 0,
        feelingTool = 0,
        level = 5
      )
      val userGameStatusJson = write(userGameStatus)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId))
        .thenReturn(userGameStatusJson)

      // Mock user data
      val user = User(
        userId = userId,
        emailId = "test@example.com",
        name = "Test User",
        password = "password123",
        nameOfChild = "Child Name",
        ageOfChild = "5-7",
        passcode = "1234",
        status = GlobalConstants.ACTIVE,
        lastLogin = Some(timestamp),
        lastLogout = Some(timestamp),
        zipcode = None,
        genderOfChild = Some("male"),
        createdAt = Some(timestamp),
        ip = None,
        deviceInfo = None,
        schoolName = None,
        className = None
      )
      val userJson = write(user)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(userJson)

      // Mock attempt data
      val attemptKey = s"${ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST}_${userId}_2024-01-01"
      val attemptId = s"${userId}_level1_1"
      when(mockRedisCommands.llen(attemptKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(attemptKey, 0, -1))
        .thenReturn(java.util.Arrays.asList(attemptId))

      // Mock level attempt data
      val levelAttempt = LevelAttempt(
        levelJson = """{"status":"complete"}""",
        levelPoint = 100,
        createdAt = Some(timestamp),
        ip = Some("127.0.0.1"),
        deviceInfo = Some("Mozilla/5.0"),
        userTime = Some(timestamp),
        landingFrom = Some("direct")
      )
      val levelAttemptJson = write(levelAttempt)
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level1", "1"))
        .thenReturn(true)
      when(mockRedisCommands.hget(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level1", "1"))
        .thenReturn(levelAttemptJson)

      // Mock level data
      val gameLevel = GameLevel(
        id = "level1",
        name = "Level 1",
        image = GameFileObject("fileId1", "title1", "file1.png", "image"),
        color = "#000000",
        sortOrder = 1
      )
      val gameLevelJson = write(gameLevel)
      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, "level1"))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, "level1"))
        .thenReturn(gameLevelJson)

      val generateExcel = system.actorOf(Props[GenerateExcel])
      
      // Act
      generateExcel ! request
      
      // Assert
      Thread.sleep(1000)
      
      val excelFile = new File(s"${StarterMain.fileSystemPath}/excel/${userId}/${userId}.xls")
      excelFile.exists() shouldBe true
      
      // Verify excel content
      val workbook = new HSSFWorkbook(new java.io.FileInputStream(excelFile))
      val sheet = workbook.getSheet("GenerateExcel")
      
      sheet.getRow(1).getCell(1).getStringCellValue shouldBe userId
      
      workbook.close()
    }
  }
} 