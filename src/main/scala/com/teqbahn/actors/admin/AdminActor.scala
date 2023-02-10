package com.teqbahn.actors.admin

import java.io.File
import java.util.{Date, Random, UUID}
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorContext, ActorRef, PoisonPill, Props, ReceiveTimeout}
import com.teqbahn.bootstrap.StarterMain
import com.teqbahn.caseclasses._
import com.teqbahn.global.{Encryption, GlobalConstants, GlobalMessageConstants, ZiRedisCons}
import com.teqbahn.utils.ZiFunctions
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import com.teqbahn.bootstrap.StarterMain.{redisCommands}
import org.json4s.jackson.Serialization.{read, write}

import java.sql.Timestamp
import scala.collection.immutable.ListMap

object AdminActor {
  private[admin] def props = Props.create(classOf[AdminActor])
}

class AdminActor() extends Actor {

  var actorSystem = this.context.system
  implicit val formats = Serialization.formats(NoTypeHints)
  val random = new Random();

  import scala.collection.JavaConverters._

  override def preStart(): Unit = {
    if (!redisCommands.hexists(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS, "tilliadmin")) {
      val encryptPwd = Encryption.encrypt("admin", "tilliadmin")
      val loginId = ZiFunctions.getId()
      var adminLogin = AdminLogin("Tilli Admin", encryptPwd, "developer", loginId)
      redisCommands.hset(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS, "tilliadmin", write(adminLogin))
    }

    var counter = redisCommands.get(ZiRedisCons.USER_demoUserCounter)
    if (counter != null && !counter.equalsIgnoreCase("null") && !counter.isEmpty) {
      println("counter --> " + counter)
    } else {
      redisCommands.set(ZiRedisCons.USER_demoUserCounter, "0")
    }

    //todo remove this code after migration
    /*var totalSize: Long = redisCommands.llen(ZiRedisCons.ADMIN_userIdsList)
    if(totalSize==0 || totalSize<0)
    {
      println("migration start")
      var userIdsList =redisCommands.smembers(ZiRedisCons.ADMIN_userIdList).asScala.toList
      for (userId <- userIdsList) {
        redisCommands.lpush(ZiRedisCons.ADMIN_userIdsList, userId)

      }

    }*/


  }

  override def postStop(): Unit = {
    ZiFunctions.printNodeInfo(self, "AdminActor got PoisonPill")
  }


  def receive: Receive = {
    case getAdminLoginRequest: GetAdminLoginRequest =>
      if (redisCommands.hexists(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS, getAdminLoginRequest.loginId)) {
        val loginCredentialsStr = redisCommands.hget(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS, getAdminLoginRequest.loginId)
        println(loginCredentialsStr)
        val adminLogin = read[AdminLogin](loginCredentialsStr)
        val decryptPwd = Encryption.decrypt("admin", adminLogin.password)
        if (getAdminLoginRequest.password.equals(decryptPwd)) {
          sender ! GetAdminLoginResponse(GlobalMessageConstants.SUCCESS, adminLogin.name, "1", adminLogin.adminType, adminLogin.loginId)
        } else {
          sender ! GetAdminLoginResponse(GlobalMessageConstants.INVALID_PASSWORD, "", "0", "", "")
        }
      } else {
        sender ! GetAdminLoginResponse(GlobalMessageConstants.INVALID_ACCOUNT, "", "0", "", "")
      }
    case createUserRequest: CreateUserRequest =>
      var userId = ZiFunctions.getId()
      var status = GlobalConstants.ACTIVE
      var userEmailId = createUserRequest.emailId.toLowerCase.trim
      var user = User(userId, userEmailId, createUserRequest.name, createUserRequest.password, createUserRequest.nameOfChild, createUserRequest.ageOfChild, createUserRequest.passcode, status)
      if (createUserRequest.zipcode != null && createUserRequest.zipcode != None) {
        user = user.copy(zipcode = Option(createUserRequest.zipcode.get))
      }
      val createdAt = new Timestamp((new Date).getTime).getTime

      user = user.copy(createdAt = Option(createdAt))

      redisCommands.hset(ZiRedisCons.USER_JSON, userId, write(user))
      var userloginCredentials = UserLoginCredential(userId, status, createUserRequest.password)
      redisCommands.hset(ZiRedisCons.USER_LOGIN_CREDENTIALS, userEmailId, write(userloginCredentials))
      redisCommands.lpush(ZiRedisCons.ADMIN_userIdsList, userId)
      // getUserActorRef() ! InitUsersActorRequest(userId)
      sender ! CreateUserResponse(GlobalMessageConstants.SUCCESS)


     case request: CreateGameUserRequest =>
      var userId = ZiFunctions.getId()
      var status = GlobalConstants.ACTIVE
      var userEmailId = request.emailId.toLowerCase.trim
      val createdAt = new Timestamp((new Date).getTime).getTime
      var response  = GlobalMessageConstants.FAILURE
       if(!redisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, userEmailId)) {
        var user = User(userId = userId, emailId = userEmailId, name = "", password = request.password, nameOfChild = request.nameOfChild, ageOfChild = request.ageOfChild, passcode = request.passcode, status = status, genderOfChild = Option(request.genderOfChild), createdAt = Option(createdAt), schoolName = Option(request.schoolName), className = Option(request.className))
        redisCommands.hset(ZiRedisCons.USER_JSON, userId, write(user))
        var userloginCredentials = UserLoginCredential(userId, status, request.password)
        redisCommands.hset(ZiRedisCons.USER_LOGIN_CREDENTIALS, userEmailId, write(userloginCredentials))
        redisCommands.lpush(ZiRedisCons.ADMIN_userIdsList, userId)
        // getUserActorRef() ! InitUsersActorRequest(userId)
        response  = GlobalMessageConstants.SUCCESS
      }
    
     sender ! CreateGameUserResponse(response)

    case createDemoUserRequest: CreateDemoUserRequest =>

      if (createDemoUserRequest.demoUserId != null && !createDemoUserRequest.demoUserId.isEmpty && redisCommands.hexists(ZiRedisCons.USER_JSON, createDemoUserRequest.demoUserId)) {

        var userDataStr = redisCommands.hget(ZiRedisCons.USER_JSON, createDemoUserRequest.demoUserId)
        val userData = read[User](userDataStr)
        println("get exist demo user :" + createDemoUserRequest.demoUserId)
        var genderOfChild = ""
        if (userData.genderOfChild != null && userData.genderOfChild != None) {
          genderOfChild = userData.genderOfChild.get
        }
        sender ! CreateDemoUserResponse(createDemoUserRequest.sessionId, GlobalMessageConstants.SUCCESS, userData.userId, userData.emailId, userData.name, false, "1", userData.nameOfChild, userData.ageOfChild, genderOfChild)

      }
      else {
        var userId = ZiFunctions.getId()
        var status = GlobalConstants.ACTIVE
        val counter = redisCommands.incr(ZiRedisCons.USER_demoUserCounter)
        var userEmailId = userId
        var name = "user"
        var childName = "demoUserChild" + counter
        var password = randomString(6)
        val createdAt = new Timestamp((new Date).getTime).getTime

        var ipAddress = ""
        if (createDemoUserRequest.ip != None) {
          ipAddress = createDemoUserRequest.ip.get
        }
        var deviceInfo = ""
        if (createDemoUserRequest.deviceInfo != None) {
          deviceInfo = createDemoUserRequest.deviceInfo.get
        }

        var user = User(userId, userEmailId, name, password, childName, "0", "1234", status)
        user = user.copy(ip = Option(ipAddress), deviceInfo = Option(deviceInfo), createdAt = Option(createdAt))

        /* if(createUserRequest.zipcode!=null &&createUserRequest.zipcode!=None )
      {
        user = user.copy(zipcode = Option(createUserRequest.zipcode.get) )
      }*/
        redisCommands.hset(ZiRedisCons.USER_JSON, userId, write(user))
        var userloginCredentials = UserLoginCredential(userId, status, password)
        redisCommands.hset(ZiRedisCons.USER_LOGIN_CREDENTIALS, userEmailId, write(userloginCredentials))
        redisCommands.lpush(ZiRedisCons.ADMIN_userIdsList, userId)

        if (createDemoUserRequest.userType != None) {
          if (createDemoUserRequest.userType.get.equalsIgnoreCase("test")) {
            redisCommands.lpush(ZiRedisCons.ADMIN_testuserIdList, userId)

          }
        }

        StarterMain.accumulatorsActorRef ! AddToAccumulationRequest("User", null, createdAt)

        // getUserActorRef() ! InitUsersActorRequest(userId)

        sender ! CreateDemoUserResponse(createDemoUserRequest.sessionId, GlobalMessageConstants.SUCCESS, userId, userEmailId, name, false, "1", childName, "", "")

      }

