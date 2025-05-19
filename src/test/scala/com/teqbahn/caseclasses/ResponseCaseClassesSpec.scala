package com.teqbahn.caseclasses

import org.json4s._
import org.scalatest.flatspec.AnyFlatSpec
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.ListMap

class ResponseCaseClassesSpec extends AnyFlatSpec with Matchers {
  implicit val formats: Formats = Serialization.formats(NoTypeHints)

  "FetchAnalyticsResponse" should "serialize and deserialize correctly" in {
    val response = FetchAnalyticsResponse("test analytics data")
    val json = write(response)
    val deserialized = read[FetchAnalyticsResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[FetchAnalyticsResponse](invalidJson)
  }

  "CreateDemoUserResponse" should "serialize and deserialize correctly" in {
    val response = CreateDemoUserResponse(
      sessionId = "session123",
      response = "Success",
      id = "user123",
      email = "test@example.com",
      name = "Test User",
      isFirstLogin = true,
      responseCode = "200",
      nameOfChild = "Child Name",
      ageOfChild = "10",
      genderOfChild = "Male"
    )

    val json = write(response)
    val deserialized = read[CreateDemoUserResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[CreateDemoUserResponse](invalidJson)
  }

  "CreateDemo2UserResponse" should "serialize and deserialize correctly" in {
    val response = CreateDemo2UserResponse(
      sessionId = "session123",
      response = "Success",
      id = "user123",
      email = "test@example.com",
      name = "Test User",
      isFirstLogin = true,
      responseCode = "200",
      nameOfChild = "Child Name"
    )
    val json = write(response)
    val deserialized = read[CreateDemo2UserResponse](json)
    deserialized shouldEqual response

    val invalidJson = """{"sessionId": null, "response": null}"""
    an[MappingException] should be thrownBy read[CreateDemo2UserResponse](invalidJson)
  }

  "FetchFilterUserAttemptAnalyticsResponse" should "serialize and deserialize correctly" in {
    val response = FetchFilterUserAttemptAnalyticsResponse("User attempt analytics data")
    val json = write(response)
    val deserialized = read[FetchFilterUserAttemptAnalyticsResponse](json)
    deserialized shouldEqual response
  }

  "GetLoginResponse" should "serialize and deserialize correctly" in {
    val response = GetLoginResponse(
      sessionId = "session123",
      response = "Success",
      id = "user123",
      email = "test@example.com",
      name = "Test User",
      isFirstLogin = true,
      responseCode = "200",
      nameOfChild = "Child Name"
    )

    val json = write(response)
    val deserialized = read[GetLoginResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLoginResponse](invalidJson)
  }

  "GetLogoutResponse" should "serialize and deserialize correctly" in {
    val response = GetLogoutResponse("Successfully logged out")
    val json = write(response)
    val deserialized = read[GetLogoutResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLogoutResponse](invalidJson)
  }

  "CheckEmailIdAlreadyExistResponse" should "serialize and deserialize correctly" in {
    val response = CheckEmailIdAlreadyExistResponse(true)
    val json = write(response)
    val deserialized = read[CheckEmailIdAlreadyExistResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[CheckEmailIdAlreadyExistResponse](invalidJson)
  }

  "SendForgotPasswordResponse" should "serialize and deserialize correctly" in {
    val response = SendForgotPasswordResponse("session123", "Password reset email sent")
    val json = write(response)
    val deserialized = read[SendForgotPasswordResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[SendForgotPasswordResponse](invalidJson)
  }

  "UpdateForgotPasswordResponse" should "serialize and deserialize correctly" in {
    val response = UpdateForgotPasswordResponse("session123", "Password updated successfully")
    val json = write(response)
    val deserialized = read[UpdateForgotPasswordResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateForgotPasswordResponse](invalidJson)
  }

  "AddGameLevelResponse" should "serialize and deserialize correctly" in {
    val response = AddGameLevelResponse("Game level added successfully")
    val json = write(response)
    val deserialized = read[AddGameLevelResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[AddGameLevelResponse](invalidJson)
  }

  "UpdateGameLevelResponse" should "serialize and deserialize correctly" in {
    val response = UpdateGameLevelResponse("Game level updated successfully")
    val json = write(response)
    val deserialized = read[UpdateGameLevelResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateGameLevelResponse](invalidJson)
  }

  "GetGameLevelsResponse" should "serialize and deserialize correctly" in {
    val gameFileObject = GameFileObject("file1", "Test File", "test.png", "image")
    val gameLevel = GameLevel("level1", "Level 1", gameFileObject, "#FF0000", 1)
    val levelsMap = Map("level1" -> gameLevel)
    val response = GetGameLevelsResponse(levelsMap)
    val json = write(response)
    val deserialized = read[GetGameLevelsResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetGameLevelsResponse](invalidJson)
  }

  "DeleteGameLevelResponse" should "serialize and deserialize correctly" in {
    val response = DeleteGameLevelResponse("Game level deleted successfully")
    val json = write(response)
    val deserialized = read[DeleteGameLevelResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[DeleteGameLevelResponse](invalidJson)
  }

  "UpdateUserDetailsResponse" should "serialize and deserialize correctly" in {
    val response = UpdateUserDetailsResponse("User details updated successfully")
    val json = write(response)
    val deserialized = read[UpdateUserDetailsResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateUserDetailsResponse](invalidJson)
  }

  "AddThemeResponse" should "serialize and deserialize correctly" in {
    val response = AddThemeResponse("Theme added successfully")
    val json = write(response)
    val deserialized = read[AddThemeResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[AddThemeResponse](invalidJson)
  }

  "UpdateThemeResponse" should "serialize and deserialize correctly" in {
    val response = UpdateThemeResponse("Theme updated successfully")
    val json = write(response)
    val deserialized = read[UpdateThemeResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateThemeResponse](invalidJson)
  }

  "GetThemesResponse" should "serialize and deserialize correctly" in {
    val gameFileObject = GameFileObject("file1", "Test File", "test.png", "image")
    val theme = Theme("theme1", "Theme 1", gameFileObject)
    val themesMap = Map("theme1" -> theme)
    val response = GetThemesResponse(themesMap)
    val json = write(response)
    val deserialized = read[GetThemesResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetThemesResponse](invalidJson)
  }

  "DeleteThemesResponse" should "serialize and deserialize correctly" in {
    val response = DeleteThemesResponse("Theme deleted successfully")
    val json = write(response)
    val deserialized = read[DeleteThemesResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[DeleteThemesResponse](invalidJson)
  }

  "UpdateThemeContentResponse" should "serialize and deserialize correctly" in {
    val response = UpdateThemeContentResponse("Theme content updated successfully")
    val json = write(response)
    val deserialized = read[UpdateThemeContentResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateThemeContentResponse](invalidJson)
  }

  "GetThemeContentResponse" should "serialize and deserialize correctly" in {
    val themeLayerContent = ThemeLayerContent("layer data")
    val response = GetThemeContentResponse(themeLayerContent)
    val json = write(response)
    val deserialized = read[GetThemeContentResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetThemeContentResponse](invalidJson)
  }

  "AddGameFileResponse" should "serialize and deserialize correctly" in {
    val response = AddGameFileResponse("Game file added successfully")
    val json = write(response)
    val deserialized = read[AddGameFileResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[AddGameFileResponse](invalidJson)
  }

  "UpdateGameFileResponse" should "serialize and deserialize correctly" in {
    val response = UpdateGameFileResponse("Game file updated successfully")
    val json = write(response)
    val deserialized = read[UpdateGameFileResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateGameFileResponse](invalidJson)
  }

  "GetGameFilesListResponse" should "serialize and deserialize correctly" in {
    val gameFile = GameFileObject("file1", "Test File", "test.png", "image")
    val filesMap = Map("file1" -> gameFile)
    val response = GetGameFilesListResponse(filesMap)
    val json = write(response)
    val deserialized = read[GetGameFilesListResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetGameFilesListResponse](invalidJson)
  }

  "GetGameFileSearchListResponse" should "serialize and deserialize correctly" in {
    val gameFile = GameFileObject("file1", "Test File", "test.png", "image")
    val filesMap = Map("file1" -> gameFile)
    val response = GetGameFileSearchListResponse(filesMap)
    val json = write(response)
    val deserialized = read[GetGameFileSearchListResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetGameFileSearchListResponse](invalidJson)
  }

  "UpdateLevelMappingResponse" should "serialize and deserialize correctly" in {
    val response = UpdateLevelMappingResponse("Level mapping updated successfully")
    val json = write(response)
    val deserialized = read[UpdateLevelMappingResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateLevelMappingResponse](invalidJson)
  }

  "GetLevelMappingDataResponse" should "serialize and deserialize correctly" in {
    val response = GetLevelMappingDataResponse("Level mapping data")
    val json = write(response)
    val deserialized = read[GetLevelMappingDataResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLevelMappingDataResponse](invalidJson)
  }

  "UpdateUserGameStatusResponse" should "serialize and deserialize correctly" in {
    val response = UpdateUserGameStatusResponse("Game status updated")
    val json = write(response)
    val deserialized = read[UpdateUserGameStatusResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateUserGameStatusResponse](invalidJson)
  }

  "GetUserGameStatusResponse" should "serialize and deserialize correctly" in {
    val response = GetUserGameStatusResponse("Game status data")
    val json = write(response)
    val deserialized = read[GetUserGameStatusResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetUserGameStatusResponse](invalidJson)
  }

  "UpdateLevelAttemptResponse" should "serialize and deserialize correctly" in {
    val response = UpdateLevelAttemptResponse("Level attempt updated")
    val json = write(response)
    val deserialized = read[UpdateLevelAttemptResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateLevelAttemptResponse](invalidJson)
  }

  "GetGameDateWiseResponse" should "serialize and deserialize correctly" in {
    val result = Map("date" -> "2024-03-20", "count" -> 5)
    val response = GetGameDateWiseResponse(result, 10L)
    val json = write(response)
    val deserialized = read[GetGameDateWiseResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetGameDateWiseResponse](invalidJson)
  }

  "GameCsvFileGenerateResponse" should "serialize and deserialize correctly" in {
    val response = GameCsvFileGenerateResponse("CSV file generated")
    val json = write(response)
    val deserialized = read[GameCsvFileGenerateResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GameCsvFileGenerateResponse](invalidJson)
  }

  "GameFileStatusResponse" should "serialize and deserialize correctly" in {
    val response = GameFileStatusResponse("File status: Ready")
    val json = write(response)
    val deserialized = read[GameFileStatusResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GameFileStatusResponse](invalidJson)
  }

  "DeleteGameFileSearchListResponse" should "serialize and deserialize correctly" in {
    val response = DeleteGameFileSearchListResponse("File deleted successfully")
    val json = write(response)
    val deserialized = read[DeleteGameFileSearchListResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[DeleteGameFileSearchListResponse](invalidJson)
  }

  "GetLevelAttemptCountResponse" should "serialize and deserialize correctly" in {
    val response = GetLevelAttemptCountResponse(5)
    val json = write(response)
    val deserialized = read[GetLevelAttemptCountResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLevelAttemptCountResponse](invalidJson)
  }

  "GetAllUserAttemptListResponse" should "serialize and deserialize correctly" in {
    val result = Map("user1" -> "attempt1", "user2" -> "attempt2")
    val response = GetAllUserAttemptListResponse(result, 2L)
    val json = write(response)
    val deserialized = read[GetAllUserAttemptListResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetAllUserAttemptListResponse](invalidJson)
  }

  "LevelAttemptObject" should "serialize and deserialize correctly" in {
    val attempt = LevelAttemptObject("level1", "Level 1", "1", 100, Some(1234567890L))
    val json = write(attempt)
    val deserialized = read[LevelAttemptObject](json)
    deserialized shouldEqual attempt
  }

  "GetLevelAttemptsRequestResponse" should "serialize and deserialize correctly" in {
    val attempt = LevelAttemptObject("level1", "Level 1", "1", 100, Some(1234567890L))
    val attemptMap = Map("attempt1" -> attempt)
    val response = GetLevelAttemptsRequestResponse(attemptMap, 1L)
    val json = write(response)
    val deserialized = read[GetLevelAttemptsRequestResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLevelAttemptsRequestResponse](invalidJson)
  }

  "GetLevelAttemptsJsonDetailsResponse" should "serialize and deserialize correctly" in {
    val response = GetLevelAttemptsJsonDetailsResponse("Attempt details")
    val json = write(response)
    val deserialized = read[GetLevelAttemptsJsonDetailsResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLevelAttemptsJsonDetailsResponse](invalidJson)
  }

  "GetAllUserListResponse" should "serialize and deserialize correctly" in {
    val userInfo = ShortUserInfo(
      userId = "user1",
      emailId = "user@example.com",
      name = "User One",
      nameOfChild = "Child One",
      ageOfChild = "5-7",
      status = "active",
      lastLogin = Some(1234567890L),
      lastLogout = Some(1234567891L),
      genderOfChild = Some("male"),
      createdAt = Some(1234567889L)
    )
    val userMap = Map("user1" -> userInfo)
    val response = GetAllUserListResponse("session123", userMap)
    val json = write(response)
    val deserialized = read[GetAllUserListResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetAllUserListResponse](invalidJson)
  }

  "GetAdminListResponse" should "serialize and deserialize correctly" in {
    val adminInfo = ShortAdminInfo(
      userId = "admin1",
      emailId = "admin@example.com",
      name = "Admin One",
      status = "active",
      createdAt = Some(1234567890L)
    )
    val adminMap = Map("admin1" -> adminInfo)
    val response = GetAdminListResponse("session123", adminMap)
    val json = write(response)
    val deserialized = read[GetAdminListResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetAdminListResponse](invalidJson)
  }

  "UpdateLanguageMappingDataResponse" should "serialize and deserialize correctly" in {
    val response = UpdateLanguageMappingDataResponse("Language mapping updated")
    val json = write(response)
    val deserialized = read[UpdateLanguageMappingDataResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateLanguageMappingDataResponse](invalidJson)
  }

  "GetLanguageMappingDataResponse" should "serialize and deserialize correctly" in {
    val response = GetLanguageMappingDataResponse("Language mapping data")
    val json = write(response)
    val deserialized = read[GetLanguageMappingDataResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLanguageMappingDataResponse](invalidJson)
  }

  "GetLanguageMappingDataWithBaseDataResponse" should "serialize and deserialize correctly" in {
    val dataMap = Map("key1" -> "value1", "key2" -> "value2")
    val response = GetLanguageMappingDataWithBaseDataResponse(dataMap)
    val json = write(response)
    val deserialized = read[GetLanguageMappingDataWithBaseDataResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLanguageMappingDataWithBaseDataResponse](invalidJson)
  }

  "AddLanguageResponse" should "serialize and deserialize correctly" in {
    val response = AddLanguageResponse("Language added successfully")
    val json = write(response)
    val deserialized = read[AddLanguageResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[AddLanguageResponse](invalidJson)
  }

  "UpdateLanguageResponse" should "serialize and deserialize correctly" in {
    val response = UpdateLanguageResponse("Language updated successfully")
    val json = write(response)
    val deserialized = read[UpdateLanguageResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateLanguageResponse](invalidJson)
  }

  "GetLanguagesResponse" should "serialize and deserialize correctly" in {
    val response = GetLanguagesResponse("Languages list")
    val json = write(response)
    val deserialized = read[GetLanguagesResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLanguagesResponse](invalidJson)
  }

  "UpdateLanguageBaseDataResponse" should "serialize and deserialize correctly" in {
    val response = UpdateLanguageBaseDataResponse("Base data updated")
    val json = write(response)
    val deserialized = read[UpdateLanguageBaseDataResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateLanguageBaseDataResponse](invalidJson)
  }

  "GetLanguageBaseDataResponse" should "serialize and deserialize correctly" in {
    val response = GetLanguageBaseDataResponse("Base language data")
    val json = write(response)
    val deserialized = read[GetLanguageBaseDataResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLanguageBaseDataResponse](invalidJson)
  }

  "UpdateModuleLanguageMappingResponse" should "serialize and deserialize correctly" in {
    val response = UpdateModuleLanguageMappingResponse("Module language mapping updated")
    val json = write(response)
    val deserialized = read[UpdateModuleLanguageMappingResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateModuleLanguageMappingResponse](invalidJson)
  }

  "GetModuleLanguageMappingResponse" should "serialize and deserialize correctly" in {
    val response = GetModuleLanguageMappingResponse("Module language mapping data")
    val json = write(response)
    val deserialized = read[GetModuleLanguageMappingResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetModuleLanguageMappingResponse](invalidJson)
  }

  "UpdateLevelsNameLanguageMappingResponse" should "serialize and deserialize correctly" in {
    val response = UpdateLevelsNameLanguageMappingResponse("Levels name mapping updated")
    val json = write(response)
    val deserialized = read[UpdateLevelsNameLanguageMappingResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateLevelsNameLanguageMappingResponse](invalidJson)
  }

  "GetLevelsNameLanguageMappingResponse" should "serialize and deserialize correctly" in {
    val response = GetLevelsNameLanguageMappingResponse("Levels name mapping data")
    val json = write(response)
    val deserialized = read[GetLevelsNameLanguageMappingResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetLevelsNameLanguageMappingResponse](invalidJson)
  }

  "UpdateStatusBasedOnStoryResponse" should "serialize and deserialize correctly" in {
    val response = UpdateStatusBasedOnStoryResponse("Story status updated")
    val json = write(response)
    val deserialized = read[UpdateStatusBasedOnStoryResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UpdateStatusBasedOnStoryResponse](invalidJson)
  }

  "GetStoryBasedStatusResponse" should "serialize and deserialize correctly" in {
    val response = GetStoryBasedStatusResponse("Story status data")
    val json = write(response)
    val deserialized = read[GetStoryBasedStatusResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetStoryBasedStatusResponse](invalidJson)
  }

  "CaptureLogsResponse" should "serialize and deserialize correctly" in {
    val response = CaptureLogsResponse("Logs captured successfully")
    val json = write(response)
    val deserialized = read[CaptureLogsResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[CaptureLogsResponse](invalidJson)
  }

  "GetWebLogsResponse" should "serialize and deserialize correctly" in {
    val logsJsonData = LogsJsonData(
      page = "homepage",
      action = "view",
      userAgent = "Mozilla/5.0",
      ipAddress = "127.0.0.1",
      timestamp = "2024-03-20T10:00:00Z"
    )
    val logsData = LogsData(
      logsJson = logsJsonData,
      createdAt = 1234567890L
    )
    val responseMap = ListMap[String, LogsData]("log1" -> logsData)
    val response = GetWebLogsResponse(responseMap, 1L)
    val json = write(response)
    val deserialized = read[GetWebLogsResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an [MappingException] should be thrownBy read[GetWebLogsResponse](invalidJson)
  }


  "GetAdminLoginResponse" should "serialize and deserialize correctly" in {
    val response = GetAdminLoginResponse(
      response = "Login successful",
      name = "Admin User",
      responseCode = "200",
      adminType = "super",
      loginId = "admin@example.com"
    )
    val json = write(response)
    val deserialized = read[GetAdminLoginResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetAdminLoginResponse](invalidJson)
  }

  "CreateRoleResponse" should "serialize and deserialize correctly" in {
    val response = CreateRoleResponse("Role created successfully")
    val json = write(response)
    val deserialized = read[CreateRoleResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[CreateRoleResponse](invalidJson)
  }

  "GetRolesResponse" should "serialize and deserialize correctly" in {
    val role = Role("role1", "Admin", "active", 1234567890L)
    val response = GetRolesResponse(ListMap("role1" -> role), 1L)
    val json = write(response)
    val deserialized = read[GetRolesResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetRolesResponse](invalidJson)
  }

  "CreateMemberResponse" should "serialize and deserialize correctly" in {
    val response = CreateMemberResponse("Member created successfully")
    val json = write(response)
    val deserialized = read[CreateMemberResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[CreateMemberResponse](invalidJson)
  }

  "GetMembersResponse" should "serialize and deserialize correctly" in {
    val member = Member("member1", "John Doe", "john@example.com", "password123", "admin1", "active", 1234567890L)
    val response = GetMembersResponse(ListMap("member1" -> member), 1L)
    val json = write(response)
    val deserialized = read[GetMembersResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetMembersResponse](invalidJson)
  }

  "CreatePageResponse" should "serialize and deserialize correctly" in {
    val response = CreatePageResponse("Page created successfully")
    val json = write(response)
    val deserialized = read[CreatePageResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[CreatePageResponse](invalidJson)
  }

  "GetPagesResponse" should "serialize and deserialize correctly" in {
    val page = Page("page1", "Dashboard", "/dashboard", "active", 1234567890L)
    val response = GetPagesResponse(ListMap("page1" -> page), 1L)
    val json = write(response)
    val deserialized = read[GetPagesResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetPagesResponse](invalidJson)
  }

  "MapUserToRoleResponse" should "serialize and deserialize correctly" in {
    val response = MapUserToRoleResponse("User mapped to role successfully")
    val json = write(response)
    val deserialized = read[MapUserToRoleResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[MapUserToRoleResponse](invalidJson)
  }

  "MapRoleToPageResponse" should "serialize and deserialize correctly" in {
    val response = MapRoleToPageResponse("Role mapped to page successfully")
    val json = write(response)
    val deserialized = read[MapRoleToPageResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[MapRoleToPageResponse](invalidJson)
  }

  "GetMapUserToRoleResponse" should "serialize and deserialize correctly" in {
    val response = GetMapUserToRoleResponse(List("role1", "role2"))
    val json = write(response)
    val deserialized = read[GetMapUserToRoleResponse](json)
    deserialized shouldEqual response
  }

  "GetMapRoleToPageResponse" should "serialize and deserialize correctly" in {
    val response = GetMapRoleToPageResponse(List("page1", "page2"))
    val json = write(response)
    val deserialized = read[GetMapRoleToPageResponse](json)
    deserialized shouldEqual response
  }

  "GetRoleAccessResponse" should "serialize and deserialize correctly" in {
    val page = Page("page1", "Dashboard", "/dashboard", "active", 1234567890L)
    val response = GetRoleAccessResponse(ListMap("page1" -> page))
    val json = write(response)
    val deserialized = read[GetRoleAccessResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetRoleAccessResponse](invalidJson)
  }

  "UserAttemptDetailsBetweenDateRangeResponse" should "serialize and deserialize correctly" in {
    val response = UserAttemptDetailsBetweenDateRangeResponse(Map("date" -> "2024-03-20", "attempts" -> 5))
    val json = write(response)
    val deserialized = read[UserAttemptDetailsBetweenDateRangeResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[UserAttemptDetailsBetweenDateRangeResponse](invalidJson)
  }

  "EmotionCaptureResponse" should "serialize and deserialize correctly" in {
    val response = EmotionCaptureResponse("Emotion captured successfully")
    val json = write(response)
    val deserialized = read[EmotionCaptureResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[EmotionCaptureResponse](invalidJson)
  }

  "GetEmotionCaptureListResponse" should "serialize and deserialize correctly" in {
    val response = GetEmotionCaptureListResponse(List("happy", "sad", "neutral"))
    val json = write(response)
    val deserialized = read[GetEmotionCaptureListResponse](json)
    deserialized shouldEqual response
  }

  "FeedbackCapturtResponse" should "serialize and deserialize correctly" in {
    val response = FeedbackCapturtResponse("Feedback captured successfully")
    val json = write(response)
    val deserialized = read[FeedbackCapturtResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[FeedbackCapturtResponse](invalidJson)
  }

  "GetFeedbackCaptureListResponse" should "serialize and deserialize correctly" in {
    val response = GetFeedbackCaptureListResponse(Map("rating" -> 5, "comment" -> "Great experience"))
    val json = write(response)
    val deserialized = read[GetFeedbackCaptureListResponse](json)
    deserialized shouldEqual response

    val invalidJson = "{}"
    an[MappingException] should be thrownBy read[GetFeedbackCaptureListResponse](invalidJson)
  }

}
