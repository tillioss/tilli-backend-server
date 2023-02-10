package com.teqbahn.caseclasses
import scala.collection.immutable.ListMap

//new
case class FetchAnalyticsResponse(response: String) extends Response

case class FetchFilterAnalyticsResponse(response: String) extends Response

case class FetchFilterUserAttemptAnalyticsResponse(response: String) extends Response

case class CreateUserResponse(response: String) extends Response
case class CreateGameUserResponse(response: String) extends Response

case class CreateDemoUserResponse(sessionId: String, response: String, id: String, email: String, name: String, isFirstLogin: Boolean, responseCode: String, nameOfChild: String, ageOfChild: String, genderOfChild: String) extends Response

case class CreateDemo2UserResponse(sessionId: String, response: String, id: String, email: String, name: String, isFirstLogin: Boolean, responseCode: String, nameOfChild: String) extends Response

case class GetLoginResponse(sessionId: String, response: String, id: String, email: String, name: String, isFirstLogin: Boolean, responseCode: String, nameOfChild: String) extends Response

case class GetLogoutResponse(response: String) extends Response

case class CheckEmailIdAlreadyExistResponse(response: Boolean) extends Response

case class SendForgotPasswordResponse(sessionId: String, responseMessage: String) extends Response

case class UpdateForgotPasswordResponse(sessionId: String, response: String) extends Response

case class AddGameLevelResponse(response: String) extends Response

case class UpdateGameLevelResponse(response: String) extends Response

case class GetGameLevelsResponse(levelsMap: Map[String, GameLevel]) extends Response

case class DeleteGameLevelResponse(response: String) extends Response

case class UpdateUserDetailsResponse(response: String) extends Response

case class AddThemeResponse(response: String) extends Response

case class UpdateThemeResponse(response: String) extends Response

case class GetThemesResponse(themesMap: Map[String, Theme]) extends Response

case class DeleteThemesResponse(response: String) extends Response

case class UpdateThemeContentResponse(response: String) extends Response

case class GetThemeContentResponse(response: String) extends Response


case class AddGameFileResponse(response: String) extends Response

case class UpdateGameFileResponse(response: String) extends Response

case class GetGameFilesListResponse(filesMap: Map[String, GameFileObject]) extends Response

case class GetGameFileSearchListResponse(filesMap: Map[String, GameFileObject]) extends Response

case class UpdateLevelMappingResponse(response: String) extends Response

case class GetLevelMappingDataResponse(response: String) extends Response

case class UpdateUserGameStatusResponse(response: String) extends Response

case class GetUserGameStatusResponse(response: String) extends Response

case class UpdateLevelAttemptResponse(response: String) extends Response

case class GetGameDateWiseResponse(result: Map[String,Any],totalResult: Long) extends Response

case class GameCsvFileGenrateResponse(response: String) extends Response


case class DeleteGameFileSearchListResponse(response: String) extends Response

case class GetLevelAttemptCountResponse(response: Integer) extends Response

case class GetAllUserAttemptListResponse(result: Map[String, Any], totalResult: Long) extends Response

case class LevelAttemptObject(levelId: String, levelName: String, attamptNo: String, levelPoint: Integer, createdAt: Option[Long] = None)


case class GetLevelAttemptsRequestResponse(levelAttemptMap: Map[String, LevelAttemptObject], totalResult: Long) extends Response

case class GetLevelAttemptsJsonDetailsResponse(response: String) extends Response


case class GetAllUserListResponse(sessionId: String, shortUserInfoMap: Map[String, ShortUserInfo]) extends Response

case class GetAdminListResponse(sessionId: String, shortAdminInfoMap: Map[String, ShortAdminInfo]) extends Response

case class UpdateLanguageMappingDataResponse(response: String) extends Response

case class GetLanguageMappingDataResponse(response: String) extends Response

case class GetLanguageMappingDataWithBaseDataResponse(dataMap: Map[String, String]) extends Response

case class AddLanguageResponse(response: String) extends Response

case class UpdateLanguageResponse(response: String) extends Response

case class GetLanguagesResponse(response: String) extends Response

case class UpdateLanguageBaseDataResponse(response: String) extends Response

case class GetLanguageBaseDataResponse(response: String) extends Response


case class UpdateModuleLanguageMappingResponse(response: String) extends Response

case class GetModuleLanguageMappingResponse(response: String) extends Response

case class UpdateLevelsNameLanguageMappingResponse(response: String) extends Response

case class GetLevelsNameLanguageMappingResponse(response: String) extends Response

case class UpdateStatusBasedOnStoryResponse(response: String) extends Response

case class GetStoryBasedStatusResponse(response: String) extends Response

case class CaptureLogsResponse(response: String) extends Response

case class GetWebLogsResponse(response: ListMap[String, LogsData], totalResult: Long) extends Response

case class GetAdminLoginResponse(response: String, name: String, responseCode: String, adminType: String, loginId: String) extends Response

case class CreateRoleResponse(response: String) extends Response

case class GetRolesResponse(result: ListMap[String, Role], totalResult: Long) extends Response

case class CreateMemberResponse(response: String) extends Response

case class GetMembersResponse(result: ListMap[String, Member], totalResult: Long) extends Response

case class CreatePageResponse(response: String) extends Response

case class GetPagesResponse(result: ListMap[String, Page], totalResult: Long) extends Response

case class MapUserToRoleResponse(response: String) extends Response

case class MapRoleToPageResponse(response: String) extends Response

case class GetMapUserToRoleResponse(listOfRoleIds: List[String]) extends Response

case class GetMapRoleToPageResponse(listOfPageIds: List[String]) extends Response

case class GetRoleAccessResponse(result: ListMap[String, Page]) extends Response