    case createDemo2UserRequest: CreateDemo2UserRequest =>
      if (createDemo2UserRequest.demoUserId != null && !createDemo2UserRequest.demoUserId.isEmpty && redisCommands.hexists(ZiRedisCons.USER_JSON, createDemo2UserRequest.demoUserId)) {

        var userDataStr = redisCommands.hget(ZiRedisCons.USER_JSON, createDemo2UserRequest.demoUserId)
        val userData = read[User](userDataStr)
        println("get exist demo user :" + createDemo2UserRequest.demoUserId)
        sender ! CreateDemo2UserResponse(createDemo2UserRequest.sessionId, GlobalMessageConstants.SUCCESS, userData.userId, userData.emailId, userData.name, false, "1", userData.nameOfChild)

      }
      else {
        val createdAt = new Timestamp((new Date).getTime).getTime

        var userId = ZiFunctions.getId()
        var status = GlobalConstants.ACTIVE
        val counter = redisCommands.incr(ZiRedisCons.USER_demoUserCounter)
        //var userEmailId = "tilliDemo2User"+counter+"@teqbahn.com"
        var userEmailId = userId
        var name = "user"
        var childName = "demo2UserChild" + counter
        var password = randomString(6)

        var ipAddress = ""
        if (createDemo2UserRequest.ip != None) {
          ipAddress = createDemo2UserRequest.ip.get
        }
        var deviceInfo = ""
        if (createDemo2UserRequest.deviceInfo != None) {
          deviceInfo = createDemo2UserRequest.deviceInfo.get
        }

        var user = User(userId, userEmailId, name, password, childName, "1", "1234", status)
        user = user.copy(ip = Option(ipAddress), deviceInfo = Option(deviceInfo), createdAt = Option(createdAt))

        //        println("Write TO: " + write(user))
        var ageOfChild = "0"
        var genderOfChild = "male"
        if (createDemo2UserRequest.age != null && createDemo2UserRequest.gender != null && createDemo2UserRequest.gender != None) {
          ageOfChild = createDemo2UserRequest.age
          genderOfChild = createDemo2UserRequest.gender
          user = user.copy(ageOfChild = createDemo2UserRequest.age, genderOfChild = Option(createDemo2UserRequest.gender))
        }

        redisCommands.hset(ZiRedisCons.USER_JSON, userId, write(user))
        var userloginCredentials = UserLoginCredential(userId, status, password)
        redisCommands.hset(ZiRedisCons.USER_LOGIN_CREDENTIALS, userEmailId, write(userloginCredentials))
        redisCommands.lpush(ZiRedisCons.ADMIN_userIdsList, userId)
        if (createDemo2UserRequest.userType != None) {
          if (createDemo2UserRequest.userType.get.equalsIgnoreCase("test")) {
            redisCommands.lpush(ZiRedisCons.ADMIN_testuserIdList, userId)

          }
        }
        // getUserActorRef() ! InitUsersActorRequest(userId)
        // println("create new user :"+userId )

        var lang = ""
        if (createDemo2UserRequest.language != None && createDemo2UserRequest.language != null) {
          if (createDemo2UserRequest.language.get != null && !createDemo2UserRequest.language.get.isEmpty)
            lang = createDemo2UserRequest.language.get
        }
        val userAccumulation: UserAccumulation = UserAccumulation(createdAt, userId, ageOfChild, genderOfChild, lang, ipAddress, deviceInfo)
        StarterMain.accumulatorsActorRef ! AddToAccumulationRequest("User", Option(userAccumulation), createdAt)

        sender ! CreateDemo2UserResponse(createDemo2UserRequest.sessionId, GlobalMessageConstants.SUCCESS, userId, userEmailId, name, false, "1", childName)

      }


    case updateUserDetailsRequest: UpdateUserDetailsRequest =>
      var response = GlobalMessageConstants.FAILURE
      if (redisCommands.hexists(ZiRedisCons.USER_JSON, updateUserDetailsRequest.userId)) {
        var userDataStr = redisCommands.hget(ZiRedisCons.USER_JSON, updateUserDetailsRequest.userId)
        val userData = read[User](userDataStr)
        var ageOfChild = userData.ageOfChild
        var genderOfChild = userData.genderOfChild
        if (updateUserDetailsRequest.age != null && !updateUserDetailsRequest.age.isEmpty) {
          ageOfChild = updateUserDetailsRequest.age
        }
        if (updateUserDetailsRequest.gender != null && !updateUserDetailsRequest.gender.isEmpty) {
          genderOfChild = Option(updateUserDetailsRequest.gender)
        }

        var userDataNew = userData.copy(ageOfChild = ageOfChild, genderOfChild = genderOfChild)
        redisCommands.hset(ZiRedisCons.USER_JSON, updateUserDetailsRequest.userId, write(userDataNew))


        val userAccumulation: UserAccumulation = UserAccumulation(userData.createdAt.get, updateUserDetailsRequest.userId, ageOfChild, genderOfChild.get, updateUserDetailsRequest.language, "", "")
        StarterMain.accumulatorsActorRef ! UpdateUserDetailsAccumulationRequest("User", Option(userAccumulation), userData.createdAt.get)


        response = GlobalMessageConstants.SUCCESS
      }


      sender ! UpdateUserDetailsResponse(response)


