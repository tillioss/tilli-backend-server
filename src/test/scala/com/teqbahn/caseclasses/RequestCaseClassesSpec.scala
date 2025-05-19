package com.teqbahn.caseclasses;

import org.json4s._
import org.scalatest.flatspec.AnyFlatSpec;

import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.scalatest.matchers.should.Matchers

class RequestCaseClassesSpec extends AnyFlatSpec with Matchers {
  implicit val formats: Formats = Serialization.formats(NoTypeHints)

  "AdminLogin" should "serialize and deserialize correctly" in {
    val login = AdminLogin(
      name = "Admin",
      password = "password123",
      adminType = "super",
      loginId = "admin@example.com"
    )

    val json = write(login)
    val deserialized = read[AdminLogin](json)
    deserialized shouldEqual login
  }

  "CreateUserRequest" should "handle optional fields correctly" in {
    val request = CreateUserRequest(
      emailId = "user@example.com",
      password = "pass123",
      name = "Test User",
      ageOfChild = "10",
      nameOfChild = "Child Name",
      passcode = "1234",
      sessionId = "session123",
      zipcode = Some("12345")
    )

    val json = write(request)
    val deserialized = read[CreateUserRequest](json)
    deserialized shouldEqual request

    val requestNoZip = request.copy(zipcode = None)
    val jsonNoZip = write(requestNoZip)
    val deserializedNoZip = read[CreateUserRequest](jsonNoZip)
    deserializedNoZip shouldEqual requestNoZip
  }

  "CreateDemoUserRequest" should "handle all optional fields" in {
    val request = CreateDemoUserRequest(
      sessionId = "session123",
      demoUserId = "demo1",
      userType = Some("test"),
      ip = Some("127.0.0.1"),
      deviceInfo = Some("Chrome")
    )

    val json = write(request)
    val deserialized = read[CreateDemoUserRequest](json)
    deserialized shouldEqual request

    val minimalRequest = CreateDemoUserRequest(
      sessionId = "session123",
      demoUserId = "demo1"
    )
    val minimalJson = write(minimalRequest)
    val deserializedMinimal = read[CreateDemoUserRequest](minimalJson)
    deserializedMinimal shouldEqual minimalRequest
  }

  "CreateDemo2UserRequest" should "handle optional fields correctly" in {
    val request = CreateDemo2UserRequest(
      sessionId = "session123",
      age = "12",
      gender = "Female",
      demoUserId = "demo2",
      userType = Some("student"),
      ip = Some("192.168.1.1"),
      deviceInfo = Some("Firefox"),
      language = Some("en")
    )

    val json = write(request)
    val deserialized = read[CreateDemo2UserRequest](json)
    deserialized shouldEqual request

    val minimalRequest = CreateDemo2UserRequest(
      sessionId = "session123",
      age = "12",
      gender = "Female",
      demoUserId = "demo2"
    )

    val minimalJson = write(minimalRequest)
    val deserializedMinimal = read[CreateDemo2UserRequest](minimalJson)
    deserializedMinimal shouldEqual minimalRequest
  }

  "GetLoginRequest" should "serialize and deserialize correctly" in {
    val request = GetLoginRequest("user@example.com", "pass123", "session123")
    val json = write(request)
    val deserialized = read[GetLoginRequest](json)
    deserialized shouldEqual request
  }

  "GetLogoutRequest" should "serialize and deserialize correctly" in {
    val request = GetLogoutRequest("user123", "session123")
    val json = write(request)
    val deserialized = read[GetLogoutRequest](json)
    deserialized shouldEqual request
  }

  "InitUsersActorRequest" should "serialize and deserialize correctly" in {
    val request = InitUsersActorRequest("user123")
    val json = write(request)
    val deserialized = read[InitUsersActorRequest](json)
    deserialized shouldEqual request
  }

  "CheckEmailIdAlreadyExistRequest" should "serialize and deserialize correctly" in {
    val request = CheckEmailIdAlreadyExistRequest("user@example.com", "session123")
    val json = write(request)
    val deserialized = read[CheckEmailIdAlreadyExistRequest](json)
    deserialized shouldEqual request
  }

