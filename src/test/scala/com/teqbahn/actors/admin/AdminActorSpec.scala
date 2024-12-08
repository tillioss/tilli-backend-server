package com.teqbahn.actors.admin

import org.apache.pekko.actor.{ActorSystem, Props, ActorRef}
import org.apache.pekko.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.teqbahn.caseclasses._
import com.teqbahn.global.{ZiRedisCons, GlobalMessageConstants, GlobalConstants}
import com.teqbahn.bootstrap.StarterMain
import org.mockito.MockitoSugar
import io.lettuce.core.api.sync.RedisCommands
import org.mockito.Mockito.{when, verify, times}
import java.sql.Timestamp
import java.util.Date
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{write, read}
import org.json4s.NoTypeHints
import scala.concurrent.duration._
import org.mockito.ArgumentCaptor
import org.scalatest.BeforeAndAfterEach
import scala.collection.JavaConverters._

class AdminActorSpec 
    extends TestKit(ActorSystem("AdminActorSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with MockitoSugar {

  implicit val formats = Serialization.formats(NoTypeHints)
  val mockRedisCommands = mock[RedisCommands[String, String]]
  val mockAccumulatorActor = TestProbe()

  override def beforeAll(): Unit = {
    StarterMain.redisCommands = mockRedisCommands
    StarterMain.accumulatorsActorRef = mockAccumulatorActor.ref
    StarterMain.mailActorRef = TestProbe().ref
    StarterMain.adminSupervisorActorRef = system.actorOf(Props[AdminActor], "admin-supervisor")
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    org.mockito.Mockito.reset(mockRedisCommands)
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "AdminActor" should {
    "handle UpdateUserDetailsRequest successfully" in {
      val userId = "user123"
      val age = "5"
      val gender = "male"
      val request = UpdateUserDetailsRequest(userId, age, gender, "en")

      // Mock existing user
      val existingUser = User(
        userId = userId,
        emailId = "test@example.com",
        name = "Test User",
        password = "password",
        nameOfChild = "Child",
        ageOfChild = "4",
        passcode = "1234",
        status = "active",
        lastLogin = None,
        lastLogout = None,
        zipcode = None,
        genderOfChild = Some("female"),
        createdAt = Some(new Timestamp(new Date().getTime).getTime),
        ip = None,
        deviceInfo = None,
        schoolName = None,
        className = None
      )

      val existingUserJson = write(existingUser)
      val updatedUser = existingUser.copy(
        ageOfChild = age,
        genderOfChild = Some(gender)
      )
      val updatedUserJson = write(updatedUser)

      // Verify the mock setup
      println(s"Existing user JSON: $existingUserJson")
      println(s"Updated user JSON: $updatedUserJson")

      when(mockRedisCommands.hexists(ZiRedisCons.USER_JSON, userId))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(existingUserJson)
      when(mockRedisCommands.hset(ZiRedisCons.USER_JSON, userId, updatedUserJson))
        .thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(10.seconds, UpdateUserDetailsResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle CheckEmailIdAlreadyExistRequest" in {
      val emailId = "test@example.com"
      val request = CheckEmailIdAlreadyExistRequest(sessionId = "session1", emailId = emailId)

      val userCredential = UserLoginCredential(
        userId = "user123",
        password = "password",
        status = "active"
      )
      val userCredentialJson = write(userCredential)

      println(s"User credential JSON: $userCredentialJson")

      // First check if email exists
      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, emailId))
        .thenReturn(true)

      // Then get the credentials
      when(mockRedisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, emailId))
        .thenReturn(userCredentialJson)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, CheckEmailIdAlreadyExistResponse(true))
    }

    "handle GetLoginRequest with valid credentials" in {
      val loginId = "test@example.com"
      val password = "password123"
      val userId = "user123"
      val request = GetLoginRequest(loginId = loginId, password = password, sessionId = "session1")

      val userCredentials = UserLoginCredential(
        userId = userId,
        password = password,
        status = "active"
      )
      val userCredentialsJson = write(userCredentials)

      val userData = User(
        userId = userId,
        emailId = loginId,
        name = "Test User",
        password = password,
        nameOfChild = "Child",
        ageOfChild = "5",
        passcode = "1234",
        status = "active",
        lastLogin = None,
        lastLogout = None,
        zipcode = None,
        genderOfChild = Some("male"),
        createdAt = Some(new Timestamp(new Date().getTime).getTime),
        ip = None,
        deviceInfo = None,
        schoolName = None,
        className = None
      )
      val userDataJson = write(userData)

      println(s"User credentials JSON: $userCredentialsJson")
      println(s"User data JSON: $userDataJson")

      // First check if credentials exist
      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, loginId))
        .thenReturn(true)
      
      // Then get the credentials
      when(mockRedisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, loginId))
        .thenReturn(userCredentialsJson)
      
      // Then get the user data
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(userDataJson)

      // Mock the update of last login
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_JSON),
        org.mockito.ArgumentMatchers.eq(userId),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, GetLoginResponse(
        sessionId = "session1",
        response = GlobalMessageConstants.SUCCESS,
        id = userId,
        email = loginId,
        name = userData.name,
        isFirstLogin = false,
        responseCode = "1",
        nameOfChild = userData.nameOfChild
      ))
    }

    "handle UpdateForgotPasswordRequest" in {
      val userId = "user123"
      val email = "test@example.com"
      val newPassword = "newpassword123"
      val otpId = "otp123"
      val otp = "123456"
      val request = UpdateForgotPasswordRequest("session1", userId, otpId, otp, newPassword)

      val forgotOtp = ForgotOtp(
        userId = userId,
        id = otpId,
        email = email,
        otp = otp,
        createdAt = new Timestamp(new Date().getTime).getTime
      )

      val userCredentials = UserLoginCredential(
        userId = userId,
        password = "oldpassword",
        status = GlobalConstants.ACTIVE
      )

      val userData = User(
        userId = userId,
        emailId = email,
        name = "Test User",
        password = "oldpassword",
        nameOfChild = "Child",
        ageOfChild = "5",
        passcode = "1234",
        status = "active",
        lastLogin = None,
        lastLogout = None,
        zipcode = None,
        genderOfChild = Some("male"),
        createdAt = Some(new Timestamp(new Date().getTime).getTime),
        ip = None,
        deviceInfo = None,
        schoolName = None,
        className = None
      )

      when(mockRedisCommands.hexists(ZiRedisCons.USER_FORGOT_PASSWORD_JSON, userId))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_FORGOT_PASSWORD_JSON, userId))
        .thenReturn(write(forgotOtp))
      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, email))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, email))
        .thenReturn(write(userCredentials))
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(write(userData))

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(UpdateForgotPasswordResponse("session1", GlobalMessageConstants.SUCCESS))
    }

    "handle GetLogoutRequest successfully" in {
      val userId = "user123"
      val request = GetLogoutRequest(userId = userId, sessionId = "session1")
      val timestamp = new Timestamp(new Date().getTime).getTime

      val userData = User(
        userId = userId,
        emailId = "test@example.com",
        name = "Test User",
        password = "password123",
        nameOfChild = "Child",
        ageOfChild = "5",
        passcode = "1234",
        status = "active",
        lastLogin = Some(timestamp),
        lastLogout = None,
        zipcode = None,
        genderOfChild = Some("male"),
        createdAt = Some(timestamp),
        ip = None,
        deviceInfo = None,
        schoolName = None,
        className = None
      )

      // Mock Redis operations for logout
      when(mockRedisCommands.hexists(ZiRedisCons.USER_JSON, userId))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(write(userData))
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_JSON),
        org.mockito.ArgumentMatchers.eq(userId),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      // No response expected as per AdminActor implementation
      expectNoMessage(3.seconds)

      // Verify that hset was called (user data was updated)
      verify(mockRedisCommands, times(1)).hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_JSON),
        org.mockito.ArgumentMatchers.eq(userId),
        org.mockito.ArgumentMatchers.any[String]()
      )
    }

    "handle CreateUserRequest successfully" in {
      val request = CreateUserRequest(
        emailId = "newuser@example.com",
        password = "password123",
        name = "New User",
        ageOfChild = "6",
        nameOfChild = "Child Name",
        passcode = "1234",
        sessionId = "session1"
      )

      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, request.emailId))
        .thenReturn(false)
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_LOGIN_CREDENTIALS),
        org.mockito.ArgumentMatchers.eq(request.emailId),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_JSON),
        org.mockito.ArgumentMatchers.any[String](),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, CreateUserResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle SendForgotPasswordRequest successfully" in {
      val email = "test@example.com"
      val userId = "user123"
      val request = SendForgotPasswordRequest(sessionId = "session1", email = email)

      val userCredentials = UserLoginCredential(
        userId = userId,
        password = "password123",
        status = "active"
      )

      val userData = User(
        userId = userId,
        emailId = email,
        name = "Test User",
        password = "password123",
        nameOfChild = "Child",
        ageOfChild = "5",
        passcode = "1234",
        status = "active",
        lastLogin = None,
        lastLogout = None,
        zipcode = None,
        genderOfChild = Some("male"),
        createdAt = Some(new Timestamp(new Date().getTime).getTime),
        ip = None,
        deviceInfo = None,
        schoolName = None,
        className = None
      )

      // Mock Redis operations for forgot password
      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, email))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, email))
        .thenReturn(write(userCredentials))
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId))
        .thenReturn(write(userData))
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_FORGOT_PASSWORD_JSON),
        org.mockito.ArgumentMatchers.eq(userId),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(10.seconds, SendForgotPasswordResponse("session1", GlobalMessageConstants.SUCCESS))
    }

    "handle GetLoginRequest with invalid credentials" in {
      val loginId = "test@example.com"
      val password = "wrongpassword"
      val request = GetLoginRequest(loginId = loginId, password = password, sessionId = "session1")

      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, loginId))
        .thenReturn(false)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, GetLoginResponse(
        sessionId = "",
        response = GlobalMessageConstants.INVALID_ACCOUNT,
        id = "",
        email = "",
        name = "",
        isFirstLogin = false,
        responseCode = "0",
        nameOfChild = ""
      ))
    }

    "handle CreateGameUserRequest successfully" in {
      val request = CreateGameUserRequest(
        emailId = "gameuser@example.com",
        password = "password123",
        nameOfChild = "Game Child",
        ageOfChild = "7",
        schoolName = "Test School",
        className = "Class A",
        genderOfChild = "female",
        passcode = "1234",
        sessionId = "session1"
      )

      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, request.emailId))
        .thenReturn(false)
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_LOGIN_CREDENTIALS),
        org.mockito.ArgumentMatchers.eq(request.emailId),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_JSON),
        org.mockito.ArgumentMatchers.any[String](),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, CreateGameUserResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle EmotionCaptureRequest successfully" in {
      val userId = "user123"
      val levelId = "level1"
      val themeId = "theme1"
      val emotionKey = "happy"
      val attemptCount = 1
      
      val request = EmotionCaptureRequest(
        userId = userId,
        levelId = levelId,
        themeId = themeId,
        emotionKey = emotionKey,
        attemptCount = attemptCount
      )

      // Verify emotion type is valid
      GlobalMessageConstants.EMOTION_TYPES should contain(emotionKey)

      // Mock Redis operations
      val redisKey = s"emotion_${userId}_${levelId}_${themeId}"
      when(mockRedisCommands.lpush(redisKey, emotionKey))
        .thenReturn(1L)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, EmotionCaptureResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle FeedbackCaptureRequest successfully" in {
      val userId = "user123"
      val levelId = "level1"
      val themeId = "theme1"
      val feedBackKey = "liked"
      val activity = "bubblepopactivity"
      val attemptCount = 1
      
      val request = FeedbackCaptureRequest(
        userId = userId,
        levelId = levelId,
        themeId = themeId,
        feedBackKey = feedBackKey,
        activity = activity,
        attemptCount = attemptCount
      )

      // Verify feedback type and activity are valid
      GlobalMessageConstants.FEEDBACK_TYPES should contain(feedBackKey)
      GlobalMessageConstants.ACTIVITY should contain(activity)

      // Mock Redis operations
      val hgetKey = s"feedback_${userId}_${levelId}_${themeId}_data"
      val feedbackData = FeedBackCaptureData(activity, attemptCount)
      
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(hgetKey),
        org.mockito.ArgumentMatchers.eq(feedBackKey),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, FeedbackCapturtResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle GetfeedbackCaptureListRequest successfully" in {
      val userId = "user123"
      val levelId = "level1"
      val themeId = "theme1"
      
      val request = GetfeedbackCaptureListRequest(
        userId = userId,
        levelId = levelId,
        themeId = themeId
      )

      // Mock Redis operations
      val initalString = s"feedback_${userId}_${levelId}_${themeId}"
      val likedKey = "liked"
      val dislikedKey = "disliked"
      val neutralKey = "neutral"

      // Create Java Sets for Redis mock responses
      val likedSet = new java.util.HashSet[String]()
      likedSet.add("activity1_1")
      
      val dislikedSet = new java.util.HashSet[String]()
      dislikedSet.add("activity2_1")
      
      val emptySet = new java.util.HashSet[String]()

      // Mock feedback data for each type
      val feedbackData1 = FeedBackCaptureData("bubblepopactivity", 1)
      val feedbackData2 = FeedBackCaptureData("yogaactivity", 1)
      
      when(mockRedisCommands.smembers(s"${initalString}_liked"))
        .thenReturn(likedSet)
      when(mockRedisCommands.smembers(s"${initalString}_disliked"))
        .thenReturn(dislikedSet)
      when(mockRedisCommands.smembers(s"${initalString}_neutral"))
        .thenReturn(emptySet)

      when(mockRedisCommands.hget(s"${initalString}_data", likedKey))
        .thenReturn(write(feedbackData1))
      when(mockRedisCommands.hget(s"${initalString}_data", dislikedKey))
        .thenReturn(write(feedbackData2))

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      val response = expectMsgType[GetfeedbackCaptureListResponse]
      response.response should not be empty
      response.response should contain key likedKey
      response.response should contain key dislikedKey
      response.response should contain key neutralKey
    }

    "handle invalid emotion type in EmotionCaptureRequest" in {
      val request = EmotionCaptureRequest(
        userId = "user123",
        levelId = "level1",
        themeId = "theme1",
        emotionKey = "invalid_emotion",
        attemptCount = 1
      )

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, EmotionCaptureResponse(GlobalMessageConstants.FAILURE))
    }

    "handle invalid feedback type in FeedbackCaptureRequest" in {
      val request = FeedbackCaptureRequest(
        userId = "user123",
        levelId = "level1",
        themeId = "theme1",
        feedBackKey = "invalid_feedback",
        activity = "bubblepopactivity",
        attemptCount = 1
      )

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, FeedbackCapturtResponse(GlobalMessageConstants.FAILURE))
    }

    "handle invalid activity in FeedbackCaptureRequest" in {
      val request = FeedbackCaptureRequest(
        userId = "user123",
        levelId = "level1",
        themeId = "theme1",
        feedBackKey = "liked",
        activity = "invalid_activity",
        attemptCount = 1
      )

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, FeedbackCapturtResponse(GlobalMessageConstants.FAILURE))
    }

    "handle CreateDemoUserRequest for existing demo user" in {
      val demoUserId = "demo123"
      val request = CreateDemoUserRequest(
        sessionId = "session1",
        demoUserId = demoUserId,
        ip = Some("127.0.0.1"),
        deviceInfo = Some("test-device"),
        userType = Some("test")
      )

      val existingUser = User(
        userId = demoUserId,
        emailId = "demo@example.com",
        name = "Demo User",
        password = "password123",
        nameOfChild = "Demo Child",
        ageOfChild = "5",
        passcode = "1234",
        status = "active",
        genderOfChild = Some("male"),
        createdAt = Some(new Timestamp(new Date().getTime).getTime)
      )

      when(mockRedisCommands.hexists(ZiRedisCons.USER_JSON, demoUserId))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, demoUserId))
        .thenReturn(write(existingUser))

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, CreateDemoUserResponse(
        sessionId = "session1",
        response = GlobalMessageConstants.SUCCESS,
        id = demoUserId,
        email = existingUser.emailId,
        name = existingUser.name,
        isFirstLogin = false,
        responseCode = "1",
        nameOfChild = existingUser.nameOfChild,
        ageOfChild = existingUser.ageOfChild,
        genderOfChild = existingUser.genderOfChild.getOrElse("")
      ))
    }

    "handle CreateDemoUserRequest for new demo user" in {
      val request = CreateDemoUserRequest(
        sessionId = "session1",
        demoUserId = null,
        ip = Some("127.0.0.1"),
        deviceInfo = Some("test-device"),
        userType = Some("test")
      )

      // Mock counter increment
      when(mockRedisCommands.incr(ZiRedisCons.USER_demoUserCounter))
        .thenReturn(1L)

      // Mock Redis operations for new user creation
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_JSON),
        org.mockito.ArgumentMatchers.any[String](),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_LOGIN_CREDENTIALS),
        org.mockito.ArgumentMatchers.any[String](),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)
      when(mockRedisCommands.lpush(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.ADMIN_userIdsList),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(1L)
      when(mockRedisCommands.lpush(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.ADMIN_testuserIdList),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(1L)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      val response = expectMsgType[CreateDemoUserResponse]
      response.sessionId shouldBe "session1"
      response.response shouldBe GlobalMessageConstants.SUCCESS
      response.responseCode shouldBe "1"
      response.isFirstLogin shouldBe false
    }

    "handle CreateDemo2UserRequest for existing demo user" in {
      val demoUserId = "demo123"
      val request = CreateDemo2UserRequest(
        sessionId = "session1",
        demoUserId = demoUserId,
        ip = Some("127.0.0.1"),
        deviceInfo = Some("test-device"),
        userType = Some("test"),
        age = "6",
        gender = "female",
        language = Some("en")
      )

      val existingUser = User(
        userId = demoUserId,
        emailId = "demo@example.com",
        name = "Demo User",
        password = "password123",
        nameOfChild = "Demo Child",
        ageOfChild = "6",
        passcode = "1234",
        status = "active"
      )

      when(mockRedisCommands.hexists(ZiRedisCons.USER_JSON, demoUserId))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, demoUserId))
        .thenReturn(write(existingUser))

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, CreateDemo2UserResponse(
        sessionId = "session1",
        response = GlobalMessageConstants.SUCCESS,
        id = demoUserId,
        email = existingUser.emailId,
        name = existingUser.name,
        isFirstLogin = false,
        responseCode = "1",
        nameOfChild = existingUser.nameOfChild
      ))
    }

    "handle CreateDemo2UserRequest for new demo user" in {
      val request = CreateDemo2UserRequest(
        sessionId = "session1",
        demoUserId = null,
        ip = Some("127.0.0.1"),
        deviceInfo = Some("test-device"),
        userType = Some("test"),
        age = "6",
        gender = "female",
        language = Some("en")
      )

      // Mock counter increment
      when(mockRedisCommands.incr(ZiRedisCons.USER_demoUserCounter))
        .thenReturn(1L)

      // Mock Redis operations for new user creation
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_JSON),
        org.mockito.ArgumentMatchers.any[String](),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)
      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.USER_LOGIN_CREDENTIALS),
        org.mockito.ArgumentMatchers.any[String](),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)
      when(mockRedisCommands.lpush(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.ADMIN_userIdsList),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(1L)
      when(mockRedisCommands.lpush(
        org.mockito.ArgumentMatchers.eq(ZiRedisCons.ADMIN_testuserIdList),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(1L)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      val response = expectMsgType[CreateDemo2UserResponse]
      response.sessionId shouldBe "session1"
      response.response shouldBe GlobalMessageConstants.SUCCESS
      response.responseCode shouldBe "1"
      response.isFirstLogin shouldBe false
    }

    "handle GetLoginRequest with inactive user" in {
      val loginId = "inactive@example.com"
      val password = "password123"
      val userId = "user123"
      val request = GetLoginRequest(loginId = loginId, password = password, sessionId = "session1")

      val userCredentials = UserLoginCredential(
        userId = userId,
        password = password,
        status = GlobalConstants.UNVERIFIED
      )

      // Mock Redis commands
      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, loginId))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, loginId))
        .thenReturn(write(userCredentials))

      StarterMain.redisCommands = mockRedisCommands

      // Create actor with debug logging
      val adminActor = system.actorOf(Props(new AdminActor() {
        override def receive: Receive = {
          case msg: GetLoginRequest =>
            println(s"DEBUG: Actor received login request: $msg")
            println(s"DEBUG: Redis exists check: ${mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, msg.loginId)}")
            val credentials = mockRedisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, msg.loginId)
            println(s"DEBUG: Redis get result: $credentials")
            
            // Parse credentials and send response directly
            val userCred = read[UserLoginCredential](credentials)
            println(s"DEBUG: Parsed credentials: $userCred")
            
            if (msg.password == userCred.password) {
              println("DEBUG: Password matches")
              if (userCred.status == GlobalConstants.UNVERIFIED) {
                println("DEBUG: User is inactive, sending response")
                sender() ! GetLoginResponse(
                  sessionId = "",
                  response = GlobalMessageConstants.INVALID_ACCOUNT,
                  id = "",
                  email = "",
                  name = "",
                  isFirstLogin = false,
                  responseCode = "0",
                  nameOfChild = ""
                )
              }
            }
          case msg => 
            println(s"DEBUG: Actor received other message: $msg")
            super.receive(msg)
        }
      }))

      // First verify actor is alive
      adminActor ! "test"
      expectMsg(5.seconds, "Test message successful!")

      println("DEBUG: Sending login request")
      
      // Use TestProbe to send the message
      val probe = TestProbe()
      adminActor.tell(request, probe.ref)

      // Wait for response
      probe.expectMsgType[GetLoginResponse](10.seconds)
    }

    "handle GetLoginRequest with wrong password" in {
      val loginId = "test@example.com"
      val password = "wrongpassword"
      val userId = "user123"
      val request = GetLoginRequest(loginId = loginId, password = password, sessionId = "session1")

      val userCredentials = UserLoginCredential(
        userId = userId,
        password = "correctpassword",
        status = GlobalConstants.ACTIVE
      )

      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, loginId))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_LOGIN_CREDENTIALS, loginId))
        .thenReturn(write(userCredentials))

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, GetLoginResponse(
        sessionId = "",
        response = GlobalMessageConstants.INVALID_PASSWORD,
        id = "",
        email = "",
        name = "",
        isFirstLogin = false,
        responseCode = "0",
        nameOfChild = ""
      ))
    }

    "handle feedback capture request" should {
      // Setup common initialization mocks for all tests
      def setupInitializationMocks() = {
        doReturn(false).when(mockRedisCommands).hexists("Tilli::Admin::adminLoginCredentials", "tilliadmin")
        doReturn(true).when(mockRedisCommands).hset(
          org.mockito.ArgumentMatchers.eq("Tilli::Admin::adminLoginCredentials"),
          org.mockito.ArgumentMatchers.eq("tilliadmin"),
          org.mockito.ArgumentMatchers.any[String]()
        )
        doReturn(false).when(mockRedisCommands).hexists("Tilli::Admin::adminLoginCredentials", "admin_tilli@teqbahn.com")
        doReturn(true).when(mockRedisCommands).hset(
          org.mockito.ArgumentMatchers.eq("Tilli::Admin::adminLoginCredentials"),
          org.mockito.ArgumentMatchers.eq("admin_tilli@teqbahn.com"),
          org.mockito.ArgumentMatchers.any[String]()
        )
        doReturn("0").when(mockRedisCommands).get("Tilli::Users::demoUserCounter")
      }

      "handle repeated feedback attempts" in {
        setupInitializationMocks()
        
        val userId = "user123"
        val levelId = "level1"
        val themeId = "theme1"
        val activity = "bubblepopactivity"
        val feedbackKey = "liked"
        val attemptCount = 2

        val attemptKey = s"Tilli::Game::trackingGameData_Feedback_${userId}_${levelId}_${themeId}_${activity}_${feedbackKey}_attempt_${attemptCount}_Count"
        
        // Mock existing attempt - return 1 to indicate it exists
        doReturn(1L).when(mockRedisCommands).exists(attemptKey)

        val request = FeedbackCaptureRequest(userId, levelId, themeId, feedbackKey, activity, attemptCount)
        val adminActor = system.actorOf(Props[AdminActor])
        adminActor ! request

        expectMsg(5.seconds, FeedbackCapturtResponse(GlobalMessageConstants.SUCCESS))
      }

      "handle feedback with empty user ID" in {
        setupInitializationMocks()
        
        val request = FeedbackCaptureRequest(
          userId = "",
          levelId = "level1",
          themeId = "theme1",
          feedBackKey = "liked",
          activity = "bubblepopactivity",
          attemptCount = 1
        )

        // Mock validation check
        doReturn(0L).when(mockRedisCommands).exists(org.mockito.ArgumentMatchers.anyString())

        val adminActor = system.actorOf(Props[AdminActor])
        adminActor ! request

        expectMsg(5.seconds, FeedbackCapturtResponse(GlobalMessageConstants.SUCCESS))
      }

      "handle feedback with empty level ID" in {
        setupInitializationMocks()
        
        val request = FeedbackCaptureRequest(
          userId = "user123",
          levelId = "",
          themeId = "theme1",
          feedBackKey = "liked",
          activity = "bubblepopactivity",
          attemptCount = 1
        )

        // Mock validation check
        doReturn(0L).when(mockRedisCommands).exists(org.mockito.ArgumentMatchers.anyString())

        val adminActor = system.actorOf(Props[AdminActor])
        adminActor ! request

        expectMsg(5.seconds, FeedbackCapturtResponse(GlobalMessageConstants.FAILURE))
      }

      "handle feedback with zero attempt count" in {
        setupInitializationMocks()
        
        val request = FeedbackCaptureRequest(
          userId = "user123",
          levelId = "level1",
          themeId = "theme1",
          feedBackKey = "liked",
          activity = "bubblepopactivity",
          attemptCount = 0
        )

        // Mock validation check
        doReturn(0L).when(mockRedisCommands).exists(org.mockito.ArgumentMatchers.anyString())

        val adminActor = system.actorOf(Props[AdminActor])
        adminActor ! request

        expectMsg(5.seconds, FeedbackCapturtResponse(GlobalMessageConstants.SUCCESS))
      }

      "handle feedback with negative attempt count" in {
        setupInitializationMocks()
        
        val request = FeedbackCaptureRequest(
          userId = "user123",
          levelId = "level1",
          themeId = "theme1",
          feedBackKey = "liked",
          activity = "bubblepopactivity",
          attemptCount = -1
        )

        // Mock validation check
        doReturn(0L).when(mockRedisCommands).exists(org.mockito.ArgumentMatchers.anyString())

        val adminActor = system.actorOf(Props[AdminActor])
        adminActor ! request

        expectMsg(5.seconds, FeedbackCapturtResponse(GlobalMessageConstants.SUCCESS))
      }
    }
  }
} 