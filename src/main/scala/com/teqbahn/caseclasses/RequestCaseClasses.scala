package com.teqbahn.caseclasses

//new
// dataType -> User, Module, ...
case class AdminLogin(name: String, password: String, adminType: String, loginId: String)

case class CreateUserRequest(emailId: String, password: String, name: String, ageOfChild: String, nameOfChild: String, passcode: String, sessionId: String, zipcode: Option[String] = None) extends Request

case class CreateGameUserRequest(emailId:String, password: String,nameOfChild:String, ageOfChild: String,schoolName: String, className: String,genderOfChild:String,passcode:String,sessionId: String ) extends Request


case class CreateDemoUserRequest(sessionId: String, demoUserId: String, userType: Option[String] = None, ip: Option[String] = None, deviceInfo: Option[String] = None) extends Request

case class CreateDemo2UserRequest(sessionId: String, age: String, gender: String, demoUserId: String, userType: Option[String] = None, ip: Option[String] = None, deviceInfo: Option[String] = None, language: Option[String] = None) extends Request

case class GetLoginRequest(loginId: String, password: String, sessionId: String) extends Request

case class GetLogoutRequest(userId: String, sessionId: String) extends Request

case class InitUsersActorRequest(userId: String) extends Request

case class CheckEmailIdAlreadyExistRequest(emailId: String, sessionId: String) extends Request

case class UpdateUserDetailsRequest(userId: String, age: String, gender: String, language: String) extends Request

case class SendForgotPasswordRequest(sessionId: String, email: String) extends Request

case class UpdateForgotPasswordRequest(sessionId: String, userId: String, id: String, otp: String, password: String) extends Request

case class AddGameLevelRequest(name: String, color: String, sessionId: String, image: GameFileObject, sortOrder: Integer) extends Request

case class UpdateGameLevelRequest(levelId: String, name: String, color: String, image: GameFileObject, sessionId: String, sortOrder: Integer) extends Request

case class GetGameLevelsRequest(levelId: String, sessionId: String) extends Request

case class DeleteGameLevelsRequest(levelId: String, sessionId: String) extends Request

case class AddThemeRequest(name: String, sessionId: String, image: GameFileObject, themeType: String,gameFile: Option[FileObject] = None) extends Request

case class UpdateThemeRequest(themeId: String, name: String, image: GameFileObject, themeType: String, sessionId: String,gameFile: Option[FileObject] = None) extends Request

case class GetThemesRequest(themeId: String, sessionId: String) extends Request

case class DeleteThemesRequest(themeId: String, sessionId: String) extends Request

case class UpdateThemeContentRequest(themeId: String, data: String) extends Request

case class GetThemeContentRequest(themeId: String) extends Request

case class AddGameFileRequest(title: String, fileName: String, fileType: String, sessionId: String) extends Request

case class UpdateGameFileRequest(fileId: String, title: String, fileName: String, fileType: String, sessionId: String) extends Request

case class GetGameFilesListRequest(fileType: String, sessionId: String) extends Request

case class GetGameFileSearchListRequest(fileType: String, searchString: String, limit: String, sessionId: String) extends Request

case class DeleteGameFileSearchListRequest(fileId: String, sessionId: String) extends Request

case class UpdateLevelMappingRequest(levelId: String, stagesData: String, sessionId: String) extends Request

case class GetLevelMappingDataRequest(levelId: String, sessionId: String) extends Request

case class UpdateUserGameStatusRequest(userId: String, points: Integer, feelingTool: Integer, level: Integer, sessionId: String) extends Request

case class GetUserGameStatusRequest(userId: String, sessionId: String) extends Request

case class GetLevelAttemptCountRequest(userId: String, levelId: String) extends Request

case class UpdateLevelAttemptRequest(userId: String, levelId: String, levelPoints: Integer, leveljson: String, levelNo: Integer, sessionId: String, attemptCount: Integer, ip: String, deviceInfo: String, userTime: Long, landingFrom: String,dateString: String) extends Request

case class GetLevelAttemptsRequest(userId: String, pageLimit: Int, noOfPage: Int) extends Request

case class UpdateStatusBasedOnStoryRequest(userId: String, levelId: String, attemptCount: Integer, statusJson: String, levelPoints: Integer, leveljson: String, levelNo: Integer, ip: String, deviceInfo: String, userTime: Long, landingFrom: String) extends Request

case class GetStoryBasedStatusRequest(userId: String, levelId: String, attemptCount: Integer) extends Request

case class GetLevelAttemptsJsonDetailsRequest(userId: String, levelId: String, attamptNo: String, sessionId: String) extends Request

case class GetAllUserAttemptListRequest(reqId: String, auth: String, pageLimit: Int, noOfPage: Int, actoinType: String) extends Request