  "UpdateUserDetailsRequest" should "serialize and deserialize correctly" in {
    val request = UpdateUserDetailsRequest("user123", "12", "Female", "en")
    val json = write(request)
    val deserialized = read[UpdateUserDetailsRequest](json)
    deserialized shouldEqual request
  }

  "SendForgotPasswordRequest" should "serialize and deserialize correctly" in {
    val request = SendForgotPasswordRequest("session123", "user@example.com")
    val json = write(request)
    val deserialized = read[SendForgotPasswordRequest](json)
    deserialized shouldEqual request
  }

  "UpdateForgotPasswordRequest" should "serialize and deserialize correctly" in {
    val request = UpdateForgotPasswordRequest("session123", "user123", "id456", "otp789", "newPassword123")
    val json = write(request)
    val deserialized = read[UpdateForgotPasswordRequest](json)
    deserialized shouldEqual request
  }

  def createSampleGameFileObject(): GameFileObject = {
    GameFileObject(
      id = "file123",
      title = "Sample File",
      fileName = "sample.png",
      fileType = "image"
    )
  }

  def createSampleFileObject(): FileObject = {
    FileObject(
      processType = "upload",
      fileName = "game.json",
      fileType = "json",
      origFileName = "original_game.json"
    )
  }

  "AddGameLevelRequest" should "serialize and deserialize correctly" in {
    val request = AddGameLevelRequest(
      name = "Level 1",
      color = "#FF0000",
      sessionId = "session123",
      image = createSampleGameFileObject(),
      sortOrder = 1
    )

    val json = write(request)
    val deserialized = read[AddGameLevelRequest](json)
    deserialized shouldEqual request
  }

  "UpdateGameLevelRequest" should "serialize and deserialize correctly" in {
    val request = UpdateGameLevelRequest(
      levelId = "level123",
      name = "Updated Level",
      color = "#00FF00",
      image = createSampleGameFileObject(),
      sessionId = "session123",
      sortOrder = 2
    )

    val json = write(request)
    val deserialized = read[UpdateGameLevelRequest](json)
    deserialized shouldEqual request
  }

  "GetGameLevelsRequest" should "serialize and deserialize correctly" in {
    val request = GetGameLevelsRequest("level123", "session123")
    val json = write(request)
    val deserialized = read[GetGameLevelsRequest](json)
    deserialized shouldEqual request
  }

  "DeleteGameLevelsRequest" should "serialize and deserialize correctly" in {
    val request = DeleteGameLevelsRequest("level123", "session123")
    val json = write(request)
    val deserialized = read[DeleteGameLevelsRequest](json)
    deserialized shouldEqual request
  }

  "AddThemeRequest" should "handle optional fields correctly" in {
    val fullRequest = AddThemeRequest(
      name = "Theme 1",
      sessionId = "session123",
      image = createSampleGameFileObject(),
      themeType = "custom",
      gameFile = Some(createSampleFileObject())
    )

    val fullJson = write(fullRequest)
    val deserializedFull = read[AddThemeRequest](fullJson)
    deserializedFull shouldEqual fullRequest

    val minimalRequest = fullRequest.copy(gameFile = None)
    val minimalJson = write(minimalRequest)
    val deserializedMinimal = read[AddThemeRequest](minimalJson)
    deserializedMinimal shouldEqual minimalRequest
  }

  "UpdateThemeRequest" should "handle optional fields correctly" in {
    val fullRequest = UpdateThemeRequest(
      themeId = "theme123",
      name = "Updated Theme",
      image = createSampleGameFileObject(),
      themeType = "custom",
      sessionId = "session123",
      gameFile = Some(createSampleFileObject())
    )

    val fullJson = write(fullRequest)
    val deserializedFull = read[UpdateThemeRequest](fullJson)
    deserializedFull shouldEqual fullRequest

    val minimalRequest = fullRequest.copy(gameFile = None)
    val minimalJson = write(minimalRequest)
    val deserializedMinimal = read[UpdateThemeRequest](minimalJson)
    deserializedMinimal shouldEqual minimalRequest
  }

