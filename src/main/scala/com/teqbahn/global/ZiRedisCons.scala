package com.teqbahn.global
object ZiRedisCons {

  val DB = "Tilli"
  val SEPARATOR = "::"
  val ADMIN = "Admin"
  val USERS = "Users"
  val GAME = "Game"
  val LANGUAGE = "Language"
  val LOGS = "Logs"

  //For Admin Actor :
  val ADMIN_SEPARATOR = DB + SEPARATOR + ADMIN + SEPARATOR;
  val ADMIN_userIdList = ADMIN_SEPARATOR + "userIdList"; // Sets[String] // Tilli::Admin::userIdList, Sets[String]
  val ADMIN_userIdsList = ADMIN_SEPARATOR + "userIdsList"; // Sets[String] // Tilli::Admin::userIdList, Sets[String]
  val ADMIN_adminMap = ADMIN_SEPARATOR + "adminMap"; // Map [string, string] // Tilli::Admin::adminMap, support@mynap.in, {support@mynap.in, Welcome@123#, admin, admin, null}
  val ADMIN_userCredentialsMap = ADMIN_SEPARATOR + "userCredentialsMap"; // Map [string, string] // Tilli::Admin::userCredentialsMap, support@mynap.in, {support@mynap.in, Welcome@123#, admin, admin, null}
  val ADMIN_userIdMap = ADMIN_SEPARATOR + "userIdMap"; // Map [string, string] // Tilli::Admin::adminMap, support@mynap.in, uuid
  val ADMIN_testuserIdList = ADMIN_SEPARATOR + "testUserIdList"; // Sets[String] // Tilli::Admin::userIdList, Sets[String]
  val ADMIN_LOGIN_CREDENTIALS = ADMIN_SEPARATOR + "adminLoginCredentials";

  val ADMIN_ROLE = ADMIN_SEPARATOR + "adminRole";
  val ADMIN_ROLE_JSON = ADMIN_SEPARATOR + "adminRoleJson";
  val ADMIN_ROLE_ID_LIST = ADMIN_SEPARATOR + "adminRoleList";
  val ADMIN_MEMBER = ADMIN_SEPARATOR + "adminMember";
  val ADMIN_MEMBER_JSON = ADMIN_SEPARATOR + "adminMemberJson";
  val ADMIN_MEMBER_ID_LIST = ADMIN_SEPARATOR + "adminMemberList";
  val ADMIN_PAGE = ADMIN_SEPARATOR + "adminPage";
  val ADMIN_PAGE_JSON = ADMIN_SEPARATOR + "adminPageJson";
  val ADMIN_PAGE_ID_LIST = ADMIN_SEPARATOR + "adminPageList";
  val ADMIN_MAP_USER_TO_ROLE = ADMIN_SEPARATOR + "adminMapUserToRole_";
  val ADMIN_MAP_ROLE_TO_PAGE = ADMIN_SEPARATOR + "adminMapRoleToPage_";

  //For User Actor :
  val USER_SEPARATOR = DB + SEPARATOR + USERS + SEPARATOR; // Tilli::Users::
  val USER_JSON = USER_SEPARATOR + "userJson"; // Map[string,String] // Tilli::Users::userJson, userId,userjson
  val USER_LOGIN_CREDENTIALS = USER_SEPARATOR + "userLoginCredentials";  // Map[string,String] // Tilli::Users::userLoginCredentials, userId,userLoginjson
  val USER_GAME_STATUS_JSON = USER_SEPARATOR + "gameStatus"; // Map[string,String] // Tilli::Users::userJson, userId,userjson
  val USER_FORGOT_PASSWORD_JSON = USER_SEPARATOR + "userForgotPasswordJson";  // Map[string,String] // Tilli::Users::userLoginCredentials, userId,userLoginjson

  val USER_GAME_ATTEMPT_JSON = USER_SEPARATOR + "gameAttemptJson";
  val USER_GAME_ATTEMPT = USER_SEPARATOR + "gameAttempt";
  val USER_GAME_STORY_STATUS = USER_SEPARATOR + "gameStoryStatus";
  val USER_demoUserCounter = USER_SEPARATOR + "demoUserCounter";
  val USER_GAME_ATTEMPT_LIST = USER_SEPARATOR + "UserAttemptList" + SEPARATOR;
  val USER_ALL_GAME_ATTEMPT_LIST = USER_SEPARATOR + "allUserAttemptList";
  val USER_TYPE_BASED_ALL_GAME_ATTEMPT_LIST = USER_SEPARATOR + "typeBasedAllUserAttemptList";
  val USER_DATE_WISE_ATTEMPT_LIST = USER_SEPARATOR +"dateWiseAttemptList";
  val USER_DATE_WISE_ATTEMPT_DATA_LIST = USER_SEPARATOR + "dateWiseAttemptDataList";
  val USER_DATE_WISE_EXISTS = USER_SEPARATOR + "dateWiseAttemptExists";
  val USER_EXCEL_SHEET_STATUS = USER_SEPARATOR + "excelSheetStatus";