case class GetAllUserListRequest(sessionId: String) extends Request

case class GetAdminListRequest(sessionId: String) extends Request

case class UpdateLanguageMappingDataRequest(grouptype: String, languageId: String, jsonData: String, sessionId: String) extends Request

case class GetLanguageMappingDataRequest(grouptype: String, languageId: String, sessionId: String) extends Request

case class GetLanguageMappingDataWithBaseDataRequest(grouptype: String, languageId: String, sessionId: String) extends Request

case class AddLanguageRequest(languageName: String, sessionId: String) extends Request

case class UpdateLanguageRequest(languageId: String, languageName: String, sessionId: String) extends Request

case class GetLanguagesRequest(sessionId: String) extends Request

case class UpdateLanguageBaseDataRequest(grouptype: String, jsonData: String, sessionId: String) extends Request

case class GetLanguageBaseDataRequest(grouptype: String, sessionId: String) extends Request

case class UpdateModuleLanguageMappingRequest(levelId: String, languageId: String, jsonData: String, sessionId: String) extends Request

case class GetModuleLanguageMappingRequest(levelId: String, languageId: String, sessionId: String) extends Request


case class UpdateLevelsNameLanguageMappingRequest(languageId: String, jsonData: String, sessionId: String) extends Request

case class GetLevelsNameLanguageMappingRequest(languageId: String, sessionId: String) extends Request


case class LogsJsonData(page: String, action: String, userAgent: String, ipAddress: String, timestamp: String)

case class CaptureLogsRequest(reqId: String, logsJson: LogsJsonData) extends Request

case class CaptureLogsRequestWrapper(id: String, captureLogsRequest: CaptureLogsRequest) extends Request

case class GetWebLogsRequest(reqId: String, auth: String, pageLimit: Int, noOfPage: Int, totalResult: Long) extends Request

case class LogsData(logsJson: LogsJsonData, createdAt: Long)


//new end

case class FetchAnalyticsRequest(id: String) extends Request

case class FetchFilterAnalyticsRequest(sDate: Option[String] = None, eDate: Option[String] = None, filterAge: List[String], filterGender: List[String], filterLanguage: List[String], requestType: String, id: String) extends Request

case class FetchFilterUserAttemptAnalyticsRequest(sDate: Option[String] = None, eDate: Option[String] = None, id: String) extends Request

case class AccumulationDate(year: String, month: String, date: String)

case class UserAccumulation(createdAt: Long, userId: String, ageOfChild: String, genderOfChild: String, language: String, ip: String, deviceInfo: String)

case class AddToAccumulationRequest(dataType: String, accumulation: Option[UserAccumulation] = None, createdAt: Long) extends Request

case class AddToAccumulationWrapper(accumulator: AddToAccumulationRequest, id: String) extends Request

case class AddToFilterAccumulationWrapper(accumulator: AddToAccumulationRequest, id: String, accumulationDate: AccumulationDate) extends Request

case class AddUserAttemptAccumulationRequest(dataType: String, userid: String, createdAt: Long) extends Request

case class AddUserAttemptAccumulationWrapper(accumulator: AddUserAttemptAccumulationRequest, id: String) extends Request

case class UpdateUserDetailsAccumulationRequest(dataType: String, accumulation: Option[UserAccumulation] = None, createdAt: Long) extends Request

case class SendMailRequest(sessionId: String, email: String, fullName: String, mobileNumber: String, subContent: String, mailContent: String, toMailIds: List[String], ccMailIds: List[String], bccMailIds: List[String], id: String ) extends Request

case class GetAdminLoginRequest(loginId: String, password: String) extends Request

case class CreateRoleRequest(sessionId: String, role: String) extends Request

case class GetRolesRequest(sessionId: String, pageLimit: Int, noOfPage: Int) extends Request

case class CreateMemberRequest(sessionId: String, name: String, email: String, password: String, createdBy: String) extends Request

case class GetMembersRequest(sessionId: String, pageLimit: Int, noOfPage: Int) extends Request

case class CreatePageRequest(sessionId: String, title: String, route: String) extends Request

case class GetPagesRequest(sessionId: String, pageLimit: Int, noOfPage: Int) extends Request

case class MapUserToRoleRequest(sessionId: String, userId: String, roles: List[String]) extends Request

case class MapRoleToPageRequest(sessionId: String, roleId: String, pages: List[String]) extends Request

case class GetMapRoleToPageRequest(sessionId: String, roleId: String) extends Request

case class GetMapUserToRoleRequest(sessionId: String, userId: String) extends Request

case class GetRoleAccessRequest(sessionId: String, userId: String) extends Request