  "GetThemesRequest" should "serialize and deserialize correctly" in {
    val request = GetThemesRequest("theme123", "session123")
    val json = write(request)
    val deserialized = read[GetThemesRequest](json)
    deserialized shouldEqual request
  }

  "DeleteThemesRequest" should "serialize and deserialize correctly" in {
    val request = DeleteThemesRequest("theme123", "session123")
    val json = write(request)
    val deserialized = read[DeleteThemesRequest](json)
    deserialized shouldEqual request
  }

  "UpdateThemeContentRequest" should "handle optional fields correctly" in {
    val fullRequest = UpdateThemeContentRequest(
      themeId = "theme123",
      data = "theme content",
      pageType = Some("main")
    )

    val fullJson = write(fullRequest)
    val deserializedFull = read[UpdateThemeContentRequest](fullJson)
    deserializedFull shouldEqual fullRequest

    val minimalRequest = fullRequest.copy(pageType = None)
    val minimalJson = write(minimalRequest)
    val deserializedMinimal = read[UpdateThemeContentRequest](minimalJson)
    deserializedMinimal shouldEqual minimalRequest
  }

  "GetThemeContentRequest" should "serialize and deserialize correctly" in {
    val request = GetThemeContentRequest("theme123")
    val json = write(request)
    val deserialized = read[GetThemeContentRequest](json)
    deserialized shouldEqual request
  }

  "AddGameFileRequest" should "serialize and deserialize correctly" in {
    val request = AddGameFileRequest(
      title = "Game File",
      fileName = "game.png",
      fileType = "image",
      sessionId = "session123"
    )

    val json = write(request)
    val deserialized = read[AddGameFileRequest](json)
    deserialized shouldEqual request
  }

  "UpdateGameFileRequest" should "serialize and deserialize correctly" in {
    val request = UpdateGameFileRequest(
      fileId = "file123",
      title = "Updated Game File",
      fileName = "updated_game.png",
      fileType = "image",
      sessionId = "session123"
    )

    val json = write(request)
    val deserialized = read[UpdateGameFileRequest](json)
    deserialized shouldEqual request
  }

  "GetGameFilesListRequest" should "serialize and deserialize correctly" in {
    val request = GetGameFilesListRequest("image", "session123")
    val json = write(request)
    val deserialized = read[GetGameFilesListRequest](json)
    deserialized shouldEqual request
  }

  "GetGameFileSearchListRequest" should "serialize and deserialize correctly" in {
    val request = GetGameFileSearchListRequest(
      fileType = "image",
      searchString = "game",
      limit = "10",
      sessionId = "session123"
    )

    val json = write(request)
    val deserialized = read[GetGameFileSearchListRequest](json)
    deserialized shouldEqual request
  }

  "DeleteGameFileSearchListRequest" should "serialize and deserialize correctly" in {
    val request = DeleteGameFileSearchListRequest("file123", "session123")
    val json = write(request)
    val deserialized = read[DeleteGameFileSearchListRequest](json)
    deserialized shouldEqual request
  }

  "UpdateLevelMappingRequest" should "serialize and deserialize correctly" in {
    val request = UpdateLevelMappingRequest("level123", "stageData", "session123")
    val json = write(request)
    val deserialized = read[UpdateLevelMappingRequest](json)
    deserialized shouldEqual request
  }

  "GetLevelMappingDataRequest" should "serialize and deserialize correctly" in {
    val request = GetLevelMappingDataRequest("level123", "session123")
    val json = write(request)
    val deserialized = read[GetLevelMappingDataRequest](json)
    deserialized shouldEqual request
  }

  "UpdateUserGameStatusRequest" should "serialize and deserialize correctly" in {
    val request = UpdateUserGameStatusRequest("user123", 100, 5, 3, "session123")
    val json = write(request)
    val deserialized = read[UpdateUserGameStatusRequest](json)
    deserialized shouldEqual request
  }