  val USER_SIGNUP_OTP = USER_SEPARATOR + "signUpOtp";  // Map[string,String] // Tilli::Users::signUpOtp, userId,signUpOtpjson
  val USER_FORGOT_OTP = USER_SEPARATOR + "forgotOtp"; // Map[string,String] // Tilli::Users::forgotOtp, userId,forgotOtpjson
  val USER_NOTIFICATIONS = USER_SEPARATOR + "notifications";
  val USER_SMDETAILS = USER_SEPARATOR + "smDetails";


  val GAME_SEPARATOR = DB + SEPARATOR + GAME + SEPARATOR; // Tilli::Game::
  val LEVEL_JSON = GAME_SEPARATOR + "levelJson"; // Map[string,String] // Tilli::Game::levelJson,
  val THEME_JSON = GAME_SEPARATOR + "themeJson"; // Map[string,String] // Tilli::Game::themeJson,
  val THEME_CONTENT_JSON = GAME_SEPARATOR + "themeContentJson";
  val FILE_JSON = GAME_SEPARATOR + "fileJson"; // Map[string,String] // Tilli::Game::fileJson,
  val FILE_TYPE = GAME_SEPARATOR + "fileType"; // Map[string,String] // Tilli::Game::fileType,
  val LEVEL_MAPPING_JSON = GAME_SEPARATOR + "levelMappingJson"; // Map[string,String] // Tilli::Game::levelMappingJson,

  val LANGUAGE_SEPARATOR = DB + SEPARATOR + LANGUAGE + SEPARATOR; // Tilli::Language::
  val LANGUAGE_DATA = LANGUAGE_SEPARATOR + "languageData"; // Tilli::Game::languageData,
  val LANGUAGE_BASE_DATA = LANGUAGE_SEPARATOR + "languageBaseData"; // Tilli::Game::languageData,
  val LANGUAGE_MAPPING_JSON = LANGUAGE_SEPARATOR + "languageMappingJson"; // Tilli::Game::languageMappingJson,

  val MODULE_LANGUAGE_MAPPING_JSON = LANGUAGE_SEPARATOR + "moduleLanguageMappingJson"; // Tilli::Game::moduleLanguageMappingJson,
  val LEVEL_NAME_LANGUAGE_MAPPING_JSON = LANGUAGE_SEPARATOR + "lavelNameLanguageMappingJson"; // Tilli::Game::moduleLanguageMappingJson,

  //For logs Actor :
  val LOGS_SEPARATOR = DB + SEPARATOR + LOGS + SEPARATOR;
  val LOGS_webLog = LOGS_SEPARATOR + "webLog"
  val LOGS_webLogList = LOGS_SEPARATOR + "webLogList"
  val LOGS_webLogBasedOnDate = LOGS_SEPARATOR + "webLogBasedOnDate"



  // Accumulators
  val ACCUMULATOR = "Accumulators"
  val ACCUMULATOR_SEPARATOR = DB + SEPARATOR + ACCUMULATOR + SEPARATOR; // Tilli::Accumulators::

  val ACCUMULATOR_DayUserCounter = ACCUMULATOR_SEPARATOR + "dayUserCounter" + SEPARATOR; // Tilli::Accumulators::dayUserCounter::
  val ACCUMULATOR_MonthUserCounter = ACCUMULATOR_SEPARATOR + "monthUserCounter" + SEPARATOR; // Tilli::Accumulators::monthUserCounter::
  val ACCUMULATOR_YearUserCounter = ACCUMULATOR_SEPARATOR + "yearUserCounter" + SEPARATOR; // Tilli::Accumulators::yearUserCounter::

  val ACCUMULATOR_AgeUserCounter = ACCUMULATOR_SEPARATOR + "ageUserCounter" + SEPARATOR; // Tilli::Accumulators::ageUserCounter::
  val ACCUMULATOR_LanguageUserCounter = ACCUMULATOR_SEPARATOR + "languageUserCounter" + SEPARATOR; // Tilli::Accumulators::languageUserCounter::
  val ACCUMULATOR_GenderUserCounter = ACCUMULATOR_SEPARATOR + "genderUserCounter" + SEPARATOR; // Tilli::Accumulators::genderUserCounter::