    case checkEmailIdAlreadyExist: CheckEmailIdAlreadyExistRequest =>
      var reponse = false
      if (redisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, checkEmailIdAlreadyExist.emailId)) {
        reponse = true
      }
      sender ! CheckEmailIdAlreadyExistResponse(reponse)


    case getLoginRequest: GetLoginRequest =>
      if (redisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, getLoginRequest.loginId)) {
        var loginCredentialsStr = redisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, getLoginRequest.loginId)
        val userLoginCredential = read[UserLoginCredential](loginCredentialsStr)
        if (getLoginRequest.password.equals(userLoginCredential.password)) {
          if (userLoginCredential.status.equalsIgnoreCase(GlobalConstants.ACTIVE)) {
            var userId = userLoginCredential.userId
            var userDataStr = redisCommands.hget(ZiRedisCons.USER_JSON, userId)
            val userData = read[User](userDataStr)
            val lastlogin = new Timestamp((new Date).getTime).getTime

            var userDataNew = userData.copy(lastLogin = Option(lastlogin))
            redisCommands.hset(ZiRedisCons.USER_JSON, userId, write(userDataNew))

            sender ! GetLoginResponse(getLoginRequest.sessionId, GlobalMessageConstants.SUCCESS, userData.userId, userData.emailId, userData.name, false, "1", userData.nameOfChild)

          }
        }
        else {
          sender ! GetLoginResponse("", GlobalMessageConstants.INVALID_PASSWORD, "", "", "", false, "0", "")
        }
      }
      else {
        sender ! GetLoginResponse("", GlobalMessageConstants.INVALID_ACCOUNT, "", "", "", false, "0", "")

      }
    case getLogoutRequest: GetLogoutRequest =>
      var userId = getLogoutRequest.userId
      var userDataStr = redisCommands.hget(ZiRedisCons.USER_JSON, userId)
      val userData = read[User](userDataStr)
      val lastLogout = new Timestamp((new Date).getTime).getTime
      var userDataNew = userData.copy(lastLogout = Option(lastLogout))
      redisCommands.hset(ZiRedisCons.USER_JSON, userId, write(userDataNew))


    case sendForgotPasswordRequest: SendForgotPasswordRequest =>
      var response = GlobalMessageConstants.FAILURE
      if (redisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, sendForgotPasswordRequest.email)) {
        //  val userId: String = redisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, sendForgotPasswordRequest.email)
        val userloginCredentialStr = redisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, sendForgotPasswordRequest.email)
        if (userloginCredentialStr != null && !userloginCredentialStr.isEmpty) {
          val userLoginCredential = read[UserLoginCredential](userloginCredentialStr)
          var userId = userLoginCredential.userId
          val date = new Date
          val created_at = new Timestamp(date.getTime).getTime

          var otpId = UUID.randomUUID().toString();
          val otp = (100000 + random.nextInt(900000)).toString()
          var forgotOtpData = ForgotOtp(userId, otpId, sendForgotPasswordRequest.email, otp, created_at)
          redisCommands.hset(ZiRedisCons.USER_FORGOT_PASSWORD_JSON, userId, write(forgotOtpData))


          var userDataStr = redisCommands.hget(ZiRedisCons.USER_JSON, userId)
          val userData = read[User](userDataStr)


          val subContent = "Tilli Forgot Password"
          //      var mailContent = ""

          val url = StarterMain.frontEndPath + "updatepassword/" + userId + "/" + otpId
          //      println("URL-->" + url)
          //      val linkContent = "<br/><br/><a href='" + url + "'>Verify</a><br/><br/>"
          //          var mailContent = "<!DOCTYPE html>\n<html>\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"> \n<link href=\"http://fonts.googleapis.com/css?family=Open+Sans\" rel=\"stylesheet\" type=\"text/css\">\n<body style=\"width: 100%; font-family: 'open sans'; text-align:center;\">\n<div style=\"width: 600px; text-align:center; margin:0 auto\">\n<img src=\""+StarterMain.frontEndPath+"logo.jpg\" style=\"width:300px;height:100px; margin-top:50px; text-align: center; \"/>\n<br /><br /><br />\n<p style=\"text-align:left;font-size: 18px;\">Welcome <b>"+userData.name.toString.capitalize+"!</b></p>\n<br /><br />\n<a href=\""+url+"\" style=\"text-decoration:none;color:#FFF\" ><p style=\"text-align: center; width: 250px; height: 25; background-color: #FF6C57; padding :10px; text-decoration:none;border-radius: 5px;font-size: 18px;\"><b>Click to reset password</b></p></a>\n<br /><br /><br /><br /><br />\n\n<div style=\"width:100%; text-align:center;font-size: 18px;\">\n<span>Need help?</span><br />\n<span>Please email</span><br />\n<span><a href=\"mailto:emily@tilliPlayers.com\" >example@tilliPlayers.com</a></span>\n</div>\n</div>\n</body>\n</html>";
          var mailContent = "<!DOCTYPE html>\n<html>\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"> \n<link href=\"http://fonts.googleapis.com/css?family=Open+Sans\" rel=\"stylesheet\" type=\"text/css\">\n<body style=\"width: 100%; font-family: 'open sans'; text-align:center;\">\n<div style=\"width: 600px; text-align:center; margin:0 auto\">\n<img src=\"" + StarterMain.frontEndPath + "logo.png\" style=\"width:250px;height:250px; margin-top:50px; text-align: center; \"/>\n<br /><br /><br />\n<p style=\"text-align:left;font-size: 18px;\">Welcome <b>" + userData.name.toString.capitalize + "!</b></p>\n<br /><br />\n<a href=\"" + url + "\" style=\"text-decoration:none;e:#FFF\" ><p style=\"text-align: center; width: 250px; height: 25; background-color: #FF6C57; padding :10px; text-decoration:none;border-radius: 5px;font-size: 18px;\"><b>Click to reset password</b></p></a>\n<br /><br /><br /><br /><br />\n\n<div style=\"width:100%; text-align:center;font-size: 18px;\">\n<span>Need help?</span><br />\n<span>Please email</span><br />\n<span><a href=\"mailto:support@tilli.com\" >support@tilli.com</a></span>\n</div>\n</div>\n</body>\n</html>";
          StarterMain.mailActorRef ! SendMailRequest(sendForgotPasswordRequest.sessionId, sendForgotPasswordRequest.email, "User", "", subContent, mailContent, List.empty, List.empty, List.empty, ZiFunctions.getId())


          response = GlobalMessageConstants.SUCCESS
        }
      }
      sender ! SendForgotPasswordResponse(sendForgotPasswordRequest.sessionId, response)


    case updateForgotPassword: UpdateForgotPasswordRequest =>
      var response = GlobalMessageConstants.FAILURE

      var userId = updateForgotPassword.userId
      if (redisCommands.hexists(ZiRedisCons.USER_FORGOT_PASSWORD_JSON, updateForgotPassword.userId)) {
        val forgotPasswordStr = redisCommands.hget(ZiRedisCons.USER_FORGOT_PASSWORD_JSON, updateForgotPassword.userId)
        if (forgotPasswordStr != null && !forgotPasswordStr.isEmpty) {
          val forgotOtpDetails = read[ForgotOtp](forgotPasswordStr)
          if (forgotOtpDetails.id.equalsIgnoreCase(updateForgotPassword.id)) {
            var userEmailId = forgotOtpDetails.email

            if (redisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, userEmailId)) {

              val userloginCredentialStr = redisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, userEmailId)
              if (userloginCredentialStr != null && !userloginCredentialStr.isEmpty) {
                val userLoginCredential = read[UserLoginCredential](userloginCredentialStr)
                var userLoginCredentialNew = userLoginCredential.copy(password = updateForgotPassword.password)
                redisCommands.hset(ZiRedisCons.USER_LOGIN_CREDENTIALS, userEmailId, write(userLoginCredentialNew))

                val userDataStr = redisCommands.hget(ZiRedisCons.USER_JSON, userId)
                val userData = read[User](userDataStr)
                var userDataNew = userData.copy(password = updateForgotPassword.password)

                redisCommands.hset(ZiRedisCons.USER_JSON, userId, write(userDataNew))
                response = GlobalMessageConstants.SUCCESS

              }


            }

          }
        }
      }
      sender ! UpdateForgotPasswordResponse(updateForgotPassword.sessionId, response)


    case getAllUserListRequest: GetAllUserListRequest =>

      var userMap: Map[String, ShortUserInfo] = redisCommands.hgetall(ZiRedisCons.USER_JSON).asScala.toMap.map(data => {
        var userData = read[User](data._2)
        var shortUserInfo = ShortUserInfo(userData.userId, userData.emailId, userData.name, userData.nameOfChild, userData.ageOfChild, userData.status, userData.lastLogin, userData.lastLogin, userData.genderOfChild, userData.createdAt)
        (data._1, shortUserInfo)
      })

      sender ! GetAllUserListResponse(getAllUserListRequest.sessionId, userMap)

    case getAdminListRequest: GetAdminListRequest =>
      var userMap: Map[String, ShortAdminInfo] = redisCommands.hgetall(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS).asScala.toMap.map(data => {
        var userData = read[User](data._2)
        var shortUserInfo = ShortAdminInfo(userData.userId, userData.emailId, userData.name, userData.status, userData.createdAt)
        (data._1, shortUserInfo)
      })

      sender ! GetAdminListResponse(getAdminListRequest.sessionId, userMap)


    case addGameLevelRequest: AddGameLevelRequest =>
      var levelId = ZiFunctions.getId()
      var gameLevel = GameLevel(levelId, addGameLevelRequest.name, addGameLevelRequest.image, addGameLevelRequest.color, addGameLevelRequest.sortOrder)
      redisCommands.hset(ZiRedisCons.LEVEL_JSON, levelId, write(gameLevel))
      sender ! AddGameLevelResponse(GlobalMessageConstants.SUCCESS)


    case updateGameLevelRequest: UpdateGameLevelRequest =>
      if (redisCommands.hexists(ZiRedisCons.LEVEL_JSON, updateGameLevelRequest.levelId)) {
        var levelDataStr = redisCommands.hget(ZiRedisCons.LEVEL_JSON, updateGameLevelRequest.levelId)
        val levelData: GameLevel = read[GameLevel](levelDataStr)
        var levelName = levelData.name
        var levelColor = levelData.color
        var levelImage = levelData.image
        var levelsortOrder = levelData.sortOrder
        if (updateGameLevelRequest.name != null && !updateGameLevelRequest.name.isEmpty) {
          levelName = updateGameLevelRequest.name
        }
        if (updateGameLevelRequest.color != null && !updateGameLevelRequest.color.isEmpty) {
          levelColor = updateGameLevelRequest.color
        }
        if (updateGameLevelRequest.image != null) {
          levelImage = updateGameLevelRequest.image
        }
        if (updateGameLevelRequest.sortOrder != null) {
          levelsortOrder = updateGameLevelRequest.sortOrder
        }
        var levelDataNew = levelData.copy(name = levelName, image = levelImage, color = levelColor, sortOrder = levelsortOrder)
        redisCommands.hset(ZiRedisCons.LEVEL_JSON, updateGameLevelRequest.levelId, write(levelDataNew))
        sender ! UpdateGameLevelResponse(GlobalMessageConstants.SUCCESS)

      }
      else
        sender ! UpdateGameLevelResponse(GlobalMessageConstants.INVALID)

    case getGameLevelsRequest: GetGameLevelsRequest =>
      var gameLevelMap: Map[String, GameLevel] = Map.empty
      if (getGameLevelsRequest.levelId.isEmpty) {
        gameLevelMap = redisCommands.hgetall(ZiRedisCons.LEVEL_JSON).asScala.toMap.map((data) => {
          (data._1, read[GameLevel](data._2))
        })
      } else {
        val levelId = getGameLevelsRequest.levelId
        gameLevelMap += (levelId -> read[GameLevel](redisCommands.hget(ZiRedisCons.LEVEL_JSON, levelId).toString))
      }
      sender ! GetGameLevelsResponse(gameLevelMap)

    case deleteGameLevelsRequest: DeleteGameLevelsRequest =>
      redisCommands.hdel(ZiRedisCons.LEVEL_JSON, deleteGameLevelsRequest.levelId)

    case deleteThemesRequest: DeleteThemesRequest =>
      redisCommands.hdel(ZiRedisCons.THEME_JSON, deleteThemesRequest.themeId)

    case updateThemeContentRequest: UpdateThemeContentRequest =>
      redisCommands.hset(ZiRedisCons.THEME_CONTENT_JSON, updateThemeContentRequest.themeId, updateThemeContentRequest.data)
      sender ! UpdateThemeContentResponse(GlobalMessageConstants.SUCCESS)

    case getThemeContentRequest: GetThemeContentRequest =>
      var themeContent = redisCommands.hget(ZiRedisCons.THEME_CONTENT_JSON, getThemeContentRequest.themeId)
      sender ! GetThemeContentResponse(themeContent)

    case addThemeRequest: AddThemeRequest =>
      var themeId = ZiFunctions.getId()
      var themeData = Theme(themeId, addThemeRequest.name, addThemeRequest.image, Option(addThemeRequest.themeType),addThemeRequest.gameFile)
      redisCommands.hset(ZiRedisCons.THEME_JSON, themeId, write(themeData))
      sender ! AddThemeResponse(GlobalMessageConstants.SUCCESS)


    case updateThemeRequest: UpdateThemeRequest =>
      if (redisCommands.hexists(ZiRedisCons.THEME_JSON, updateThemeRequest.themeId)) {
        var themeDataStr = redisCommands.hget(ZiRedisCons.THEME_JSON, updateThemeRequest.themeId)
        val themeData: Theme = read[Theme](themeDataStr)
        var themeName = themeData.name
        var themeImage = themeData.image
        var themeType = themeData.themeType
         var gameFile = themeData.gameFile
        if (updateThemeRequest.name != null && !updateThemeRequest.name.isEmpty) {
          themeName = updateThemeRequest.name
        }

        if (updateThemeRequest.image != null) {
          themeImage = updateThemeRequest.image
        }
        if (updateThemeRequest.themeType != null) {
          themeType = Option(updateThemeRequest.themeType)
        }
        if (updateThemeRequest.gameFile != null) {
          gameFile = updateThemeRequest.gameFile
        }
        var themeDataNew = themeData.copy(name = themeName, image = themeImage, themeType = themeType,gameFile = gameFile)
        redisCommands.hset(ZiRedisCons.THEME_JSON, updateThemeRequest.themeId, write(themeDataNew))
        sender ! UpdateThemeResponse(GlobalMessageConstants.SUCCESS)

      }
      else
        sender ! UpdateGameLevelResponse(GlobalMessageConstants.INVALID)

    case getThemesRequest: GetThemesRequest =>
      var themeMap: Map[String, Theme] = Map.empty
      if (getThemesRequest.themeId.isEmpty) {
        themeMap = redisCommands.hgetall(ZiRedisCons.THEME_JSON).asScala.toMap.map((data) => {
          (data._1, read[Theme](data._2))
        })
      } else {
        val themeId = getThemesRequest.themeId
        themeMap += (themeId -> read[Theme](redisCommands.hget(ZiRedisCons.THEME_JSON, themeId).toString))
      }
      sender ! GetThemesResponse(themeMap)

    case addGameImageRequest: AddGameFileRequest =>
      var fileId = ZiFunctions.getId()
      var themeData = GameFileObject(fileId, addGameImageRequest.title, addGameImageRequest.fileName, addGameImageRequest.fileType)
      redisCommands.hset(ZiRedisCons.FILE_JSON, fileId, write(themeData))
      redisCommands.sadd(ZiRedisCons.FILE_TYPE + "_" + addGameImageRequest.fileType, fileId)
      sender ! AddGameFileResponse(GlobalMessageConstants.SUCCESS)

    case updateGameFileRequest: UpdateGameFileRequest =>
      if (redisCommands.hexists(ZiRedisCons.FILE_JSON, updateGameFileRequest.fileId)) {
        var filDataStr = redisCommands.hget(ZiRedisCons.FILE_JSON, updateGameFileRequest.fileId)
        val gameFileObjectData: GameFileObject = read[GameFileObject](filDataStr)
        var title = gameFileObjectData.title
        var fileName = gameFileObjectData.fileName
        var fileType = gameFileObjectData.fileType
        if (updateGameFileRequest.title != null && !updateGameFileRequest.title.isEmpty) {
          title = updateGameFileRequest.title
        }

        if (updateGameFileRequest.fileType != null && !updateGameFileRequest.fileType.isEmpty) {
          fileType = updateGameFileRequest.fileType
        }

        if (updateGameFileRequest.fileName != null && !updateGameFileRequest.fileName.isEmpty) {
          fileName = updateGameFileRequest.fileName
        }
        var gameFileObjectNew = gameFileObjectData.copy(title = title, fileType = fileType, fileName = fileName)
        redisCommands.hset(ZiRedisCons.FILE_JSON, updateGameFileRequest.fileId, write(gameFileObjectNew))
        sender ! UpdateGameFileResponse(GlobalMessageConstants.SUCCESS)

      }
      else
        sender ! UpdateGameFileResponse(GlobalMessageConstants.INVALID)

    case deleteGameFileSearchListRequest: DeleteGameFileSearchListRequest =>
      if (redisCommands.hexists(ZiRedisCons.FILE_JSON, deleteGameFileSearchListRequest.fileId)) {
        var filDataStr = redisCommands.hget(ZiRedisCons.FILE_JSON, deleteGameFileSearchListRequest.fileId)
        val gameFileObjectData: GameFileObject = read[GameFileObject](filDataStr)
        redisCommands.hdel(ZiRedisCons.FILE_JSON, deleteGameFileSearchListRequest.fileId)
        redisCommands.srem(ZiRedisCons.FILE_TYPE + "_" + gameFileObjectData.fileType, deleteGameFileSearchListRequest.fileId)
        var path = "module/" + gameFileObjectData.fileType + "/" + gameFileObjectData.fileName
        var filepath = StarterMain.fileSystemPath + StarterMain.projectName + "/" + path

        val fileDelete = new File(filepath)
        if (fileDelete.exists()) {
          fileDelete.delete()
        }

        sender ! DeleteGameFileSearchListResponse(GlobalMessageConstants.SUCCESS)
      }
      else
        sender ! DeleteGameFileSearchListResponse(GlobalMessageConstants.INVALID)


    case getGameFilesListRequest: GetGameFilesListRequest =>
      var fileType = getGameFilesListRequest.fileType
      var gameFilesMap: Map[String, GameFileObject] = Map.empty

      if (fileType != null && !fileType.isEmpty) {
        var fileIdList = redisCommands.smembers(ZiRedisCons.FILE_TYPE + "_" + fileType).asScala.toList


        fileIdList.par.map(id => {
          var filDataStr = redisCommands.hget(ZiRedisCons.FILE_JSON, id)
          val gameFileObjectData: GameFileObject = read[GameFileObject](filDataStr)
          gameFilesMap += (id -> gameFileObjectData)
        })

      }
      else {
        gameFilesMap = redisCommands.hgetall(ZiRedisCons.FILE_JSON).asScala.toMap.map((data) => {
          (data._1, read[GameFileObject](data._2))
        })
      }

      sender ! GetGameFilesListResponse(gameFilesMap)

    case getGameFileSearchListRequest: GetGameFileSearchListRequest =>
      var fileType = getGameFileSearchListRequest.fileType
      var gameFilesMap: Map[String, GameFileObject] = Map.empty

      if (fileType != null && !fileType.isEmpty) {
        var fileIdList = redisCommands.smembers(ZiRedisCons.FILE_TYPE + "_" + fileType).asScala.toList

        import scala.util.control.Breaks._
        breakable {
          for (fileId <- fileIdList) {
            if (gameFilesMap.size < getGameFileSearchListRequest.limit.toInt) {
              var filDataStr = redisCommands.hget(ZiRedisCons.FILE_JSON, fileId)
              val gameFileObjectData: GameFileObject = read[GameFileObject](filDataStr)

              if (gameFileObjectData.title.toLowerCase.indexOf(getGameFileSearchListRequest.searchString.toLowerCase()) == 0) {
                gameFilesMap += (fileId -> gameFileObjectData)
              }
            } else {
              break
            }
          }

        }

      }
      sender ! GetGameFileSearchListResponse(gameFilesMap)

    case updateLevelMappingRequest: UpdateLevelMappingRequest =>
      val stagesData = updateLevelMappingRequest.stagesData;
      //      val stagesJsonData:List[StageJson] = read[List[StageJson]](stagesData)
      redisCommands.hset(ZiRedisCons.LEVEL_MAPPING_JSON, updateLevelMappingRequest.levelId, stagesData)

      sender ! UpdateLevelMappingResponse(GlobalMessageConstants.SUCCESS)

    case getLevelMappingDataRequest: GetLevelMappingDataRequest =>
      var response = redisCommands.hget(ZiRedisCons.LEVEL_MAPPING_JSON, getLevelMappingDataRequest.levelId)
      sender ! GetLevelMappingDataResponse(response)

    case updateUserGameStatusRequest: UpdateUserGameStatusRequest =>
      if (redisCommands.hexists(ZiRedisCons.USER_GAME_STATUS_JSON, updateUserGameStatusRequest.userId)) {
        var filDataStr = redisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, updateUserGameStatusRequest.userId)
        val userGameStatus: UserGameStatus = read[UserGameStatus](filDataStr)
        var points = userGameStatus.points
        var feelingTool = userGameStatus.feelingTool
        var level = userGameStatus.level
        if (updateUserGameStatusRequest.points != 0 && updateUserGameStatusRequest.points > 0) {
          points = updateUserGameStatusRequest.points
        }
        if (updateUserGameStatusRequest.feelingTool != 0 && updateUserGameStatusRequest.feelingTool > 0) {
          feelingTool = updateUserGameStatusRequest.feelingTool
        }
        if (updateUserGameStatusRequest.level != 0 && updateUserGameStatusRequest.level > 0) {
          level = updateUserGameStatusRequest.level
        }

        var userGameStatusNew = userGameStatus.copy(points = points, feelingTool = feelingTool, level = level)
        redisCommands.hset(ZiRedisCons.USER_GAME_STATUS_JSON, updateUserGameStatusRequest.userId, write(userGameStatusNew))
        sender ! UpdateUserGameStatusResponse(GlobalMessageConstants.SUCCESS)
      }
      else {
        var userGameStatus = UserGameStatus(updateUserGameStatusRequest.points, updateUserGameStatusRequest.feelingTool, updateUserGameStatusRequest.level)
        redisCommands.hset(ZiRedisCons.USER_GAME_STATUS_JSON, updateUserGameStatusRequest.userId, write(userGameStatus))
        sender ! UpdateUserGameStatusResponse(GlobalMessageConstants.SUCCESS)
      }
    case getUserGameStatusRequest: GetUserGameStatusRequest =>
      var response = redisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, getUserGameStatusRequest.userId)
      sender ! GetUserGameStatusResponse(response)

    case getLevelAttemptCount: GetLevelAttemptCountRequest =>
      var attemptCount = 1
      if (redisCommands.hexists(ZiRedisCons.USER_GAME_ATTEMPT + "_" + getLevelAttemptCount.userId, getLevelAttemptCount.levelId)) {
        var existAttemptCount = redisCommands.hget(ZiRedisCons.USER_GAME_ATTEMPT + "_" + getLevelAttemptCount.userId, getLevelAttemptCount.levelId).toInt
        attemptCount = existAttemptCount + 1
      }
      sender ! GetLevelAttemptCountResponse(attemptCount)


    case updateLevelAttemptRequest: UpdateLevelAttemptRequest =>

      var userPoint = 0
      var totalPoint = 0
      var existLevelpoint = 0
      var userGameStatus: UserGameStatus = null
      var response = GlobalMessageConstants.FAILURE

      if (updateLevelAttemptRequest.userId != null) {
        if (redisCommands.hexists(ZiRedisCons.USER_GAME_STATUS_JSON, updateLevelAttemptRequest.userId)) {
          var filDataStr = redisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, updateLevelAttemptRequest.userId)
          userGameStatus = read[UserGameStatus](filDataStr)
          userPoint = userGameStatus.points
        }

        var attemptCount = updateLevelAttemptRequest.attemptCount


        var existAttemptCount = attemptCount - 1
        if (redisCommands.hexists(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + updateLevelAttemptRequest.userId + "_" + updateLevelAttemptRequest.levelId, existAttemptCount.toString)) {
          var attemptJsonStr = redisCommands.hget(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + updateLevelAttemptRequest.userId + "_" + updateLevelAttemptRequest.levelId, existAttemptCount.toString)
          val levelAttempt: LevelAttempt = read[LevelAttempt](attemptJsonStr)
          existLevelpoint = levelAttempt.levelPoint

        }

         /*date wise*/
          var dateString=updateLevelAttemptRequest.dateString

          var attemptKey=updateLevelAttemptRequest.userId + "_" + updateLevelAttemptRequest.levelId+"_"+attemptCount.toString

          var jsonDataKeyExists=ZiRedisCons.USER_DATE_WISE_EXISTS + "_" + updateLevelAttemptRequest.userId+"_"+dateString

          var dateWiseAttemtData=ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST+"_"+updateLevelAttemptRequest.userId+"_"+dateString
          redisCommands.lpush(dateWiseAttemtData, attemptKey)
           
          if(redisCommands.exists(jsonDataKeyExists) == 0)
           {                
             var dateWiseKey=ZiRedisCons.USER_DATE_WISE_ATTEMPT_LIST+"_"+dateString
             redisCommands.lpush(dateWiseKey, updateLevelAttemptRequest.userId)
             redisCommands.hset(jsonDataKeyExists,updateLevelAttemptRequest.userId,dateString)
           }
         
          /*date wise*/
          

        /*     attemptCount = existAttemptCount + 1
      }*/
        redisCommands.hset(ZiRedisCons.USER_GAME_ATTEMPT + "_" + updateLevelAttemptRequest.userId, updateLevelAttemptRequest.levelId, attemptCount.toString)


        if (attemptCount == 1) {
          totalPoint = userPoint + updateLevelAttemptRequest.levelPoints
        }
        else {
          totalPoint = userPoint - existLevelpoint + updateLevelAttemptRequest.levelPoints

        }


        if (userGameStatus == null) {
          userGameStatus = UserGameStatus(totalPoint, 0, updateLevelAttemptRequest.levelNo)
          redisCommands.hset(ZiRedisCons.USER_GAME_STATUS_JSON, updateLevelAttemptRequest.userId, write(userGameStatus))
        }
        else {
          var userGameStatusNew = userGameStatus.copy(points = totalPoint, level = updateLevelAttemptRequest.levelNo)
          redisCommands.hset(ZiRedisCons.USER_GAME_STATUS_JSON, updateLevelAttemptRequest.userId, write(userGameStatusNew))
        }

        response = GlobalMessageConstants.SUCCESS
      }

      sender ! UpdateLevelAttemptResponse(response)

    case getAllUserAttemptListRequest: GetAllUserAttemptListRequest =>

      var resultData: ListMap[String, Any] = ListMap.empty
      var totalSize: Long = 0
      if (getAllUserAttemptListRequest.auth.equalsIgnoreCase(GlobalMessageConstants.AUTH_TEXT)) {
        var filterkey = ZiRedisCons.USER_ALL_GAME_ATTEMPT_LIST
        if (getAllUserAttemptListRequest.actoinType != null && !getAllUserAttemptListRequest.actoinType.isEmpty) {
          filterkey = ZiRedisCons.USER_TYPE_BASED_ALL_GAME_ATTEMPT_LIST + "_" + getAllUserAttemptListRequest.actoinType.toLowerCase()
        }

        val fromIndex = (getAllUserAttemptListRequest.noOfPage - 1) * getAllUserAttemptListRequest.pageLimit;
        totalSize = redisCommands.llen(filterkey)
        if (totalSize > 0) {
          if (totalSize > fromIndex) {
            val lisOfIds = redisCommands.lrange(filterkey, fromIndex, Math.min((getAllUserAttemptListRequest.noOfPage * getAllUserAttemptListRequest.pageLimit), totalSize) - 1).asScala.toList
            for (idStr <- lisOfIds) {

              var idArr = idStr.split("_")
              if (idArr.length > 2) {
                var userId = idArr(0)
                var levelId = idArr(1)
                var attemptCount = idArr(2)

                if (redisCommands.hexists(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + userId + "_" + levelId, attemptCount)) {
                  var attemptJsonStr = redisCommands.hget(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + userId + "_" + levelId, attemptCount)
                  val levelAttempt: LevelAttempt = read[LevelAttempt](attemptJsonStr)
                  var dataMap: Map[String, Any] = Map.empty
                  dataMap += ("dockerId" -> levelAttempt.levelPoint)
                  dataMap += ("ip" -> levelAttempt.ip)
                  dataMap += ("createdAt" -> levelAttempt.createdAt)
                  dataMap += ("deviceInfo" -> levelAttempt.deviceInfo)
                  dataMap += ("userTime" -> levelAttempt.userTime)
                  dataMap += ("landingFrom" -> levelAttempt.landingFrom)
                  resultData += (idStr -> dataMap)

                }

              }

            }
          }
        }
      }
      sender() ! GetAllUserAttemptListResponse(resultData, totalSize)


    case getLevelAttemptsRequest: GetLevelAttemptsRequest =>

      var attemptMap: Map[String, LevelAttemptObject] = Map.empty
      var totalSize: Long = 0

      var filterkey = ZiRedisCons.USER_GAME_ATTEMPT_LIST + getLevelAttemptsRequest.userId
      val fromIndex = (getLevelAttemptsRequest.noOfPage - 1) * getLevelAttemptsRequest.pageLimit;
      totalSize = redisCommands.llen(filterkey)
      if (totalSize > 0) {
        if (totalSize > fromIndex) {
          val lisOfIds = redisCommands.lrange(filterkey, fromIndex, Math.min((getLevelAttemptsRequest.noOfPage * getLevelAttemptsRequest.pageLimit), totalSize) - 1).asScala.toList
          for (idStr <- lisOfIds) {

            var idArr = idStr.split("_")
            if (idArr.length > 2) {
              var userId = idArr(0)
              var levelId = idArr(1)
              var attemptCount = idArr(2)

              if (redisCommands.hexists(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + userId + "_" + levelId, attemptCount)) {
                var attemptJsonStr = redisCommands.hget(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + userId + "_" + levelId, attemptCount)
                val levelAttempt: LevelAttempt = read[LevelAttempt](attemptJsonStr)
                var levelName = ""
                if (redisCommands.hexists(ZiRedisCons.LEVEL_JSON, levelId)) {

                  var levelDataStr = redisCommands.hget(ZiRedisCons.LEVEL_JSON, levelId)
                  val levelData: GameLevel = read[GameLevel](levelDataStr)
                  levelName = levelData.name
                }

                var levelAttemptObject = LevelAttemptObject(levelId, levelName, attemptCount, levelAttempt.levelPoint, levelAttempt.createdAt)
                attemptMap += (idStr -> levelAttemptObject)

              }

            }

          }
        }
      }


      sender ! GetLevelAttemptsRequestResponse(attemptMap, totalSize)

    case updateStatusBasedOnStory: UpdateStatusBasedOnStoryRequest =>

      redisCommands.hset(ZiRedisCons.USER_GAME_STORY_STATUS + "_" + updateStatusBasedOnStory.userId + "_" + updateStatusBasedOnStory.levelId, updateStatusBasedOnStory.attemptCount.toString, updateStatusBasedOnStory.statusJson)
      val createdAt = new Timestamp((new Date).getTime).getTime

      if (!redisCommands.hexists(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + updateStatusBasedOnStory.userId + "_" + updateStatusBasedOnStory.levelId, updateStatusBasedOnStory.attemptCount.toString)) {
        var keyStr = updateStatusBasedOnStory.userId + "_" + updateStatusBasedOnStory.levelId + "_" + updateStatusBasedOnStory.attemptCount.toString
        redisCommands.lpush(ZiRedisCons.USER_ALL_GAME_ATTEMPT_LIST, keyStr)
        redisCommands.lpush(ZiRedisCons.USER_GAME_ATTEMPT_LIST + updateStatusBasedOnStory.userId, keyStr)
        if (!updateStatusBasedOnStory.landingFrom.isEmpty) {

          redisCommands.lpush(ZiRedisCons.USER_TYPE_BASED_ALL_GAME_ATTEMPT_LIST + "_" + updateStatusBasedOnStory.landingFrom.toLowerCase(), keyStr)

        }
        StarterMain.accumulatorsActorRef ! AddUserAttemptAccumulationRequest("userAttempt", updateStatusBasedOnStory.userId, createdAt)

      }
      var levelAttempt = LevelAttempt(updateStatusBasedOnStory.leveljson, updateStatusBasedOnStory.levelPoints, Option(createdAt), Option(updateStatusBasedOnStory.ip), Option(updateStatusBasedOnStory.deviceInfo), Option(updateStatusBasedOnStory.userTime), Option(updateStatusBasedOnStory.landingFrom))
      redisCommands.hset(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + updateStatusBasedOnStory.userId + "_" + updateStatusBasedOnStory.levelId, updateStatusBasedOnStory.attemptCount.toString, write(levelAttempt))


      sender ! UpdateStatusBasedOnStoryResponse(GlobalMessageConstants.SUCCESS)


    case getStoryBasedStatusRequest: GetStoryBasedStatusRequest =>
      var response = ""
      if (redisCommands.hexists(ZiRedisCons.USER_GAME_STORY_STATUS + "_" + getStoryBasedStatusRequest.userId + "_" + getStoryBasedStatusRequest.levelId, getStoryBasedStatusRequest.attemptCount.toString)) {
        response = redisCommands.hget(ZiRedisCons.USER_GAME_STORY_STATUS + "_" + getStoryBasedStatusRequest.userId + "_" + getStoryBasedStatusRequest.levelId, getStoryBasedStatusRequest.attemptCount.toString)

      }
      sender ! GetStoryBasedStatusResponse(response)


    case getLevelAttemptsJsonDetailsRequest: GetLevelAttemptsJsonDetailsRequest =>
      var attemptJsonStr = redisCommands.hget(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + getLevelAttemptsJsonDetailsRequest.userId + "_" + getLevelAttemptsJsonDetailsRequest.levelId, getLevelAttemptsJsonDetailsRequest.attamptNo)
      sender ! GetLevelAttemptsJsonDetailsResponse(attemptJsonStr)

    case addLanguageRequest: AddLanguageRequest =>
      if (!addLanguageRequest.languageName.isEmpty) {
        var languageId = ZiFunctions.getId()
        redisCommands.hset(ZiRedisCons.LANGUAGE_DATA, languageId, addLanguageRequest.languageName)
      }
      sender ! AddLanguageResponse(GlobalMessageConstants.SUCCESS)

    case updateLanguageRequest: UpdateLanguageRequest =>
      if (!updateLanguageRequest.languageName.isEmpty) {
        if (redisCommands.hexists(ZiRedisCons.LANGUAGE_DATA, updateLanguageRequest.languageId)) {
          redisCommands.hset(ZiRedisCons.LANGUAGE_DATA, updateLanguageRequest.languageId, updateLanguageRequest.languageName)

        }
      }
      sender ! UpdateLanguageResponse(GlobalMessageConstants.SUCCESS)

    case getLanguagesRequest: GetLanguagesRequest =>
      var laganuageData: Map[String, String] = redisCommands.hgetall(ZiRedisCons.LANGUAGE_DATA).asScala.toMap
      sender ! GetLanguagesResponse(write(laganuageData))


    case updateLanguageBaseDataRequest: UpdateLanguageBaseDataRequest =>

      //      println("hset "+ZiRedisCons.LANGUAGE_BASE_DATA+" "+updateLanguageBaseDataRequest.grouptype+" "+updateLanguageBaseDataRequest.jsonData)
      redisCommands.hset(ZiRedisCons.LANGUAGE_BASE_DATA, updateLanguageBaseDataRequest.grouptype, updateLanguageBaseDataRequest.jsonData)
      sender ! UpdateLanguageBaseDataResponse(GlobalMessageConstants.SUCCESS)


    case getLanguageBaseDataRequest: GetLanguageBaseDataRequest =>
      var response = ""
      if (!getLanguageBaseDataRequest.grouptype.isEmpty) {
        response = redisCommands.hget(ZiRedisCons.LANGUAGE_BASE_DATA, getLanguageBaseDataRequest.grouptype)
      }
      sender ! GetLanguageBaseDataResponse(response)

    case updateLanguageMappingDataRequest: UpdateLanguageMappingDataRequest =>

      //      println("hset "+ZiRedisCons.LANGUAGE_MAPPING_JSON+"_"+updateLanguageMappingDataRequest.grouptype+" "+updateLanguageMappingDataRequest.languageId+" "+updateLanguageMappingDataRequest.jsonData)


      redisCommands.hset(ZiRedisCons.LANGUAGE_MAPPING_JSON + "_" + updateLanguageMappingDataRequest.grouptype, updateLanguageMappingDataRequest.languageId, updateLanguageMappingDataRequest.jsonData)
      sender ! UpdateLanguageMappingDataResponse(GlobalMessageConstants.SUCCESS)

    case getLanguageMappingDataRequest: GetLanguageMappingDataRequest =>
      var response = ""
      if (!getLanguageMappingDataRequest.grouptype.isEmpty && !getLanguageMappingDataRequest.languageId.isEmpty) {
        response = redisCommands.hget(ZiRedisCons.LANGUAGE_MAPPING_JSON + "_" + getLanguageMappingDataRequest.grouptype, getLanguageMappingDataRequest.languageId)
      }
      sender ! GetLanguageMappingDataResponse(response)

    case getLanguageMappingDataWithBaseDataRequest: GetLanguageMappingDataWithBaseDataRequest =>
      var dataMap: Map[String, String] = Map.empty

      if (!getLanguageMappingDataWithBaseDataRequest.grouptype.isEmpty) {
        var baseData = redisCommands.hget(ZiRedisCons.LANGUAGE_BASE_DATA, getLanguageMappingDataWithBaseDataRequest.grouptype)
        dataMap += ("baseData" -> baseData)

        if (!getLanguageMappingDataWithBaseDataRequest.languageId.isEmpty) {
          var mappingData = redisCommands.hget(ZiRedisCons.LANGUAGE_MAPPING_JSON + "_" + getLanguageMappingDataWithBaseDataRequest.grouptype, getLanguageMappingDataWithBaseDataRequest.languageId)
          dataMap += ("mappingData" -> mappingData)

        }


      }
      sender ! GetLanguageMappingDataWithBaseDataResponse(dataMap)

    case updateModuleLanguageMappingRequest: UpdateModuleLanguageMappingRequest =>
      //      println("hset "+ZiRedisCons.MODULE_LANGUAGE_MAPPING_JSON+"_"+updateModuleLanguageMappingRequest.levelId+" "+updateModuleLanguageMappingRequest.languageId+" "+updateModuleLanguageMappingRequest.jsonData)

      redisCommands.hset(ZiRedisCons.MODULE_LANGUAGE_MAPPING_JSON + "_" + updateModuleLanguageMappingRequest.levelId, updateModuleLanguageMappingRequest.languageId, updateModuleLanguageMappingRequest.jsonData)
      sender ! UpdateModuleLanguageMappingResponse(GlobalMessageConstants.SUCCESS)

    case getModuleLanguageMappingRequest: GetModuleLanguageMappingRequest =>
      var response = ""
      if (getModuleLanguageMappingRequest.levelId != null && getModuleLanguageMappingRequest.languageId != null) {
        if (!getModuleLanguageMappingRequest.levelId.isEmpty && !getModuleLanguageMappingRequest.languageId.isEmpty) {
          response = redisCommands.hget(ZiRedisCons.MODULE_LANGUAGE_MAPPING_JSON + "_" + getModuleLanguageMappingRequest.levelId, getModuleLanguageMappingRequest.languageId)
        }
      }
      sender ! GetModuleLanguageMappingResponse(response)


    case updateLevelsNameLanguageMappingRequest: UpdateLevelsNameLanguageMappingRequest =>
      //      println("hset "+ZiRedisCons.LEVEL_NAME_LANGUAGE_MAPPING_JSON+" "+updateLevelsNameLanguageMappingRequest.languageId+" "+updateLevelsNameLanguageMappingRequest.jsonData)

      redisCommands.hset(ZiRedisCons.LEVEL_NAME_LANGUAGE_MAPPING_JSON, updateLevelsNameLanguageMappingRequest.languageId, updateLevelsNameLanguageMappingRequest.jsonData)
      sender ! UpdateLevelsNameLanguageMappingResponse(GlobalMessageConstants.SUCCESS)

    case getLevelsNameLanguageMappingRequest: GetLevelsNameLanguageMappingRequest =>
      var response = ""
      if (!getLevelsNameLanguageMappingRequest.languageId.isEmpty && !getLevelsNameLanguageMappingRequest.languageId.isEmpty) {
        response = redisCommands.hget(ZiRedisCons.LEVEL_NAME_LANGUAGE_MAPPING_JSON, getLevelsNameLanguageMappingRequest.languageId)
      }
      sender ! GetLevelsNameLanguageMappingResponse(response)

    case createRoleRequest: CreateRoleRequest => {
      val roleStr = createRoleRequest.role.toLowerCase.trim
      if (!redisCommands.hexists(ZiRedisCons.ADMIN_ROLE, roleStr)) {
        val roleId = ZiFunctions.getId()
        val status = GlobalConstants.ACTIVE
        val role = Role(roleId, createRoleRequest.role, status, ZiFunctions.getCreatedAt())
        redisCommands.hset(ZiRedisCons.ADMIN_ROLE_JSON, roleId, write(role))
        redisCommands.hset(ZiRedisCons.ADMIN_ROLE, roleStr, write(role))
        redisCommands.lpush(ZiRedisCons.ADMIN_ROLE_ID_LIST, roleId)
        sender ! CreateRoleResponse(GlobalMessageConstants.SUCCESS)
      } else {
        sender ! CreateRoleResponse(GlobalMessageConstants.FAILURE)
      }
    }
    case getRolesRequest: GetRolesRequest => {
      var resultMap: ListMap[String, Role] = ListMap.empty
      var totalSize: Long = redisCommands.llen(ZiRedisCons.ADMIN_ROLE_ID_LIST)
      if (totalSize > 0) {
        val fromIndex = (getRolesRequest.noOfPage - 1) * getRolesRequest.pageLimit;
        if (totalSize > fromIndex) {
          val endIndex = Math.min((getRolesRequest.noOfPage * getRolesRequest.pageLimit), totalSize) - 1
          val lisOfIds = redisCommands.lrange(ZiRedisCons.ADMIN_ROLE_ID_LIST, fromIndex, endIndex).asScala.toList
          for (roleId <- lisOfIds.distinct) {
            var roleDetailsStr = redisCommands.hget(ZiRedisCons.ADMIN_ROLE_JSON, roleId)
            val roleData = read[Role](roleDetailsStr)
            resultMap += (roleId -> roleData)
          }
        }
      }
      sender ! GetRolesResponse(resultMap, totalSize)
    }
    case createMemberRequest: CreateMemberRequest => {
      val email = createMemberRequest.email.toLowerCase.trim
      if (!redisCommands.hexists(ZiRedisCons.ADMIN_MEMBER, email)) {
        val memberId = ZiFunctions.getId()
        val status = GlobalConstants.ACTIVE
        val encryptPwd = Encryption.encrypt("admin", createMemberRequest.password)
        val name = createMemberRequest.name
        val member = Member(memberId, name, email, encryptPwd, createMemberRequest.createdBy, status, ZiFunctions.getCreatedAt())
        redisCommands.hset(ZiRedisCons.ADMIN_MEMBER_JSON, memberId, write(member))
        redisCommands.hset(ZiRedisCons.ADMIN_MEMBER, email, write(member))
        redisCommands.lpush(ZiRedisCons.ADMIN_MEMBER_ID_LIST, memberId)

        var adminLogin = AdminLogin(name, encryptPwd, "member", memberId)
        redisCommands.hset(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS, email, write(adminLogin))
        sender ! CreateMemberResponse(GlobalMessageConstants.SUCCESS)
      } else {
        sender ! CreateMemberResponse(GlobalMessageConstants.FAILURE)
      }
    }
    case getMembersRequest: GetMembersRequest => {
      var resultMap: ListMap[String, Member] = ListMap.empty
      var totalSize: Long = redisCommands.llen(ZiRedisCons.ADMIN_MEMBER_ID_LIST)
      if (totalSize > 0) {
        val fromIndex = (getMembersRequest.noOfPage - 1) * getMembersRequest.pageLimit;
        if (totalSize > fromIndex) {
          val endIndex = Math.min((getMembersRequest.noOfPage * getMembersRequest.pageLimit), totalSize) - 1
          val lisOfIds = redisCommands.lrange(ZiRedisCons.ADMIN_MEMBER_ID_LIST, fromIndex, endIndex).asScala.toList
          for (memberId <- lisOfIds.distinct) {
            var memberDetailsStr = redisCommands.hget(ZiRedisCons.ADMIN_MEMBER_JSON, memberId)
            val memberData = read[Member](memberDetailsStr)
            resultMap += (memberId -> memberData)
          }
        }
      }
      sender ! GetMembersResponse(resultMap, totalSize)
    }
    case createPageRequest: CreatePageRequest => {
      val title = createPageRequest.title.toLowerCase.trim
      if (!redisCommands.hexists(ZiRedisCons.ADMIN_PAGE, title)) {
        val pageId = ZiFunctions.getId()
        val status = GlobalConstants.ACTIVE
        val page = Page(pageId, createPageRequest.title, createPageRequest.route, status, ZiFunctions.getCreatedAt())
        redisCommands.hset(ZiRedisCons.ADMIN_PAGE_JSON, pageId, write(page))
        redisCommands.hset(ZiRedisCons.ADMIN_PAGE, title, write(page))
        redisCommands.lpush(ZiRedisCons.ADMIN_PAGE_ID_LIST, pageId)
        sender ! CreatePageResponse(GlobalMessageConstants.SUCCESS)
      } else {
        sender ! CreatePageResponse(GlobalMessageConstants.FAILURE)
      }
    }
    case getPagesRequest: GetPagesRequest => {
      var resultMap: ListMap[String, Page] = ListMap.empty
      var totalSize: Long = redisCommands.llen(ZiRedisCons.ADMIN_PAGE_ID_LIST)
      if (totalSize > 0) {
        val fromIndex = (getPagesRequest.noOfPage - 1) * getPagesRequest.pageLimit;
        if (totalSize > fromIndex) {
          val endIndex = Math.min((getPagesRequest.noOfPage * getPagesRequest.pageLimit), totalSize) - 1
          val lisOfIds = redisCommands.lrange(ZiRedisCons.ADMIN_PAGE_ID_LIST, fromIndex, endIndex).asScala.toList
          for (pageId <- lisOfIds.distinct) {
            var pageDetailsStr = redisCommands.hget(ZiRedisCons.ADMIN_PAGE_JSON, pageId)
            val pageData = read[Page](pageDetailsStr)
            resultMap += (pageId -> pageData)
          }
        }
      }
      sender ! GetPagesResponse(resultMap, totalSize)
    }
    case mapUserToRoleRequest: MapUserToRoleRequest => {
      val userId = mapUserToRoleRequest.userId
      var totalSize: Long = redisCommands.llen(ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId)
      val lisOfIds = redisCommands.lrange(ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId, 0, totalSize).asScala.toList

      for (roleId <- mapUserToRoleRequest.roles) {
        var exist = false;
        for (existRoleId <- lisOfIds.distinct) {
          if (existRoleId == roleId) {
            exist = true
          }
        }
        if (!exist) {
          redisCommands.lpush(ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId, roleId)
        }
      }

      for (existRoleId <- lisOfIds.distinct) {
        var removed = true;
        for (roleId <- mapUserToRoleRequest.roles) {
          if (existRoleId == roleId) {
            removed = false
          }
        }
        if (removed) {
          redisCommands.lrem(ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId, 0, existRoleId)
        }
      }
      sender ! MapUserToRoleResponse(GlobalMessageConstants.SUCCESS)
    }
    case mapRoleToPageRequest: MapRoleToPageRequest => {
      val roleId = mapRoleToPageRequest.roleId
      var totalSize: Long = redisCommands.llen(ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId)
      val lisOfIds = redisCommands.lrange(ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId, 0, totalSize).asScala.toList

      for (pageId <- mapRoleToPageRequest.pages) {
        var exist = false;
        for (existPageId <- lisOfIds.distinct) {
          if (existPageId == pageId) {
            exist = true
          }
        }
        if (!exist) {
          redisCommands.lpush(ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId, pageId)
        }
      }
      for (existPageId <- lisOfIds.distinct) {
        var removed = true;
        for (pageId <- mapRoleToPageRequest.pages) {
          if (existPageId == pageId) {
            removed = false
          }
        }
        if (removed) {
          redisCommands.lrem(ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId, 0, existPageId)
        }
      }

      sender ! MapRoleToPageResponse(GlobalMessageConstants.SUCCESS)
    }

    case getMapUserToRoleRequest: GetMapUserToRoleRequest => {
      val userId = getMapUserToRoleRequest.userId
      var totalSize: Long = redisCommands.llen(ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId)
      val lisOfIds = redisCommands.lrange(ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId, 0, totalSize).asScala.toList
      sender ! GetMapUserToRoleResponse(lisOfIds)
    }
    case getMapRoleToPageRequest: GetMapRoleToPageRequest => {
      val roleId = getMapRoleToPageRequest.roleId
      var totalSize: Long = redisCommands.llen(ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId)
      val lisOfIds = redisCommands.lrange(ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId, 0, totalSize).asScala.toList

      sender ! GetMapRoleToPageResponse(lisOfIds)
    }

    case getRoleAccessRequest: GetRoleAccessRequest => {
      var resultMap: ListMap[String, Page] = ListMap.empty
      val userId = getRoleAccessRequest.userId
      var totalSize: Long = redisCommands.llen(ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId)
      val lisOfIds = redisCommands.lrange(ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId, 0, totalSize).asScala.toList

      for (roleId <- lisOfIds.distinct) {
        var totalSizeRole: Long = redisCommands.llen(ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId)
        var lisOfIdsPage = redisCommands.lrange(ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId, 0, totalSizeRole).asScala.toList

        for (pageId <- lisOfIdsPage.distinct) {
          var pageDetailsStr = redisCommands.hget(ZiRedisCons.ADMIN_PAGE_JSON, pageId)
          val pageData = read[Page](pageDetailsStr)
          resultMap += (pageId -> pageData)
        }
      }
      sender ! GetRoleAccessResponse(resultMap)
    }
    case t: String =>
      if (t != null) {
        if (t.equalsIgnoreCase("test")) {
          sender ! "Test message successful!"
        }
        else {
          sender ! "Test message arrived but was not Test!"
        }
      }

    case ReceiveTimeout =>  context.stop(self)
  }
  
  def randomString(len: Int): String = {
    val rand = new scala.util.Random(System.nanoTime)
    val sb = new StringBuilder(len)
    val ab = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    for (i <- 0 until len) {
      sb.append(ab(rand.nextInt(ab.length)))
    }
    sb.toString
  }

   


}