  "GetUserGameStatusRequest" should "serialize and deserialize correctly" in {
    val request = GetUserGameStatusRequest("user123", "session123")
    val json = write(request)
    val deserialized = read[GetUserGameStatusRequest](json)
    deserialized shouldEqual request
  }

  "GetLevelAttemptCountRequest" should "serialize and deserialize correctly" in {
    val request = GetLevelAttemptCountRequest("user123", "level123")
    val json = write(request)
    val deserialized = read[GetLevelAttemptCountRequest](json)
    deserialized shouldEqual request
  }

  "UpdateLevelAttemptRequest" should "serialize and deserialize correctly" in {
    val request = UpdateLevelAttemptRequest(
      "user123", "level123", 100, "levelJson", 1, "session123",
      2, "127.0.0.1", "Chrome", 1234567890L, "menu", "2024-03-20"
    )
    val json = write(request)
    val deserialized = read[UpdateLevelAttemptRequest](json)
    deserialized shouldEqual request
  }

  "GetGameDateWiseReportRequest" should "serialize and deserialize correctly" in {
    val request = GetGameDateWiseReportRequest("2024-03-01", "2024-03-20", 10, 1)
    val json = write(request)
    val deserialized = read[GetGameDateWiseReportRequest](json)
    deserialized shouldEqual request
  }

  "GameCsvFileGenerateRequest" should "serialize and deserialize correctly" in {
    val request = GameCsvFileGenrateRequest("user123", "2024-03-01", "2024-03-20")
    val json = write(request)
    val deserialized = read[GameCsvFileGenrateRequest](json)
    deserialized shouldEqual request
  }

  "GameFileStatusRequest" should "serialize and deserialize correctly" in {
    val request = GameFileStatusRequest("user123")
    val json = write(request)
    val deserialized = read[GameFileStatusRequest](json)
    deserialized shouldEqual request
  }

  "GetLevelAttemptsRequest" should "serialize and deserialize correctly" in {
    val request = GetLevelAttemptsRequest("user123", 10, 1)
    val json = write(request)
    val deserialized = read[GetLevelAttemptsRequest](json)
    deserialized shouldEqual request
  }

  "UpdateStatusBasedOnStoryRequest" should "serialize and deserialize correctly" in {
    val request = UpdateStatusBasedOnStoryRequest(
      "user123", "level123", 2, "statusJson", 100,
      "levelJson", 1, "127.0.0.1", "Chrome", 1234567890L, "menu"
    )
    val json = write(request)
    val deserialized = read[UpdateStatusBasedOnStoryRequest](json)
    deserialized shouldEqual request
  }

  "GetStoryBasedStatusRequest" should "serialize and deserialize correctly" in {
    val request = GetStoryBasedStatusRequest("user123", "level123", 2)
    val json = write(request)
    val deserialized = read[GetStoryBasedStatusRequest](json)
    deserialized shouldEqual request
  }

  "GetLevelAttemptsJsonDetailsRequest" should "serialize and deserialize correctly" in {
    val request = GetLevelAttemptsJsonDetailsRequest("user123", "level123", "1", "session123")
    val json = write(request)
    val deserialized = read[GetLevelAttemptsJsonDetailsRequest](json)
    deserialized shouldEqual request
  }

  "GetAllUserAttemptListRequest" should "serialize and deserialize correctly" in {
    val request = GetAllUserAttemptListRequest("req123", "auth123", 10, 1, "type1")
    val json = write(request)
    val deserialized = read[GetAllUserAttemptListRequest](json)
    deserialized shouldEqual request
  }

  "GetAllUserListRequest" should "serialize and deserialize correctly" in {
    val request = GetAllUserListRequest("session123")
    val json = write(request)
    val deserialized = read[GetAllUserListRequest](json)
    deserialized shouldEqual request
  }

  "GetAdminListRequest" should "serialize and deserialize correctly" in {
    val request = GetAdminListRequest("session123")
    val json = write(request)
    val deserialized = read[GetAdminListRequest](json)
    deserialized shouldEqual request
  }