  val ACCUMULATOR_FILTER_SEPARATOR = DB + SEPARATOR + ACCUMULATOR + SEPARATOR + "filter" + SEPARATOR; // Tilli::Accumulators::filter::

  val ACCUMULATOR_DATA_AGE = DB + SEPARATOR + ACCUMULATOR + SEPARATOR + "data" + SEPARATOR + "age"; // Tilli::Accumulators::data::age
  val ACCUMULATOR_DATA_LANGUAGE = DB + SEPARATOR + ACCUMULATOR + SEPARATOR + "data" + SEPARATOR + "language"; // Tilli::Accumulators::data::language
  val ACCUMULATOR_DATA_GENDER = DB + SEPARATOR + ACCUMULATOR + SEPARATOR + "data" + SEPARATOR + "gender"; // Tilli::Accumulators::data::gender


  val ACCUMULATOR_DayUserAttemptCounter = ACCUMULATOR_SEPARATOR + "dayUserAttemptCounter" + SEPARATOR; // Tilli::Accumulators::dayUserAttemptCounter::
  val ACCUMULATOR_MonthUserAttemptCounter = ACCUMULATOR_SEPARATOR + "monthUserAttemptCounter" + SEPARATOR; // Tilli::Accumulators::monthUserAttemptCounter::
  val ACCUMULATOR_YearUserAttemptCounter = ACCUMULATOR_SEPARATOR + "yearUserAttemptCounter" + SEPARATOR; // Tilli::Accumulators::yearUserAttemptCounter::


  val ACCUMULATOR_DayUniqueUserAttemptCounter = ACCUMULATOR_SEPARATOR + "dayUniqueUserAttemptCounter" + SEPARATOR; // Tilli::Accumulators::dayUniqueUserAttemptCounter::
  val ACCUMULATOR_MonthUniqueUserAttemptCounter = ACCUMULATOR_SEPARATOR + "monthUniqueUserAttemptCounter" + SEPARATOR; // Tilli::Accumulators::monthUniqueUserAttemptCounter::
  val ACCUMULATOR_YearUniqueUserAttemptCounter = ACCUMULATOR_SEPARATOR + "yearUniqueUserAttemptCounter" + SEPARATOR; // Tilli::Accumulators::yearUniqueUserAttemptCounter::

  val ACCUMULATOR_DayUniqueUserAttemptSet = ACCUMULATOR_SEPARATOR + "dayUniqueUserAttemptSet" + SEPARATOR; // Tilli::Accumulators::dayUniqueUserAttemptMap::
  val ACCUMULATOR_MonthUniqueUserAttemptSet = ACCUMULATOR_SEPARATOR + "monthUniqueUserAttemptSet" + SEPARATOR; // Tilli::Accumulators::monthUniqueUserAttemptMap::
  val ACCUMULATOR_YearUniqueUserAttemptSet = ACCUMULATOR_SEPARATOR + "yearUniqueUserAttemptSet" + SEPARATOR; // Tilli::Accumulators::yearUniqueUserAttemptMap::


  //For Chat Actor
  val CHAT_CHANNEL_JSON = ADMIN_SEPARATOR + "chatChannelJson"
  val CHAT_MY_CHANNEL_LIST = ADMIN_SEPARATOR + "myChannelList_"
  val CHAT_MY_DIRECT_CHANNEL_LIST = ADMIN_SEPARATOR + "myDirectChannelList_"

  val CHAT_MY_REFFERED_CHANNEL_LIST = ADMIN_SEPARATOR + "myRefferedChannelList_"
  val CHAT_MY_REFFERED_DIRECT_CHANNEL_LIST = ADMIN_SEPARATOR + "myRefferedDirectChannelList_"

  val CHAT_CHANNEL_MESSAGES_ID_LIST = ADMIN_SEPARATOR + "channelMessagesIdList_"
  val CHAT_CHANNEL_MESSAGES = ADMIN_SEPARATOR + "channelMessages_"
  val CHAT_CHANNEL_UNREAD_COUNT = ADMIN_SEPARATOR + "channelUnReadCount_"

  //For client device Registration
  val CHAT_DEVICE_INFO = ADMIN_SEPARATOR + "deviceInfo";
}
