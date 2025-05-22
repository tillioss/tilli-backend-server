package com.teqbahn.actors.excel

import com.teqbahn.bootstrap.StarterMain
import com.teqbahn.caseclasses._
import com.teqbahn.global.{GlobalConstants, GlobalMessageConstants, ZiRedisCons}
import io.lettuce.core.api.sync.RedisCommands
import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.json4s.NoTypeHints
import org.json4s.native.Document.break
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.io.File
import java.sql.Timestamp
import java.util.Date

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

      val workbook = new HSSFWorkbook(new java.io.FileInputStream(excelFile))
      val sheet = workbook.getSheet("GenerateExcel")

      sheet.getRow(1).getCell(1).getStringCellValue shouldBe userId

      workbook.close()
    }

    "handle dynamic theme data correctly" in {
      val userId = "user234"
      val startDate = "2024-01-01"
      val endDate = "2024-01-31"
      val timestamp = new Timestamp(new Date().getTime).getTime

      val request = GameCsvFileGenrateRequest(
        userId = userId,
        startDate = startDate,
        endDate = endDate
      )

      val userGameStatus = UserGameStatus(
        points = 100,
        feelingTool = 5,
        level = 3
      )
      val userGameStatusJson = write(userGameStatus)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId))
        .thenReturn(userGameStatusJson)

      val user = User(
        userId = userId,
        emailId = "dynamic@example.com",
        name = "Dynamic User",
        password = "password456",
        nameOfChild = "Dynamic Child",
        ageOfChild = "8-10",
        passcode = "5678",
        status = GlobalConstants.ACTIVE,
        lastLogin = Some(timestamp),
        lastLogout = Some(timestamp),
        zipcode = None,
        genderOfChild = Some("female"),
        createdAt = Some(timestamp),
        ip = None,
        deviceInfo = None,
        schoolName = None,
        className = None
      )
      val userJson = write(user)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(userJson)

      val attemptKey = s"${ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST}_${userId}_2024-01-01"
      val attemptId = s"${userId}_level2_1"
      when(mockRedisCommands.llen(attemptKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(attemptKey, 0, -1))
        .thenReturn(java.util.Arrays.asList(attemptId))

      val dynamicLevelJson = """
  {
    "status":"complete",
    "startTime":1704067200000,
    "endTime":1704070800000,
    "dynamic":{
      "dynamicThemes":{
        "0":{
          "themeName":"Theme1",
          "userActionText":"Action1"
        },
        "1":{
          "themeName":"Theme2",
          "userActionText":"Action2"
        }
      }
    }
  }
  """

      val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

      val levelAttempt = LevelAttempt(
        levelJson = dynamicLevelJson,
        levelPoint = 150,
        createdAt = Some(timestamp),
        ip = Some("192.168.1.1"),
        deviceInfo = Some(userAgent),
        userTime = Some(timestamp),
        landingFrom = Some("email")
      )
      val levelAttemptJson = write(levelAttempt)
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level2", "1"))
        .thenReturn(true)
      when(mockRedisCommands.hget(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level2", "1"))
        .thenReturn(levelAttemptJson)

      val gameLevel = GameLevel(
        id = "level2",
        name = "Level 2",
        image = GameFileObject("fileId2", "title2", "file2.png", "image"),
        color = "#FF0000",
        sortOrder = 2
      )
      val gameLevelJson = write(gameLevel)
      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, "level2"))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, "level2"))
        .thenReturn(gameLevelJson)

      val generateExcel = system.actorOf(Props[GenerateExcel])

      generateExcel ! request

      Thread.sleep(1000)

      val excelFile = new File(s"${StarterMain.fileSystemPath}/excel/${userId}/${userId}.xls")
      excelFile.exists() shouldBe true

      val workbook = new HSSFWorkbook(new java.io.FileInputStream(excelFile))
      val sheet = workbook.getSheet("GenerateExcel")

      sheet.getRow(1).getCell(1).getStringCellValue shouldBe userId
      sheet.getRow(2).getCell(1).getStringCellValue shouldBe "Windows"

      sheet.getRow(3).getCell(1).getStringCellValue shouldBe "Chrome-91.0.4472.124"

      sheet.getRow(4).getCell(1).getStringCellValue shouldBe "email"

      workbook.close()
    }

    "handle static theme with stages correctly" in {
      val userId = "user345"
      val startDate = "2024-01-01"
      val endDate = "2024-01-31"
      val timestamp = new Timestamp(new Date().getTime).getTime

      val request = GameCsvFileGenrateRequest(
        userId = userId,
        startDate = startDate,
        endDate = endDate
      )

      val userGameStatus = UserGameStatus(
        points = 50,
        feelingTool = 2,
        level = 1
      )
      val userGameStatusJson = write(userGameStatus)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId))
        .thenReturn(userGameStatusJson)

      val user = User(
        userId = userId,
        emailId = "static@example.com",
        name = "Static User",
        password = "password789",
        nameOfChild = "Static Child",
        ageOfChild = "11-13",
        passcode = "9012",
        status = GlobalConstants.ACTIVE,
        lastLogin = Some(timestamp),
        lastLogout = Some(timestamp),
        zipcode = None,
        genderOfChild = None,
        createdAt = Some(timestamp),
        ip = None,
        deviceInfo = None,
        schoolName = None,
        className = None
      )
      val userJson = write(user)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(userJson)

      val attemptKey = s"${ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST}_${userId}_2024-01-01"
      val attemptId = s"${userId}_level3_1"
      when(mockRedisCommands.llen(attemptKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(attemptKey, 0, -1))
        .thenReturn(java.util.Arrays.asList(attemptId))

      val staticLevelJson =
        """
  {
    "status":"complete",
    "startTime":1704067200000,
    "endTime":1704070800000,
    "stages":[
      {
        "theme":"StoryCard",
        "startTime":1704067200000,
        "endTime":1704068100000,
        "storyPoints":10,
        "content":[
          {
            "theme":"AudioQuizScreen",
            "content":{
              "feelingsDataList":[
                {
                  "questionText":"How do you feel?",
                  "results":"Happy"
                }
              ]
            }
          }
        ]
      }
    ]
  }
  """

      val levelAttempt = LevelAttempt(
        levelJson = staticLevelJson,
        levelPoint = 75,
        createdAt = Some(timestamp),
        ip = Some("10.0.0.1"),
        deviceInfo = Some("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"),
        userTime = Some(timestamp),
        landingFrom = Some("social")
      )
      val levelAttemptJson = write(levelAttempt)
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level3", "1"))
        .thenReturn(true)
      when(mockRedisCommands.hget(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level3", "1"))
        .thenReturn(levelAttemptJson)

      val gameLevel = GameLevel(
        id = "level3",
        name = "Level 3",
        image = GameFileObject("fileId3", "title3", "file3.png", "image"),
        color = "#00FF00",
        sortOrder = 3
      )
      val gameLevelJson = write(gameLevel)
      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, "level3"))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, "level3"))
        .thenReturn(gameLevelJson)

      val generateExcel = system.actorOf(Props[GenerateExcel])

      generateExcel ! request

      Thread.sleep(1000)

      val excelFile = new File(s"${StarterMain.fileSystemPath}/excel/${userId}/${userId}.xls")
      excelFile.exists() shouldBe true

      val workbook = new HSSFWorkbook(new java.io.FileInputStream(excelFile))
      val sheet = workbook.getSheet("GenerateExcel")

      sheet.getRow(1).getCell(1).getStringCellValue shouldBe userId
      sheet.getRow(2).getCell(1).getStringCellValue shouldBe "Mac"
      sheet.getRow(4).getCell(1).getStringCellValue shouldBe "social"

      workbook.close()
    }

    "handle DropToSelection theme correctly" in {
      val userId = "user456"
      val startDate = "2024-01-01"
      val endDate = "2024-01-31"
      val timestamp = new Timestamp(new Date().getTime).getTime

      val request = GameCsvFileGenrateRequest(
        userId = userId,
        startDate = startDate,
        endDate = endDate
      )

      val userGameStatus = UserGameStatus(
        points = 200,
        feelingTool = 10,
        level = 4
      )
      val userGameStatusJson = write(userGameStatus)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId))
        .thenReturn(userGameStatusJson)

      val user = User(
        userId = userId,
        emailId = "drop@example.com",
        name = "Drop User",
        password = "password012",
        nameOfChild = "Drop Child",
        ageOfChild = "14-16",
        passcode = "3456",
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

      val attemptKey = s"${ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST}_${userId}_2024-01-01"
      val attemptId = s"${userId}_level4_1"
      when(mockRedisCommands.llen(attemptKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(attemptKey, 0, -1))
        .thenReturn(java.util.Arrays.asList(attemptId))

      val dropSelectionLevelJson =
        """
  {
    "status":"complete",
    "startTime":1704067200000,
    "endTime":1704070800000,
    "stages":[
      {
        "theme":"StoryCard",
        "startTime":1704067200000,
        "endTime":1704068100000,
        "storyPoints":15,
        "content":[
          {
            "theme":"DropToSelection",
            "content":{
              "chooseAnswer":"Correct",
              "circleSelect":"Inner Circle"
            }
          }
        ]
      }
    ]
  }
  """

      val levelAttempt = LevelAttempt(
        levelJson = dropSelectionLevelJson,
        levelPoint = 100,
        createdAt = Some(timestamp),
        ip = Some("172.16.0.1"),
        deviceInfo = Some("Mozilla/5.0 (Linux; Android 10)"),
        userTime = Some(timestamp),
        landingFrom = Some("app")
      )
      val levelAttemptJson = write(levelAttempt)
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level4", "1"))
        .thenReturn(true)
      when(mockRedisCommands.hget(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level4", "1"))
        .thenReturn(levelAttemptJson)

      val gameLevel = GameLevel(
        id = "level4",
        name = "Level 4",
        image = GameFileObject("fileId4", "title4", "file4.png", "image"),
        color = "#0000FF",
        sortOrder = 4
      )
      val gameLevelJson = write(gameLevel)
      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, "level4"))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, "level4"))
        .thenReturn(gameLevelJson)

      val generateExcel = system.actorOf(Props[GenerateExcel])

      generateExcel ! request

      Thread.sleep(1000)

      val excelFile = new File(s"${StarterMain.fileSystemPath}/excel/${userId}/${userId}.xls")
      excelFile.exists() shouldBe true

      val workbook = new HSSFWorkbook(new java.io.FileInputStream(excelFile))
      val sheet = workbook.getSheet("GenerateExcel")

      sheet.getRow(1).getCell(1).getStringCellValue shouldBe userId
      sheet.getRow(2).getCell(1).getStringCellValue shouldBe "Android"
      sheet.getRow(4).getCell(1).getStringCellValue shouldBe "app"

      workbook.close()
    }

    "update excel generation status in Redis when complete" in {
      val userId = "user567"
      val startDate = "2024-01-01"
      val endDate = "2024-01-31"
      val timestamp = new Timestamp(new Date().getTime).getTime

      val request = GameCsvFileGenrateRequest(
        userId = userId,
        startDate = startDate,
        endDate = endDate
      )

      val userGameStatus = UserGameStatus(
        points = 300,
        feelingTool = 8,
        level = 6
      )
      val userGameStatusJson = write(userGameStatus)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId))
        .thenReturn(userGameStatusJson)

      val user = User(
        userId = userId,
        emailId = "status@example.com",
        name = "Status User",
        password = "password345",
        nameOfChild = "Status Child",
        ageOfChild = "5-7",
        passcode = "7890",
        status = GlobalConstants.ACTIVE,
        lastLogin = Some(timestamp),
        lastLogout = Some(timestamp),
        zipcode = None,
        genderOfChild = Some("female"),
        createdAt = Some(timestamp),
        ip = None,
        deviceInfo = None,
        schoolName = None,
        className = None
      )
      val userJson = write(user)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(userJson)

      val attemptKey = s"${ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST}_${userId}_2024-01-01"
      val attemptId = s"${userId}_level5_1"
      when(mockRedisCommands.llen(attemptKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(attemptKey, 0, -1))
        .thenReturn(java.util.Arrays.asList(attemptId))

      val levelAttempt = LevelAttempt(
        levelJson = """{"status":"complete"}""",
        levelPoint = 120,
        createdAt = Some(timestamp),
        ip = Some("127.0.0.1"),
        deviceInfo = Some("Mozilla/5.0"),
        userTime = Some(timestamp),
        landingFrom = Some("direct")
      )
      val levelAttemptJson = write(levelAttempt)
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level5", "1"))
        .thenReturn(true)
      when(mockRedisCommands.hget(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level5", "1"))
        .thenReturn(levelAttemptJson)

      val gameLevel = GameLevel(
        id = "level5",
        name = "Level 5",
        image = GameFileObject("fileId5", "title5", "file5.png", "image"),
        color = "#FFFF00",
        sortOrder = 5
      )
      val gameLevelJson = write(gameLevel)
      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, "level5"))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, "level5"))
        .thenReturn(gameLevelJson)

      val excelStatusKey = s"${ZiRedisCons.USER_EXCEL_SHEET_STATUS}_${userId}"
      val existingStatus = ExcelSheetGenerateStatus(userId, timestamp, GlobalMessageConstants.PROCESSING)
      val existingStatusJson = write(existingStatus)
      when(mockRedisCommands.hget(excelStatusKey, userId))
        .thenReturn(existingStatusJson)

      val generateExcel = system.actorOf(Props[GenerateExcel])

      generateExcel ! request

      Thread.sleep(1000)

      val excelFile = new File(s"${StarterMain.fileSystemPath}/excel/${userId}/${userId}.xls")
      excelFile.exists() shouldBe true

      verify(mockRedisCommands).hset(
        org.mockito.ArgumentMatchers.eq(excelStatusKey),
        org.mockito.ArgumentMatchers.eq(userId),
        org.mockito.ArgumentMatchers.argThat((arg: String) => arg.contains(GlobalMessageConstants.COMPLETED))
      )
    }

    "handle case when existing excel files need to be deleted" in {
      val userId = "user678"
      val startDate = "2024-01-01"
      val endDate = "2024-01-31"
      val timestamp = new Timestamp(new Date().getTime).getTime

      val userFolderPath = s"${StarterMain.fileSystemPath}/excel/${userId}"
      new File(userFolderPath).mkdirs()

      val existingFilePath = s"${userFolderPath}/${userId}_old.xls"
      val existingFile = new File(existingFilePath)
      existingFile.createNewFile()

      val request = GameCsvFileGenrateRequest(
        userId = userId,
        startDate = startDate,
        endDate = endDate
      )

      val userGameStatus = UserGameStatus(
        points = 150,
        feelingTool = 3,
        level = 2
      )
      val userGameStatusJson = write(userGameStatus)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId))
        .thenReturn(userGameStatusJson)

      val user = User(
        userId = userId,
        emailId = "existing@example.com",
        name = "Existing User",
        password = "password678",
        nameOfChild = "Existing Child",
        ageOfChild = "8-10",
        passcode = "1234",
        status = GlobalConstants.ACTIVE,
        lastLogin = Some(timestamp),
        lastLogout = Some(timestamp),
        zipcode = None,
        genderOfChild = None,
        createdAt = Some(timestamp),
        ip = None,
        deviceInfo = None,
        schoolName = None,
        className = None
      )
      val userJson = write(user)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(userJson)

      val attemptKey = s"${ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST}_${userId}_2024-01-01"
      when(mockRedisCommands.llen(attemptKey)).thenReturn(0L)

      val generateExcel = system.actorOf(Props[GenerateExcel])

      generateExcel ! request

      Thread.sleep(1000)

      existingFile.exists() shouldBe false

      val newExcelFile = new File(s"${userFolderPath}/${userId}.xls")
      newExcelFile.exists() shouldBe true
    }

    "handle properly formatted device info with iPhone information" in {
      val userId = "iPhoneUser"
      val startDate = "2024-01-01"
      val endDate = "2024-01-31"
      val timestamp = new Timestamp(new Date().getTime).getTime

      val request = GameCsvFileGenrateRequest(
        userId = userId,
        startDate = startDate,
        endDate = endDate
      )

      val userGameStatus = UserGameStatus(
        points = 75,
        feelingTool = 3,
        level = 2
      )
      val userGameStatusJson = write(userGameStatus)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId))
        .thenReturn(userGameStatusJson)

      val user = User(
        userId = userId,
        emailId = "iphone@example.com",
        name = "iPhone User",
        password = "password789",
        nameOfChild = "Mobile Child",
        ageOfChild = "5-7",
        passcode = "4321",
        status = GlobalConstants.ACTIVE,
        lastLogin = Some(timestamp),
        lastLogout = Some(timestamp),
        genderOfChild = None,
        createdAt = Some(timestamp)
      )
      val userJson = write(user)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(userJson)

      val attemptKey = s"${ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST}_${userId}_2024-01-01"
      val attemptId = s"${userId}_level3_1"
      when(mockRedisCommands.llen(attemptKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(attemptKey, 0, -1))
        .thenReturn(java.util.Arrays.asList(attemptId))

      val iPhoneUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Mobile/15E148 Safari/604.1"
      val levelAttempt = LevelAttempt(
        levelJson = """{"status":"complete","startTime":1704067200000,"endTime":1704070800000}""",
        levelPoint = 80,
        createdAt = Some(timestamp),
        ip = Some("192.168.1.100"),
        deviceInfo = Some(iPhoneUserAgent),
        userTime = Some(timestamp),
        landingFrom = Some("social")
      )
      val levelAttemptJson = write(levelAttempt)
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level3", "1"))
        .thenReturn(true)
      when(mockRedisCommands.hget(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level3", "1"))
        .thenReturn(levelAttemptJson)

      val gameLevel = GameLevel(
        id = "level3",
        name = "Level 3",
        image = GameFileObject("fileId3", "title3", "file3.png", "image"),
        color = "#00FF00",
        sortOrder = 3
      )
      val gameLevelJson = write(gameLevel)
      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, "level3"))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, "level3"))
        .thenReturn(gameLevelJson)

      val generateExcel = system.actorOf(Props[GenerateExcel])
      generateExcel ! request

      Thread.sleep(1000)

      val excelFile = new File(s"${StarterMain.fileSystemPath}/excel/${userId}/${userId}.xls")
      excelFile.exists() shouldBe true

      val workbook = new HSSFWorkbook(new java.io.FileInputStream(excelFile))
      val sheet = workbook.getSheet("GenerateExcel")

      sheet.getRow(2).getCell(1).getStringCellValue shouldBe "Mac"
      sheet.getRow(3).getCell(1).getStringCellValue shouldBe "Safari-14.0.3"
      sheet.getRow(4).getCell(1).getStringCellValue shouldBe "social"

      workbook.close()
    }

    "handle Android device info correctly" in {
      val userId = "androidUser"
      val startDate = "2024-01-01"
      val endDate = "2024-01-31"
      val timestamp = new Timestamp(new Date().getTime).getTime

      val request = GameCsvFileGenrateRequest(
        userId = userId,
        startDate = startDate,
        endDate = endDate
      )

      val userGameStatus = UserGameStatus(
        points = 65,
        feelingTool = 2,
        level = 3
      )
      val userGameStatusJson = write(userGameStatus)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId))
        .thenReturn(userGameStatusJson)

      val user = User(
        userId = userId,
        emailId = "android@example.com",
        name = "Android User",
        password = "androidPass",
        nameOfChild = "Android Child",
        ageOfChild = "5-7",
        passcode = "2345",
        status = GlobalConstants.ACTIVE,
        lastLogin = Some(timestamp),
        lastLogout = Some(timestamp),
        genderOfChild = Some("male"),
        createdAt = Some(timestamp)
      )
      val userJson = write(user)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(userJson)

      val attemptKey = s"${ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST}_${userId}_2024-01-01"
      val attemptId = s"${userId}_level3_1"
      when(mockRedisCommands.llen(attemptKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(attemptKey, 0, -1))
        .thenReturn(java.util.Arrays.asList(attemptId))

      val androidUserAgent = "Mozilla/5.0 (Linux; Android 10; SM-G970F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.105 Mobile Safari/537.36"
      val levelAttempt = LevelAttempt(
        levelJson = """{"status":"complete","startTime":1704067200000,"endTime":1704070800000}""",
        levelPoint = 75,
        createdAt = Some(timestamp),
        ip = Some("192.168.1.150"),
        deviceInfo = Some(androidUserAgent),
        userTime = Some(timestamp),
        landingFrom = Some("direct")
      )
      val levelAttemptJson = write(levelAttempt)
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level3", "1"))
        .thenReturn(true)
      when(mockRedisCommands.hget(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level3", "1"))
        .thenReturn(levelAttemptJson)

      val gameLevel = GameLevel(
        id = "level3",
        name = "Level 3",
        image = GameFileObject("fileId3", "title3", "file3.png", "image"),
        color = "#00FF00",
        sortOrder = 3
      )
      val gameLevelJson = write(gameLevel)
      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, "level3"))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, "level3"))
        .thenReturn(gameLevelJson)

      val generateExcel = system.actorOf(Props[GenerateExcel])
      generateExcel ! request

      Thread.sleep(1000)

      val excelFile = new File(s"${StarterMain.fileSystemPath}/excel/${userId}/${userId}.xls")
      excelFile.exists() shouldBe true

      val workbook = new HSSFWorkbook(new java.io.FileInputStream(excelFile))
      val sheet = workbook.getSheet("GenerateExcel")

      sheet.getRow(2).getCell(1).getStringCellValue shouldBe "Android"
      sheet.getRow(3).getCell(1).getStringCellValue shouldBe "Chrome-89.0.4389.105"
      sheet.getRow(4).getCell(1).getStringCellValue shouldBe "direct"

      workbook.close()
    }

    "handle date formatting correctly" in {
      val userId = "dateUser"
      val startDate = "2024-02-29" // Leap year date
      val endDate = "2024-02-29"
      val timestamp = new Timestamp(new Date().getTime).getTime

      val request = GameCsvFileGenrateRequest(
        userId = userId,
        startDate = startDate,
        endDate = endDate
      )

      val userGameStatus = UserGameStatus(
        points = 110,
        feelingTool = 4,
        level = 5
      )
      val userGameStatusJson = write(userGameStatus)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId))
        .thenReturn(userGameStatusJson)

      val user = User(
        userId = userId,
        emailId = "date@example.com",
        name = "Date User",
        password = "datePass",
        nameOfChild = "Date Child",
        ageOfChild = "8-10",
        passcode = "8765",
        status = GlobalConstants.ACTIVE,
        lastLogin = Some(timestamp),
        lastLogout = Some(timestamp),
        genderOfChild = Some("female"),
        createdAt = Some(timestamp)
      )
      val userJson = write(user)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(userJson)

      val attemptKey = s"${ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST}_${userId}_2024-02-29"
      val attemptId = s"${userId}_level5_1"
      when(mockRedisCommands.llen(attemptKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(attemptKey, 0, -1))
        .thenReturn(java.util.Arrays.asList(attemptId))

      val specificStartTime = 1709226000000L // 2024-02-29 10:00:00
      val specificEndTime = 1709229600000L   // 2024-02-29 11:00:00

      val levelAttempt = LevelAttempt(
        levelJson = s"""{"status":"complete","startTime":$specificStartTime,"endTime":$specificEndTime}""",
        levelPoint = 95,
        createdAt = Some(timestamp),
        ip = Some("192.168.1.200"),
        deviceInfo = Some("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"),
        userTime = Some(timestamp),
        landingFrom = Some("email")
      )
      val levelAttemptJson = write(levelAttempt)
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level5", "1"))
        .thenReturn(true)
      when(mockRedisCommands.hget(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level5", "1"))
        .thenReturn(levelAttemptJson)

      val gameLevel = GameLevel(
        id = "level5",
        name = "Level 5",
        image = GameFileObject("fileId5", "title5", "file5.png", "image"),
        color = "#FFFF00",
        sortOrder = 5
      )
      val gameLevelJson = write(gameLevel)
      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, "level5"))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, "level5"))
        .thenReturn(gameLevelJson)

      val generateExcel = system.actorOf(Props[GenerateExcel])
      generateExcel ! request

      Thread.sleep(1000)

      val excelFile = new File(s"${StarterMain.fileSystemPath}/excel/${userId}/${userId}.xls")
      excelFile.exists() shouldBe true

      val workbook = new HSSFWorkbook(new java.io.FileInputStream(excelFile))
      val sheet = workbook.getSheet("GenerateExcel")

      // Verify the OS field is correctly populated
      sheet.getRow(2).getCell(1).getStringCellValue shouldBe "Windows"

      // Close the workbook
      workbook.close()
    }

    "process static theme with DropToSelection correctly" in {
      val userId = "user456"
      val startDate = "2024-01-01"
      val endDate = "2024-01-31"
      val timestamp = new Timestamp(new Date().getTime).getTime

      val request = GameCsvFileGenrateRequest(
        userId = userId,
        startDate = startDate,
        endDate = endDate
      )

      val userGameStatus = UserGameStatus(
        points = 50,
        feelingTool = 2,
        level = 1
      )
      val userGameStatusJson = write(userGameStatus)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId))
        .thenReturn(userGameStatusJson)

      val user = User(
        userId = userId,
        emailId = "static@example.com",
        name = "Static User",
        password = "password789",
        nameOfChild = "Static Child",
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

      val attemptKey = s"${ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST}_${userId}_2024-01-01"
      val attemptId = s"${userId}_level3_1"
      when(mockRedisCommands.llen(attemptKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(attemptKey, 0, -1))
        .thenReturn(java.util.Arrays.asList(attemptId))

      val staticThemeJson = """
{
  "status":"complete",
  "startTime":1704067200000,
  "endTime":1704070800000,
  "stages":[
    {
      "theme":"StoryCard",
      "storyPoints":10,
      "startTime":1704067200000,
      "endTime":1704068000000,
      "content":[
        {
          "theme":"DropToSelection",
          "content":{
            "chooseAnswer":"Correct",
            "circleSelect":"Inner Circle",
            "question":"How do you feel?"
          }
        }
      ]
    }
  ]
}
"""

      val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

      val levelAttempt = LevelAttempt(
        levelJson = staticThemeJson,
        levelPoint = 120,
        createdAt = Some(timestamp),
        ip = Some("192.168.1.2"),
        deviceInfo = Some(userAgent),
        userTime = Some(timestamp),
        landingFrom = Some("direct")
      )
      val levelAttemptJson = write(levelAttempt)
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level3", "1"))
        .thenReturn(true)
      when(mockRedisCommands.hget(s"${ZiRedisCons.USER_GAME_ATTEMPT_JSON}_${userId}_level3", "1"))
        .thenReturn(levelAttemptJson)

      val gameLevel = GameLevel(
        id = "level3",
        name = "Level 3",
        image = GameFileObject("fileId3", "title3", "file3.png", "image"),
        color = "#00FF00",
        sortOrder = 3
      )
      val gameLevelJson = write(gameLevel)
      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, "level3"))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, "level3"))
        .thenReturn(gameLevelJson)

      val generateExcel = system.actorOf(Props[GenerateExcel])

      generateExcel ! request

      Thread.sleep(1000)

      val excelFile = new File(s"${StarterMain.fileSystemPath}/excel/${userId}/${userId}.xls")
      excelFile.exists() shouldBe true

      val workbook = new HSSFWorkbook(new java.io.FileInputStream(excelFile))
      val sheet = workbook.getSheet("GenerateExcel")

      var found = false
      val lastRowNum = sheet.getLastRowNum
      for (i <- 10 to lastRowNum) {
        val row = sheet.getRow(i)
        if (row != null && row.getCell(2) != null) {
          val cellValue = row.getCell(2).getStringCellValue
          if (cellValue == "Story 1 Trust Circle") {
            found = true
            break
          }
        }
      }

      found shouldBe true

      workbook.close()
    }
  }
}