  "UpdateLanguageMappingDataRequest" should "serialize and deserialize correctly" in {
    val request = UpdateLanguageMappingDataRequest("group1", "lang123", "jsonData", "session123")
    val json = write(request)
    val deserialized = read[UpdateLanguageMappingDataRequest](json)
    deserialized shouldEqual request
  }

  "GetLanguageMappingDataRequest" should "serialize and deserialize correctly" in {
    val request = GetLanguageMappingDataRequest("group1", "lang123", "session123")
    val json = write(request)
    val deserialized = read[GetLanguageMappingDataRequest](json)
    deserialized shouldEqual request
  }

  "GetLanguageMappingDataWithBaseDataRequest" should "serialize and deserialize correctly" in {
    val request = GetLanguageMappingDataWithBaseDataRequest("group1", "lang123", "session123")
    val json = write(request)
    val deserialized = read[GetLanguageMappingDataWithBaseDataRequest](json)
    deserialized shouldEqual request
  }


  "AddLanguageRequest" should "serialize and deserialize correctly" in {
    val request = AddLanguageRequest("English", "session123")
    val json = write(request)
    val deserialized = read[AddLanguageRequest](json)
    deserialized shouldEqual request
  }

  "UpdateLanguageRequest" should "serialize and deserialize correctly" in {
    val request = UpdateLanguageRequest("lang123", "Spanish", "session123")
    val json = write(request)
    val deserialized = read[UpdateLanguageRequest](json)
    deserialized shouldEqual request
  }

  "GetLanguagesRequest" should "serialize and deserialize correctly" in {
    val request = GetLanguagesRequest("session123")
    val json = write(request)
    val deserialized = read[GetLanguagesRequest](json)
    deserialized shouldEqual request
  }

  "UpdateLanguageBaseDataRequest" should "serialize and deserialize correctly" in {
    val request = UpdateLanguageBaseDataRequest("group1", """{"key": "value"}""", "session123")
    val json = write(request)
    val deserialized = read[UpdateLanguageBaseDataRequest](json)
    deserialized shouldEqual request
  }

  "GetLanguageBaseDataRequest" should "serialize and deserialize correctly" in {
    val request = GetLanguageBaseDataRequest("group1", "session123")
    val json = write(request)
    val deserialized = read[GetLanguageBaseDataRequest](json)
    deserialized shouldEqual request
  }

  "UpdateModuleLanguageMappingRequest" should "serialize and deserialize correctly" in {
    val request = UpdateModuleLanguageMappingRequest("level123", "lang123", """{"key": "value"}""", "session123")
    val json = write(request)
    val deserialized = read[UpdateModuleLanguageMappingRequest](json)
    deserialized shouldEqual request
  }

  "GetModuleLanguageMappingRequest" should "serialize and deserialize correctly" in {
    val request = GetModuleLanguageMappingRequest("level123", "lang123", "session123")
    val json = write(request)
    val deserialized = read[GetModuleLanguageMappingRequest](json)
    deserialized shouldEqual request
  }

  "UpdateLevelsNameLanguageMappingRequest" should "serialize and deserialize correctly" in {
    val request = UpdateLevelsNameLanguageMappingRequest("lang123", """{"key": "value"}""", "session123")
    val json = write(request)
    val deserialized = read[UpdateLevelsNameLanguageMappingRequest](json)
    deserialized shouldEqual request
  }

  "GetLevelsNameLanguageMappingRequest" should "serialize and deserialize correctly" in {
    val request = GetLevelsNameLanguageMappingRequest("lang123", "session123")
    val json = write(request)
    val deserialized = read[GetLevelsNameLanguageMappingRequest](json)
    deserialized shouldEqual request
  }

  "LogsJsonData" should "serialize and deserialize correctly" in {
    val logsData = LogsJsonData(
      page = "homepage",
      action = "click",
      userAgent = "Chrome",
      ipAddress = "127.0.0.1",
      timestamp = "2024-03-20T10:00:00Z"
    )
    val json = write(logsData)
    val deserialized = read[LogsJsonData](json)
    deserialized shouldEqual logsData
  }

  "CaptureLogsRequest" should "serialize and deserialize correctly" in {
    val logsData = LogsJsonData(
      page = "homepage",
      action = "click",
      userAgent = "Chrome",
      ipAddress = "127.0.0.1",
      timestamp = "2024-03-20T10:00:00Z"
    )
    val request = CaptureLogsRequest("req123", logsData)
    val json = write(request)
    val deserialized = read[CaptureLogsRequest](json)
    deserialized shouldEqual request
  }

  "CaptureLogsRequestWrapper" should "serialize and deserialize correctly" in {
    val logsData = LogsJsonData(
      page = "homepage",
      action = "click",
      userAgent = "Chrome",
      ipAddress = "127.0.0.1",
      timestamp = "2024-03-20T10:00:00Z"
    )
    val captureRequest = CaptureLogsRequest("req123", logsData)
    val wrapper = CaptureLogsRequestWrapper("id123", captureRequest)
    val json = write(wrapper)
    val deserialized = read[CaptureLogsRequestWrapper](json)
    deserialized shouldEqual wrapper
  }

  "GetWebLogsRequest" should "serialize and deserialize correctly" in {
    val request = GetWebLogsRequest("req123", "auth123", 10, 1, 100L)
    val json = write(request)
    val deserialized = read[GetWebLogsRequest](json)
    deserialized shouldEqual request
  }

  "LogsData" should "serialize and deserialize correctly" in {
    val logsData = LogsJsonData(
      page = "homepage",
      action = "click",
      userAgent = "Chrome",
      ipAddress = "127.0.0.1",
      timestamp = "2024-03-20T10:00:00Z"
    )
    val data = LogsData(logsData, 1234567890L)
    val json = write(data)
    val deserialized = read[LogsData](json)
    deserialized shouldEqual data
  }

  "FetchAnalyticsRequest" should "serialize and deserialize correctly" in {
    val request = FetchAnalyticsRequest("id123")
    val json = write(request)
    val deserialized = read[FetchAnalyticsRequest](json)
    deserialized shouldEqual request
  }

  "FetchFilterAnalyticsRequest" should "serialize and deserialize correctly with optional fields" in {
    val request = FetchFilterAnalyticsRequest(
      sDate = Some("2024-03-01"),
      eDate = Some("2024-03-20"),
      filterAge = List("10", "11"),
      filterGender = List("M", "F"),
      filterLanguage = List("en", "es"),
      requestType = "type1",
      id = "id123"
    )
    val json = write(request)
    val deserialized = read[FetchFilterAnalyticsRequest](json)
    deserialized shouldEqual request

    val minimalRequest = FetchFilterAnalyticsRequest(
      filterAge = List("10"),
      filterGender = List("M"),
      filterLanguage = List("en"),
      requestType = "type1",
      id = "id123"
    )
    val minimalJson = write(minimalRequest)
    val deserializedMinimal = read[FetchFilterAnalyticsRequest](minimalJson)
    deserializedMinimal shouldEqual minimalRequest
  }

  "FetchFilterUserAttemptAnalyticsRequest" should "serialize and deserialize correctly with optional fields" in {
    val request = FetchFilterUserAttemptAnalyticsRequest(
      sDate = Some("2024-03-01"),
      eDate = Some("2024-03-20"),
      id = "id123"
    )
    val json = write(request)
    val deserialized = read[FetchFilterUserAttemptAnalyticsRequest](json)
    deserialized shouldEqual request

    val minimalRequest = FetchFilterUserAttemptAnalyticsRequest(id = "id123")
    val minimalJson = write(minimalRequest)
    val deserializedMinimal = read[FetchFilterUserAttemptAnalyticsRequest](minimalJson)
    deserializedMinimal shouldEqual minimalRequest
  }

  "AccumulationDate" should "serialize and deserialize correctly" in {
    val date = AccumulationDate("2024", "03", "20")
    val json = write(date)
    val deserialized = read[AccumulationDate](json)
    deserialized shouldEqual date
  }

  "UserAccumulation" should "serialize and deserialize correctly" in {
    val accumulation = UserAccumulation(
      createdAt = 1234567890L,
      userId = "user123",
      ageOfChild = "10",
      genderOfChild = "M",
      language = "en",
      ip = "127.0.0.1",
      deviceInfo = "Chrome"
    )
    val json = write(accumulation)
    val deserialized = read[UserAccumulation](json)
    deserialized shouldEqual accumulation
  }

  "AddToAccumulationRequest" should "handle optional fields correctly" in {
    val userAccumulation = UserAccumulation(
      createdAt = 1234567890L,
      userId = "user123",
      ageOfChild = "10",
      genderOfChild = "M",
      language = "en",
      ip = "127.0.0.1",
      deviceInfo = "Chrome"
    )

    val fullRequest = AddToAccumulationRequest(
      dataType = "type1",
      accumulation = Some(userAccumulation),
      createdAt = 1234567890L
    )
    val json = write(fullRequest)
    val deserialized = read[AddToAccumulationRequest](json)
    deserialized shouldEqual fullRequest

    val minimalRequest = AddToAccumulationRequest(
      dataType = "type1",
      createdAt = 1234567890L
    )
    val minimalJson = write(minimalRequest)
    val deserializedMinimal = read[AddToAccumulationRequest](minimalJson)
    deserializedMinimal shouldEqual minimalRequest
  }

  "AddToAccumulationWrapper" should "serialize and deserialize correctly" in {
    val accumRequest = AddToAccumulationRequest("type1", None, 1234567890L)
    val wrapper = AddToAccumulationWrapper(accumRequest, "id123")
    val json = write(wrapper)
    val deserialized = read[AddToAccumulationWrapper](json)
    deserialized shouldEqual wrapper
  }

  "AddUserAttemptAccumulationRequest" should "serialize and deserialize correctly" in {
    val request = AddUserAttemptAccumulationRequest("type1", "user123", 1234567890L)
    val json = write(request)
    val deserialized = read[AddUserAttemptAccumulationRequest](json)
    deserialized shouldEqual request
  }

  "AddUserAttemptAccumulationWrapper" should "serialize and deserialize correctly" in {
    val request = AddUserAttemptAccumulationRequest("type1", "user123", 1234567890L)
    val wrapper = AddUserAttemptAccumulationWrapper(request, "id123")
    val json = write(wrapper)
    val deserialized = read[AddUserAttemptAccumulationWrapper](json)
    deserialized shouldEqual wrapper
  }

  "UpdateUserDetailsAccumulationRequest" should "serialize and deserialize correctly" in {
    val accumulation = UserAccumulation(1234567890L, "user123", "10", "M", "en", "127.0.0.1", "Chrome")
    val request = UpdateUserDetailsAccumulationRequest("type1", Some(accumulation), 1234567890L)
    val json = write(request)
    val deserialized = read[UpdateUserDetailsAccumulationRequest](json)
    deserialized shouldEqual request
  }

  "SendMailRequest" should "serialize and deserialize correctly" in {
    val request = SendMailRequest(
      "session123", "test@email.com", "John Doe", "1234567890",
      "Test Subject", "Test Content",
      List("to@email.com"), List("cc@email.com"), List("bcc@email.com"),
      "id123"
    )
    val json = write(request)
    val deserialized = read[SendMailRequest](json)
    deserialized shouldEqual request
  }

  "GetAdminLoginRequest" should "serialize and deserialize correctly" in {
    val request = GetAdminLoginRequest("admin", "password")
    val json = write(request)
    val deserialized = read[GetAdminLoginRequest](json)
    deserialized shouldEqual request
  }

  "CreateRoleRequest" should "serialize and deserialize correctly" in {
    val request = CreateRoleRequest("session123", "admin")
    val json = write(request)
    val deserialized = read[CreateRoleRequest](json)
    deserialized shouldEqual request
  }

  "GetRolesRequest" should "serialize and deserialize correctly" in {
    val request = GetRolesRequest("session123", 10, 1)
    val json = write(request)
    val deserialized = read[GetRolesRequest](json)
    deserialized shouldEqual request
  }

  "CreateMemberRequest" should "serialize and deserialize correctly" in {
    val request = CreateMemberRequest("session123", "John Doe", "john@email.com", "password", "admin")
    val json = write(request)
    val deserialized = read[CreateMemberRequest](json)
    deserialized shouldEqual request
  }

  "GetMembersRequest" should "serialize and deserialize correctly" in {
    val request = GetMembersRequest("session123", 10, 1)
    val json = write(request)
    val deserialized = read[GetMembersRequest](json)
    deserialized shouldEqual request
  }

  "CreatePageRequest" should "serialize and deserialize correctly" in {
    val request = CreatePageRequest("session123", "Dashboard", "/dashboard")
    val json = write(request)
    val deserialized = read[CreatePageRequest](json)
    deserialized shouldEqual request
  }

  "GetPagesRequest" should "serialize and deserialize correctly" in {
    val request = GetPagesRequest("session123", 10, 1)
    val json = write(request)
    val deserialized = read[GetPagesRequest](json)
    deserialized shouldEqual request
  }

  "MapUserToRoleRequest" should "serialize and deserialize correctly" in {
    val request = MapUserToRoleRequest("session123", "user123", List("admin", "user"))
    val json = write(request)
    val deserialized = read[MapUserToRoleRequest](json)
    deserialized shouldEqual request
  }

  "MapRoleToPageRequest" should "serialize and deserialize correctly" in {
    val request = MapRoleToPageRequest("session123", "role123", List("page1", "page2"))
    val json = write(request)
    val deserialized = read[MapRoleToPageRequest](json)
    deserialized shouldEqual request
  }

  "GetMapRoleToPageRequest" should "serialize and deserialize correctly" in {
    val request = GetMapRoleToPageRequest("session123", "role123")
    val json = write(request)
    val deserialized = read[GetMapRoleToPageRequest](json)
    deserialized shouldEqual request
  }

  "GetMapUserToRoleRequest" should "serialize and deserialize correctly" in {
    val request = GetMapUserToRoleRequest("session123", "user123")
    val json = write(request)
    val deserialized = read[GetMapUserToRoleRequest](json)
    deserialized shouldEqual request
  }

  "GetRoleAccessRequest" should "serialize and deserialize correctly" in {
    val request = GetRoleAccessRequest("session123", "user123")
    val json = write(request)
    val deserialized = read[GetRoleAccessRequest](json)
    deserialized shouldEqual request
  }

  "UserAttemptDetailsBetweenDateRangeRequest" should "serialize and deserialize correctly" in {
    val request = UserAttemptDeatailsBetweenDateRangeRequest(
      "2024-03-01", "2024-03-20", "user123", "key123", "password123"
    )
    val json = write(request)
    val deserialized = read[UserAttemptDeatailsBetweenDateRangeRequest](json)
    deserialized shouldEqual request
  }

  "EmotionCaptureRequest" should "serialize and deserialize correctly" in {
    val request = EmotionCaptureRequest("user123", "level123", "theme123", "happy", 1)
    val json = write(request)
    val deserialized = read[EmotionCaptureRequest](json)
    deserialized shouldEqual request
  }

  "GetEmotionCaptureListRequest" should "serialize and deserialize correctly" in {
    val request = GetEmotionCaptureListRequest("user123", "level123", "theme123")
    val json = write(request)
    val deserialized = read[GetEmotionCaptureListRequest](json)
    deserialized shouldEqual request
  }

  "FeedbackCaptureRequest" should "serialize and deserialize correctly" in {
    val request = FeedbackCaptureRequest("user123", "level123", "theme123", "feedback1", "activity1", 1)
    val json = write(request)
    val deserialized = read[FeedbackCaptureRequest](json)
    deserialized shouldEqual request
  }

  "GetFeedbackCaptureListRequest" should "serialize and deserialize correctly" in {
    val request = GetfeedbackCaptureListRequest("user123", "level123", "theme123")
    val json = write(request)
    val deserialized = read[GetfeedbackCaptureListRequest](json)
    deserialized shouldEqual request
  }
}