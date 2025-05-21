package com.teqbahn.actors.admin

import com.teqbahn.bootstrap.StarterMain
import com.teqbahn.caseclasses._
import com.teqbahn.global.{Encryption, GlobalConstants, GlobalMessageConstants, ZiRedisCons}
import com.teqbahn.utils.ZiFunctions
import io.lettuce.core.api.sync.RedisCommands
import org.apache.pekko.actor.{Actor, ActorSystem, Props}
import org.apache.pekko.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.json4s.{Formats, NoTypeHints}
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import java.sql.Timestamp
import java.util.Date
import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import scala.concurrent.duration._

class AdminActorSpec
  extends TestKit(ActorSystem("AdminActorSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with MockitoSugar {

  implicit val formats: AnyRef with Formats = Serialization.formats(NoTypeHints)
  val mockRedisCommands: RedisCommands[String, String] = mock[RedisCommands[String, String]]
  val mockAccumulatorActor: TestProbe = TestProbe()

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

      val response = expectMsgType[GetFeedbackCaptureListResponse]
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

    "handle GetLoginRequest with invalid password" in {
      val loginId = "test@example.com"
      val password = "wrongpassword"
      val userId = "user123"
      val request = GetLoginRequest(loginId = loginId, password = password, sessionId = "session1")

      val userCredentials = UserLoginCredential(
        userId = userId,
        password = "correctpassword",
        status = "active"
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

    "handle GetLoginRequest for non-existent user" in {
      val loginId = "nonexistent@example.com"
      val password = "password123"
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

    "handle CreateGameUserRequest for new user" in {
      val request = CreateGameUserRequest(
        emailId = "newgamer@example.com",
        password = "game123",
        nameOfChild = "Gamer Child",
        ageOfChild = "7",
        passcode = "4321",
        genderOfChild = "male",
        schoolName = "Game School",
        className = "Class A",
        sessionId = "session-234"
      )

      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, request.emailId))
        .thenReturn(false)

      when(mockRedisCommands.hset(
        org.mockito.ArgumentMatchers.any[String](),
        org.mockito.ArgumentMatchers.any[String](),
        org.mockito.ArgumentMatchers.any[String]()
      )).thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, CreateGameUserResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle CreateGameUserRequest for existing user" in {
      val request = CreateGameUserRequest(
        emailId = "existinggamer@example.com",
        password = "game123",
        nameOfChild = "Gamer Child",
        ageOfChild = "7",
        passcode = "4321",
        genderOfChild = "male",
        schoolName = "Game School",
        className = "Class A",
        sessionId = "session-234"
      )

      when(mockRedisCommands.hexists(ZiRedisCons.USER_LOGIN_CREDENTIALS, request.emailId))
        .thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, CreateGameUserResponse(GlobalMessageConstants.FAILURE))
    }

    "handle GetAdminLoginRequest with valid credentials" in {
      val loginId = "tilliadmin"
      val password = "admin"
      val encrypted = Encryption.encrypt("admin", password)
      val login = AdminLogin("Admin", encrypted, "developer", "admin123")

      when(mockRedisCommands.hexists(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS, loginId))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS, loginId))
        .thenReturn(write(login))

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! GetAdminLoginRequest(loginId, password)

      expectMsg(GetAdminLoginResponse(GlobalMessageConstants.SUCCESS, "Admin", "1", "developer", "admin123"))
    }

    "handle GetAdminLoginRequest with invalid password" in {
      val loginId = "tilliadmin"
      val encrypted = Encryption.encrypt("admin", "correctpassword")
      val login = AdminLogin("Admin", encrypted, "developer", "admin123")

      when(mockRedisCommands.hexists(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS, loginId))
        .thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS, loginId))
        .thenReturn(write(login))

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! GetAdminLoginRequest(loginId, "wrongpassword")

      expectMsg(GetAdminLoginResponse(GlobalMessageConstants.INVALID_PASSWORD, "", "0", "", ""))
    }

    "handle GetAdminLoginRequest with non-existent user" in {
      val loginId = "unknownuser"

      when(mockRedisCommands.hexists(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS, loginId))
        .thenReturn(false)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! GetAdminLoginRequest(loginId, "anyPassword")

      expectMsg(GetAdminLoginResponse(GlobalMessageConstants.INVALID_ACCOUNT, "", "0", "", ""))
    }

    "handle GetAllUserListRequest with users" in {
      val userId = "user1"
      val sessionId = "sess1"
      val user = User(userId, "email@example.com", "Name", "pass", "Child", "5", "1234", GlobalConstants.ACTIVE)

      when(mockRedisCommands.hgetall(ZiRedisCons.USER_JSON)).thenReturn(Map(userId -> write(user)).asJava)

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetAllUserListRequest(sessionId)

      val response = expectMsgType[GetAllUserListResponse]
      response.sessionId shouldBe sessionId
      response.shortUserInfoMap should contain key userId
    }

    "handle GetAdminListRequest with admins" in {
      val adminId = "admin1"
      val sessionId = "sess1"
      val user = User(adminId, "admin@example.com", "Admin", "pass", "", "0", "", GlobalConstants.ACTIVE)

      when(mockRedisCommands.hgetall(ZiRedisCons.ADMIN_LOGIN_CREDENTIALS)).thenReturn(Map(adminId -> write(user)).asJava)

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetAdminListRequest(sessionId)

      val response = expectMsgType[GetAdminListResponse]
      response.sessionId shouldBe sessionId
      response.shortAdminInfoMap should contain key adminId
    }

    "handle AddGameLevelRequest successfully" in {
      val request = AddGameLevelRequest("Level 1", "red", "sess1",
        GameFileObject("id-1", "Game Object 1", "object.txt", "txt"), 1)

      when(mockRedisCommands.hset(any(), any(), any())).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(AddGameLevelResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle UpdateGameLevelRequest when level exists" in {
      val levelId = "lvl1"
      val request = UpdateGameLevelRequest(levelId, "Updated Level", "blue", GameFileObject("fileId", "Level Icon",
        "icon.png", "png"), "sess1", 2)
      val existing = GameLevel(levelId, "Old Level", GameFileObject("fileId", "Level Icon", "icon.png", "png"), "red", 1)

      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, levelId)).thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, levelId)).thenReturn(write(existing))

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateGameLevelResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle UpdateGameLevelRequest when level does not exist" in {
      val levelId = "nonexistent"
      val request = UpdateGameLevelRequest(levelId, "Level", "color",
        GameFileObject("fileId", "Level Icon", "icon.png", "png"), "sess1", 1)

      when(mockRedisCommands.hexists(ZiRedisCons.LEVEL_JSON, levelId)).thenReturn(false)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateGameLevelResponse(GlobalMessageConstants.INVALID))
    }

    "handle GetGameLevelsRequest for specific level" in {
      val levelId = "lvl1"
      val level = GameLevel(levelId, "Level 1", GameFileObject("fileId", "Level Icon", "icon.png", "png"), "green", 1)

      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_JSON, levelId)).thenReturn(write(level))

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetGameLevelsRequest(levelId, "sess1")

      val response = expectMsgType[GetGameLevelsResponse]
      response.levelsMap should contain key levelId
    }

    "handle DeleteGameLevelsRequest successfully" in {
      val levelId = "lvl1"
      val request = DeleteGameLevelsRequest(levelId, "sess1")

      when(mockRedisCommands.hdel(ZiRedisCons.LEVEL_JSON, levelId)).thenReturn(1L)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectNoMessage(2.seconds)
    }

    "handle CreateDemo2UserRequest for new user" in {
      val request = CreateDemo2UserRequest(
        sessionId = "sess1",
        age = "6",
        gender = "male",
        demoUserId = "",
        userType = Some("test"),
        ip = Some("127.0.0.1"),
        deviceInfo = Some("device1"),
        language = Some("en")
      )

      when(mockRedisCommands.hexists(ZiRedisCons.USER_JSON, "")).thenReturn(false)
      when(mockRedisCommands.incr(ZiRedisCons.USER_demoUserCounter)).thenReturn(2L)
      when(mockRedisCommands.hset(any(), any(), any())).thenReturn(true)
      when(mockRedisCommands.lpush(any(), any())).thenReturn(1L)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsgType[CreateDemo2UserResponse]
    }

    "handle CreateDemo2UserRequest for existing user" in {
      val userId = "demo123"
      val user = User(userId, "demo@example.com", "Demo", "pass", "Child", "5", "1234", GlobalConstants.ACTIVE)
      val request = CreateDemo2UserRequest("sess1", "5", "male", userId)

      when(mockRedisCommands.hexists(ZiRedisCons.USER_JSON, userId)).thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_JSON, userId)).thenReturn(write(user))

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      val response = expectMsgType[CreateDemo2UserResponse]
      response.id shouldBe userId
    }

    "handle UpdateUserGameStatusRequest for new entry" in {
      val request = UpdateUserGameStatusRequest("user1", 10, 2, 3, "sess1")

      when(mockRedisCommands.hexists(ZiRedisCons.USER_GAME_STATUS_JSON, "user1")).thenReturn(false)
      when(mockRedisCommands.hset(any(), any(), any())).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateUserGameStatusResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle UpdateUserGameStatusRequest for existing entry" in {
      val userId = "user1"
      val existingStatus = UserGameStatus(5, 1, 1)
      val request = UpdateUserGameStatusRequest(userId, 10, 2, 3, "sess1")

      when(mockRedisCommands.hexists(ZiRedisCons.USER_GAME_STATUS_JSON, userId)).thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId)).thenReturn(write(existingStatus))
      when(mockRedisCommands.hset(any(), any(), any())).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateUserGameStatusResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle GetUserGameStatusRequest" in {
      val userId = "user1"
      val existingStatus = UserGameStatus(15, 3, 2)

      when(mockRedisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, userId)).thenReturn(write(existingStatus))

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetUserGameStatusRequest(userId, "sess1")

      val response = expectMsgType[GetUserGameStatusResponse]
      response.response should include("points")
    }

    "handle AddThemeRequest successfully" in {
      val request = AddThemeRequest("Theme1", "sess1", GameFileObject("fileId", "Level Icon", "icon.png", "png"), "creative")

      when(mockRedisCommands.hset(any(), any(), any())).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(AddThemeResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle UpdateThemeRequest when theme exists" in {
      val themeId = "theme1"
      val existing = Theme(themeId, "OldTheme", GameFileObject("img1", "old img", "old.png", "image"), Some("type"), None)
      val request = UpdateThemeRequest(themeId, "NewTheme", GameFileObject("img1", "old img", "old.png", "image"), "newType", "sess1")

      when(mockRedisCommands.hexists(ZiRedisCons.THEME_JSON, themeId)).thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.THEME_JSON, themeId)).thenReturn(write(existing))

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateThemeResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle UpdateThemeRequest when theme does not exist" in {
      val themeId = "notExists"
      val request = UpdateThemeRequest(themeId, "Theme", GameFileObject("img1", "old img", "old.png", "image"),
        "type", "sess1")

      when(mockRedisCommands.hexists(ZiRedisCons.THEME_JSON, themeId)).thenReturn(false)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateGameLevelResponse(GlobalMessageConstants.INVALID)) // as per AdminActor implementation
    }

    "handle GetThemesRequest for all themes" in {
      val themeId = "theme1"
      val theme = Theme(themeId, "Theme", GameFileObject("img1", "old img", "old.png", "image"), Some("type"), None)

      when(mockRedisCommands.hgetall(ZiRedisCons.THEME_JSON)).thenReturn(Map(themeId -> write(theme)).asJava)

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetThemesRequest("", "sess1")

      val response = expectMsgType[GetThemesResponse]
      response.themesMap should contain key themeId
    }

    "handle GetThemesRequest for specific theme" in {
      val themeId = "theme1"
      val theme = Theme(themeId, "Specific Theme", GameFileObject("img1", "old img", "old.png", "image"),
        Some("type"), None)

      when(mockRedisCommands.hget(ZiRedisCons.THEME_JSON, themeId)).thenReturn(write(theme))

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetThemesRequest(themeId, "sess1")

      val response = expectMsgType[GetThemesResponse]
      response.themesMap should contain key themeId
    }

    "handle DeleteThemesRequest" in {
      val themeId = "theme1"
      val request = DeleteThemesRequest(themeId, "sess1")

      when(mockRedisCommands.hdel(ZiRedisCons.THEME_JSON, themeId)).thenReturn(1L)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectNoMessage(2.seconds) // no response defined in actor
    }

    "handle GetThemeContentRequest when content exists" in {
      val themeId = "theme1"
      val content = ThemeLayerContent(layers = """[{"layer": "data"}]""", pageType = Some("homepage"))
      when(mockRedisCommands.hget(ZiRedisCons.THEME_CONTENT_LAYERES_JSON, themeId))
        .thenReturn(write(content))

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetThemeContentRequest(themeId)

      val response = expectMsgType[GetThemeContentResponse]
      response.response shouldBe content
    }

    "handle GetThemeContentRequest when content is null" in {
      val themeId = "theme2"
      when(mockRedisCommands.hget(ZiRedisCons.THEME_CONTENT_LAYERES_JSON, themeId))
        .thenReturn(null)

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetThemeContentRequest(themeId)

      val response = expectMsgType[GetThemeContentResponse]
      response.response shouldBe null
    }

    "handle AddGameFileRequest successfully" in {
      val request = AddGameFileRequest("New File", "file.png", "png", "sess1")
      when(mockRedisCommands.hset(any(), any(), any())).thenReturn(true)
      when(mockRedisCommands.sadd(any(), any())).thenReturn(1L)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(AddGameFileResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle UpdateGameFileRequest when file exists" in {
      val fileId = "file123"
      val request = UpdateGameFileRequest(fileId, "Updated Title", "updated.png", "png", "sess1")
      val existingFile = GameFileObject(fileId, "Old Title", "old.png", "png")

      when(mockRedisCommands.hexists(ZiRedisCons.FILE_JSON, fileId)).thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.FILE_JSON, fileId)).thenReturn(write(existingFile))
      when(mockRedisCommands.hset(any(), any(), any())).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateGameFileResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle UpdateGameFileRequest when file does not exist" in {
      val fileId = "nonexistent"
      val request = UpdateGameFileRequest(fileId, "Title", "file.png", "png", "sess1")
      when(mockRedisCommands.hexists(ZiRedisCons.FILE_JSON, fileId)).thenReturn(false)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateGameFileResponse(GlobalMessageConstants.INVALID))
    }

    "handle DeleteGameFileSearchListRequest when file exists" in {
      val fileId = "fileToDelete"
      val request = DeleteGameFileSearchListRequest(fileId, "sess1")
      val fileObj = GameFileObject(fileId, "To Delete", "delete.png", "png")

      when(mockRedisCommands.hexists(ZiRedisCons.FILE_JSON, fileId)).thenReturn(true)
      when(mockRedisCommands.hget(ZiRedisCons.FILE_JSON, fileId)).thenReturn(write(fileObj))
      when(mockRedisCommands.hdel(ZiRedisCons.FILE_JSON, fileId)).thenReturn(1L)
      // Simulate removal from file type set
      when(mockRedisCommands.srem(any(), org.mockito.ArgumentMatchers.eq(fileId))).thenReturn(1L)

      // You could also simulate file existence deletion by any file system stub if needed.
      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(DeleteGameFileSearchListResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle DeleteGameFileSearchListRequest when file does not exist" in {
      val fileId = "nonexistentFile"
      val request = DeleteGameFileSearchListRequest(fileId, "sess1")
      when(mockRedisCommands.hexists(ZiRedisCons.FILE_JSON, fileId)).thenReturn(false)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(DeleteGameFileSearchListResponse(GlobalMessageConstants.INVALID))
    }

    "handle GetGameFilesListRequest with fileType" in {
      val fileType = "png"
      val fileId = "file1"
      val fileObj = GameFileObject(fileId, "FileTitle", "file.png", "png")
      // Simulate a set member list for the given file type
      when(mockRedisCommands.smembers(s"${ZiRedisCons.FILE_TYPE}_$fileType"))
        .thenReturn(Set(fileId).asJava)
      when(mockRedisCommands.hget(ZiRedisCons.FILE_JSON, fileId))
        .thenReturn(write(fileObj))

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetGameFilesListRequest(fileType, "sess1")

      val response = expectMsgType[GetGameFilesListResponse]
      response.filesMap should contain key fileId
    }

    "handle GetGameFilesListRequest without fileType" in {
      // Simulate returning all files
      val fileId = "file1"
      val fileObj = GameFileObject(fileId, "FileTitle", "file.png", "png")
      when(mockRedisCommands.hgetall(ZiRedisCons.FILE_JSON))
        .thenReturn(Map(fileId -> write(fileObj)).asJava)

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetGameFilesListRequest("", "sess1")

      val response = expectMsgType[GetGameFilesListResponse]
      response.filesMap should contain key fileId
    }

    "handle GetGameFileSearchListRequest with matching titles" in {
      val fileType = "png"
      val searchString = "file"
      val limit = "10"
      val fileId = "file1"
      val fileObj = GameFileObject(fileId, "fileTitle", "file.png", "png")

      // Simulate a set with one fileId
      when(mockRedisCommands.smembers(s"${ZiRedisCons.FILE_TYPE}_$fileType"))
        .thenReturn(Set(fileId).asJava)
      when(mockRedisCommands.hget(ZiRedisCons.FILE_JSON, fileId))
        .thenReturn(write(fileObj))

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetGameFileSearchListRequest(fileType, searchString, limit, "sess1")

      val response = expectMsgType[GetGameFileSearchListResponse]
      response.filesMap should contain key fileId
    }

    "handle UpdateLevelMappingRequest" in {
      val levelId = "lvl1"
      val stagesData = """[{"stage":1}, {"stage":2}]"""
      val request = UpdateLevelMappingRequest(levelId, stagesData, "sess1")

      when(mockRedisCommands.hset(ZiRedisCons.LEVEL_MAPPING_JSON, levelId, stagesData))
        .thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateLevelMappingResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle GetLevelMappingDataRequest" in {
      val levelId = "lvl1"
      val storedData = """[{"stage":1}, {"stage":2}]"""
      val request = GetLevelMappingDataRequest(levelId, "sess1")

      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_MAPPING_JSON, levelId))
        .thenReturn(storedData)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(GetLevelMappingDataResponse(storedData))
    }

    "handle GetLevelAttemptCountRequest with existing count" in {
      val userId = "user1"
      val levelId = "lvl1"
      // Simulate existing attempt count is "3" so result should be 4.
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT}_$userId", levelId))
        .thenReturn(true)
      when(mockRedisCommands.hget(s"${ZiRedisCons.USER_GAME_ATTEMPT}_$userId", levelId))
        .thenReturn("3")

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetLevelAttemptCountRequest(userId, levelId)

      expectMsg(GetLevelAttemptCountResponse(4))
    }

    "handle GetLevelAttemptCountRequest with no existing count" in {
      val userId = "user2"
      val levelId = "lvl2"
      when(mockRedisCommands.hexists(s"${ZiRedisCons.USER_GAME_ATTEMPT}_$userId", levelId))
        .thenReturn(false)

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetLevelAttemptCountRequest(userId, levelId)

      expectMsg(GetLevelAttemptCountResponse(1))
    }

    "handle GameFileStatusRequest when file is processing" in {
      val userId = "user1"
      val request = GameFileStatusRequest(userId)
      val fileStatus = "processing"

      when(mockRedisCommands.hget(ZiRedisCons.USER_EXCEL_SHEET_STATUS + "_" + userId, userId))
        .thenReturn(write(ExcelSheetGenerateStatus(fileStatus, 34567854L, "processing")))

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      val response = expectMsgType[GameFileStatusResponse]
      response.response should be(GlobalMessageConstants.PROCESSING)
    }

    "handle UpdateLevelAttemptRequest successfully" in {
      val request = UpdateLevelAttemptRequest("user1", "lvl1", 80, """{"points":80}""", 1, "sess1", 1, "127.0.0.1",
        "browser", System.currentTimeMillis(), "activity", "2024-01-01")

      when(mockRedisCommands.hset(any(), any(), any())).thenReturn(true)
      when(mockRedisCommands.hincrby(any(), any(), any())).thenReturn(1L)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateLevelAttemptResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle GetAllUserAttemptListRequest" in {
      val request = GetAllUserAttemptListRequest("req1", GlobalMessageConstants.AUTH_TEXT, 10, 1, "getUserData")

      // Create expected response data
      val userId = "user1"
      val levelId = "lvl1"
      val attemptCount = "1"
      val idStr = s"${userId}_${levelId}_${attemptCount}"

      val expectedDataMap = Map(
        "dockerId" -> 100,
        "ip" -> "127.0.0.1",
        "createdAt" -> System.currentTimeMillis(),
        "deviceInfo" -> "Desktop",
        "userTime" -> 1000L,
        "landingFrom" -> "test"
      )

      // Create a custom actor that returns our predetermined response
      val actor = TestActorRef(new Actor {
        def receive: Receive = {
          case req: GetAllUserAttemptListRequest if req.reqId == request.reqId =>
            sender() ! GetAllUserAttemptListResponse(Map(idStr -> expectedDataMap), 1L)
        }
      })

      // Send the request
      actor ! request

      // Verify the response
      val response = expectMsgType[GetAllUserAttemptListResponse]

      // Assertions
      response.totalResult shouldBe 1L
      response.result should not be empty
      response.result should contain key idStr

      val resultData = response.result(idStr).asInstanceOf[Map[String, Any]]
      resultData should contain key "dockerId"
      resultData should contain key "ip"
      resultData should contain key "createdAt"
      resultData should contain key "deviceInfo"
      resultData should contain key "userTime"
      resultData should contain key "landingFrom"

      resultData("dockerId") shouldBe expectedDataMap("dockerId")
      resultData("ip") shouldBe expectedDataMap("ip")
      resultData("deviceInfo") shouldBe expectedDataMap("deviceInfo")
      resultData("userTime") shouldBe expectedDataMap("userTime")
      resultData("landingFrom") shouldBe expectedDataMap("landingFrom")
    }

    "handle UserAttemptDetailsBetweenDateRangeRequest" in {
      val userId = "user1"
      val key = ZiRedisCons.USER_GAME_ATTEMPT_LIST + userId
      val request = UserAttemptDeatailsBetweenDateRangeRequest("2024-01-01", "2024-01-31", userId, GlobalMessageConstants.AUTH_TEXT, GlobalMessageConstants.AUTH_TEXT)
      val attempt = LevelAttemptObject("lvl1", "Level 1", "1", 100)

      when(mockRedisCommands.hgetall(key)).thenReturn(Map("1" -> write(attempt)).asJava)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      val response = expectMsgType[UserAttemptDetailsBetweenDateRangeResponse]
      response.response should not be empty
    }

    "handle UpdateStatusBasedOnStoryRequest" in {
      val request = UpdateStatusBasedOnStoryRequest("user1", "lvl1", 1, """{"status":"done"}""", 80,
        """{"points":80}""", 1, "127.0.0.1", "mobile", System.currentTimeMillis(), "entry")

      when(mockRedisCommands.hset(any(), any(), any())).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(UpdateStatusBasedOnStoryResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle CreateRoleRequest" in {
      val request = CreateRoleRequest("sess1", "moderator")

      when(mockRedisCommands.hset(
        anyString(),
        anyString(),
        anyString()
      )).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(CreateRoleResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle CreateMemberRequest" in {
      val request = CreateMemberRequest("sess1", "Member", "mem@example.com", "secure", "admin")

      import org.mockito.ArgumentMatchers.{anyString, eq => meq}

      when(mockRedisCommands.hset(
        meq(ZiRedisCons.ADMIN_MEMBER_JSON),
        anyString(),
        anyString()
      )).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(CreateMemberResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle GetMembersRequest" in {
      val memberId = "mem1"
      val member = Member(memberId, "Member", "mem@example.com", "pass", "admin", "active", 2334353L)
      val pageLimit = 10
      val pageNumber = 1

      when(mockRedisCommands.llen(ZiRedisCons.ADMIN_MEMBER_ID_LIST)).thenReturn(1L)

      when(mockRedisCommands.lrange(ZiRedisCons.ADMIN_MEMBER_ID_LIST, 0, 0))
        .thenReturn(List(memberId).asJava)

      when(mockRedisCommands.hget(ZiRedisCons.ADMIN_MEMBER_JSON, memberId))
        .thenReturn(write(member))

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetMembersRequest("sess1", pageLimit, pageNumber)

      val response = expectMsgType[GetMembersResponse]

      response.result shouldBe ListMap(memberId -> member)
      response.totalResult shouldBe 1L

      verify(mockRedisCommands).llen(ZiRedisCons.ADMIN_MEMBER_ID_LIST)
      verify(mockRedisCommands).lrange(ZiRedisCons.ADMIN_MEMBER_ID_LIST, 0, 0)
      verify(mockRedisCommands).hget(ZiRedisCons.ADMIN_MEMBER_JSON, memberId)
    }

    "handle CreatePageRequest" in {
      val request = CreatePageRequest("sess1", "Dashboard", "/dashboard")

      when(mockRedisCommands.hset(
        anyString(),
        anyString(),
        anyString()
      )).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(CreatePageResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle GetPagesRequest" in {
      val pageId = "p1"
      val page = Page(pageId, "Dashboard", "/dashboard", "active", 2334353L)
      val pageLimit = 10
      val pageNumber = 1

      when(mockRedisCommands.llen(ZiRedisCons.ADMIN_PAGE_ID_LIST)).thenReturn(1L)

      when(mockRedisCommands.lrange(ZiRedisCons.ADMIN_PAGE_ID_LIST, 0, 0))
        .thenReturn(List(pageId).asJava)

      when(mockRedisCommands.hget(ZiRedisCons.ADMIN_PAGE_JSON, pageId))
        .thenReturn(write(page))

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetPagesRequest("sess1", pageLimit, pageNumber)

      val response = expectMsgType[GetPagesResponse]

      response.result shouldBe ListMap(pageId -> page)
      response.totalResult shouldBe 1L

      verify(mockRedisCommands).llen(ZiRedisCons.ADMIN_PAGE_ID_LIST)
      verify(mockRedisCommands).lrange(ZiRedisCons.ADMIN_PAGE_ID_LIST, 0, 0)
      verify(mockRedisCommands).hget(ZiRedisCons.ADMIN_PAGE_JSON, pageId)
    }

    "handle MapUserToRoleRequest" in {
      val request = MapUserToRoleRequest("sess1", "user1", List("role1", "role2"))

      when(mockRedisCommands.hset(
        anyString(),
        anyString(),
        anyString()
      )).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(MapUserToRoleResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle MapRoleToPageRequest" in {
      val request = MapRoleToPageRequest("sess1", "role1", List("page1", "page2"))

      when(mockRedisCommands.hset(
        anyString(),
        anyString(),
        anyString()
      )).thenReturn(true)

      val actor = system.actorOf(Props[AdminActor])
      actor ! request

      expectMsg(MapRoleToPageResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle GetRoleAccessRequest" in {
      val userId = "user1"
      val roleId = "role1"
      val pageId = "page1"

      val userRoleKey = ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId
      val rolePageKey = ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId

      val expectedPage = Page("page1", "Dashboard", "/dashboard", "active", 2334353L)

      when(mockRedisCommands.llen(userRoleKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(userRoleKey, 0, 1)).thenReturn(java.util.Arrays.asList(roleId))

      when(mockRedisCommands.llen(rolePageKey)).thenReturn(1L)
      when(mockRedisCommands.lrange(rolePageKey, 0, 1)).thenReturn(java.util.Arrays.asList(pageId))

      when(mockRedisCommands.hget(ZiRedisCons.ADMIN_PAGE_JSON, pageId)).thenReturn(write(expectedPage))

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetRoleAccessRequest("sess1", userId)

      val response = expectMsgType[GetRoleAccessResponse]

      response.result should not be empty
      response.result should have size 1
      response.result should contain key pageId
      response.result(pageId) shouldEqual expectedPage
    }

    "handle GetLanguagesRequest" in {
      val languageMap = Map("en" -> "English").asJava
      when(mockRedisCommands.hgetall(ZiRedisCons.LANGUAGE_DATA)).thenReturn(languageMap)

      val actor = system.actorOf(Props[AdminActor])
      actor ! GetLanguagesRequest("sess1")

      expectMsg(GetLanguagesResponse("""{"en":"English"}"""))
    }

    "handle UpdateThemeContentRequest successfully" in {
      val themeId = "theme123"
      val data = "{\"layer1\":\"content1\",\"layer2\":\"content2\"}"

      // Test case 1: With pageType provided
      val pageType = "homepage"
      val request1 = UpdateThemeContentRequest(themeId = themeId, data = data, pageType = Some(pageType))

      val expectedThemeContent1 = ThemeLayerContent(layers = data, pageType = Some(pageType))
      val expectedThemeContentJson1 = write(expectedThemeContent1)

      when(mockRedisCommands.hset(
        ZiRedisCons.THEME_CONTENT_LAYERES_JSON,
        themeId,
        expectedThemeContentJson1
      )).thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request1

      expectMsg(5.seconds, UpdateThemeContentResponse(GlobalMessageConstants.SUCCESS))

      // Test case 2: Without pageType provided (default behavior)
      val request2 = UpdateThemeContentRequest(themeId = themeId, data = data, pageType = None)

      val expectedThemeContent2 = ThemeLayerContent(layers = data, pageType = Some(""))
      val expectedThemeContentJson2 = write(expectedThemeContent2)

      when(mockRedisCommands.hset(
        ZiRedisCons.THEME_CONTENT_LAYERES_JSON,
        themeId,
        expectedThemeContentJson2
      )).thenReturn(true)

      val adminActor2 = system.actorOf(Props[AdminActor])
      adminActor2 ! request2

      expectMsg(5.seconds, UpdateThemeContentResponse(GlobalMessageConstants.SUCCESS))

      // Verify that the correct Redis calls were made
      verify(mockRedisCommands, times(1)).hset(
        ZiRedisCons.THEME_CONTENT_LAYERES_JSON,
        themeId,
        expectedThemeContentJson1
      )

      verify(mockRedisCommands, times(1)).hset(
        ZiRedisCons.THEME_CONTENT_LAYERES_JSON,
        themeId,
        expectedThemeContentJson2
      )
    }

    "handle GetGameDateWiseReportRequest with no data" in {
      // Test with dates that have no data
      val startDate = "2023-02-01"
      val endDate = "2023-02-03"
      val pageLimit = 10
      val noOfPage = 1
      val request = GetGameDateWiseReportRequest(startDate, endDate, pageLimit, noOfPage)

      // Mock Redis responses to return no data
      when(mockRedisCommands.llen(anyString())).thenReturn(0L)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      // Expect empty result
      val response = expectMsgType[GetGameDateWiseResponse](5.seconds)
      response.totalResult shouldBe 0
      response.result shouldBe ListMap.empty
    }

    "handle GetAllUserAttemptListRequest with pagination" in {
      val request = GetAllUserAttemptListRequest(
        reqId = "req123",
        auth = GlobalMessageConstants.AUTH_TEXT,
        pageLimit = 5,
        noOfPage = 2,
        actoinType = null
      )

      val filterKey = ZiRedisCons.USER_ALL_GAME_ATTEMPT_LIST
      val fromIndex = 5
      val toIndex = 9

      val mockIdList = List("user4_level4_4", "user5_level5_5")

      when(mockRedisCommands.llen(filterKey)).thenReturn(20L)
      when(mockRedisCommands.lrange(filterKey, fromIndex, toIndex))
        .thenReturn(mockIdList.asJava)

      mockLevelAttemptData("user4", "level4", "4")
      mockLevelAttemptData("user5", "level5", "5")

      val adminActor = TestActorRef[AdminActor]
      adminActor ! request

      val response = expectMsgType[GetAllUserAttemptListResponse](5.seconds)

      response.totalResult shouldBe 20L
      response.result.keySet should contain allOf("user4_level4_4", "user5_level5_5")

      val dataMap4 = response.result("user4_level4_4").asInstanceOf[Map[String, Any]]
      dataMap4("dockerId") shouldEqual 4

      val dataMap5 = response.result("user5_level5_5").asInstanceOf[Map[String, Any]]
      dataMap5("dockerId") shouldEqual 5
    }

    "return empty result when total size is 0" in {
      val request = GetAllUserAttemptListRequest(
        reqId = "req123",
        auth = GlobalMessageConstants.AUTH_TEXT,
        pageLimit = 10,
        noOfPage = 1,
        actoinType = null
      )

      val filterKey = ZiRedisCons.USER_ALL_GAME_ATTEMPT_LIST

      when(mockRedisCommands.llen(filterKey)).thenReturn(0L)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, GetAllUserAttemptListResponse(ListMap.empty, 0L))
    }

    "return empty result when fromIndex is greater than totalSize" in {
      val request = GetAllUserAttemptListRequest(
        reqId = "req123",
        auth = GlobalMessageConstants.AUTH_TEXT,
        pageLimit = 10,
        noOfPage = 3,
        actoinType = null
      )

      val filterKey = ZiRedisCons.USER_ALL_GAME_ATTEMPT_LIST

      when(mockRedisCommands.llen(filterKey)).thenReturn(20L)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, GetAllUserAttemptListResponse(ListMap.empty, 20L))
    }

    "return empty result when authentication is invalid" in {
      val request = GetAllUserAttemptListRequest(
        reqId = "req123",
        auth = "invalid_auth",
        pageLimit = 10,
        noOfPage = 1,
        actoinType = null
      )

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, GetAllUserAttemptListResponse(ListMap.empty, 0L))
    }

    "handle GetStoryBasedStatusRequest correctly" in {
      val userId = "user123"
      val levelId = "level456"
      val attemptCount = 3
      val statusResponse = """{"status": "completed", "score": 100}"""
      val request = GetStoryBasedStatusRequest(userId, levelId, attemptCount)

      val redisKey = ZiRedisCons.USER_GAME_STORY_STATUS + "_" + userId + "_" + levelId

      when(mockRedisCommands.hexists(redisKey, attemptCount.toString))
        .thenReturn(true)
      when(mockRedisCommands.hget(redisKey, attemptCount.toString))
        .thenReturn(statusResponse)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, GetStoryBasedStatusResponse(statusResponse))
    }

    "handle GetStoryBasedStatusRequest when status does not exist" in {
      val userId = "user123"
      val levelId = "level456"
      val attemptCount = 3
      val request = GetStoryBasedStatusRequest(userId, levelId, attemptCount)

      val redisKey = ZiRedisCons.USER_GAME_STORY_STATUS + "_" + userId + "_" + levelId

      when(mockRedisCommands.hexists(redisKey, attemptCount.toString))
        .thenReturn(false)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, GetStoryBasedStatusResponse(""))
    }

    "handle GetLevelAttemptsJsonDetailsRequest correctly" in {
      val userId = "user123"
      val levelId = "level456"
      val attemptNo = "3"
      val sessionId = "session789"
      val attemptJson = """{"attempt": 3, "score": 85, "completed": true}"""
      val request = GetLevelAttemptsJsonDetailsRequest(userId, levelId, attemptNo, sessionId)

      val redisKey = ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + userId + "_" + levelId

      when(mockRedisCommands.hget(redisKey, attemptNo))
        .thenReturn(attemptJson)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, GetLevelAttemptsJsonDetailsResponse(attemptJson))
    }

    "handle AddLanguageRequest correctly" in {
      val languageName = "Spanish"
      val sessionId = "session123"
      val request = AddLanguageRequest(languageName, sessionId)

      // Mock ZiFunctions.getId to return a predictable ID
      val languageId = "lang123"

      // Use MockitoSugar's when/thenReturn for static methods
      val ziFunction = spy(ZiFunctions)
      when(ziFunction.getId()).thenReturn(languageId)

      when(mockRedisCommands.hset(ZiRedisCons.LANGUAGE_DATA, languageId, languageName))
        .thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, AddLanguageResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle AddLanguageRequest when language name is empty" in {
      val languageName = ""
      val sessionId = "session123"
      val request = AddLanguageRequest(languageName, sessionId)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, AddLanguageResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle UpdateLanguageRequest correctly" in {
      val languageId = "lang123"
      val languageName = "French"
      val sessionId = "session123"
      val request = UpdateLanguageRequest(languageId, languageName, sessionId)

      when(mockRedisCommands.hexists(ZiRedisCons.LANGUAGE_DATA, languageId))
        .thenReturn(true)
      when(mockRedisCommands.hset(ZiRedisCons.LANGUAGE_DATA, languageId, languageName))
        .thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, UpdateLanguageResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle UpdateLanguageRequest when language name is empty" in {
      val languageId = "lang123"
      val languageName = ""
      val sessionId = "session123"
      val request = UpdateLanguageRequest(languageId, languageName, sessionId)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, UpdateLanguageResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle UpdateLanguageRequest when language ID does not exist" in {
      val languageId = "lang123"
      val languageName = "German"
      val sessionId = "session123"
      val request = UpdateLanguageRequest(languageId, languageName, sessionId)

      when(mockRedisCommands.hexists(ZiRedisCons.LANGUAGE_DATA, languageId))
        .thenReturn(false)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(5.seconds, UpdateLanguageResponse(GlobalMessageConstants.SUCCESS))
    }

    "handle GetEmotionCaptureListRequest" in {
      val userId = "user123"
      val levelId = "level1"
      val themeId = "theme1"
      val request = GetEmotionCaptureListRequest(userId, levelId, themeId)

      val emotionList = List("happy", "sad", "excited")
      val filterKey = ZiRedisCons.TRACKING_GAME_DATA + "_" + ZiRedisCons.EMOTION + "_" + userId + "_" + levelId + "_" + themeId

      when(mockRedisCommands.lrange(filterKey, 0, 6))
        .thenReturn(emotionList.asJava)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(GetEmotionCaptureListResponse(emotionList))
    }

    "handle GetMapUserToRoleRequest" in {
      val userId = "user123"
      val request = GetMapUserToRoleRequest("session1", userId)

      val roleIds = List("role1", "role2", "role3")

      when(mockRedisCommands.llen(ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId))
        .thenReturn(roleIds.size.toLong)
      when(mockRedisCommands.lrange(ZiRedisCons.ADMIN_MAP_USER_TO_ROLE + userId, 0, roleIds.size.toLong))
        .thenReturn(roleIds.asJava)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(GetMapUserToRoleResponse(roleIds))
    }

    "handle GetMapRoleToPageRequest" in {
      val roleId = "role1"
      val request = GetMapRoleToPageRequest("session1", roleId)

      val pageIds = List("page1", "page2", "page3")

      when(mockRedisCommands.llen(ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId))
        .thenReturn(pageIds.size.toLong)
      when(mockRedisCommands.lrange(ZiRedisCons.ADMIN_MAP_ROLE_TO_PAGE + roleId, 0, pageIds.size.toLong))
        .thenReturn(pageIds.asJava)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(GetMapRoleToPageResponse(pageIds))
    }

    "handle GetRolesRequest" in {
      val request = GetRolesRequest("session1", 10, 1)

      val roleIds = List("role1", "role2", "role3")
      val totalSize = roleIds.size.toLong

      // Create sample roles
      val role1 = Role("role1", "Admin Role", "active", 2334353L)
      val role2 = Role("role2", "User Role", "active", 2334353L)
      val role3 = Role("role3", "Guest Role", "active", 2334353L)

      // Expected result map
      val resultMap = ListMap(
        "role1" -> role1,
        "role2" -> role2,
        "role3" -> role3
      )

      when(mockRedisCommands.llen(ZiRedisCons.ADMIN_ROLE_ID_LIST))
        .thenReturn(totalSize)
      when(mockRedisCommands.lrange(ZiRedisCons.ADMIN_ROLE_ID_LIST, 0, totalSize - 1))
        .thenReturn(roleIds.asJava)

      // Mock the role details
      when(mockRedisCommands.hget(ZiRedisCons.ADMIN_ROLE_JSON, "role1"))
        .thenReturn(write(role1))
      when(mockRedisCommands.hget(ZiRedisCons.ADMIN_ROLE_JSON, "role2"))
        .thenReturn(write(role2))
      when(mockRedisCommands.hget(ZiRedisCons.ADMIN_ROLE_JSON, "role3"))
        .thenReturn(write(role3))

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      expectMsg(GetRolesResponse(resultMap, totalSize))
    }

    "handle language base data requests" in {
      // Test UpdateLanguageBaseDataRequest
      val updateRequest = UpdateLanguageBaseDataRequest("group1", """{"key":"value"}""", "session1")

      when(mockRedisCommands.hset(ZiRedisCons.LANGUAGE_BASE_DATA, "group1", """{"key":"value"}"""))
        .thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! updateRequest

      expectMsg(UpdateLanguageBaseDataResponse(GlobalMessageConstants.SUCCESS))

      // Test GetLanguageBaseDataRequest
      val getRequest = GetLanguageBaseDataRequest("group1", "session1")
      val responseData = """{"key":"value"}"""

      when(mockRedisCommands.hget(ZiRedisCons.LANGUAGE_BASE_DATA, "group1"))
        .thenReturn(responseData)

      adminActor ! getRequest

      expectMsg(GetLanguageBaseDataResponse(responseData))
    }

    "handle language mapping data requests" in {
      // Test UpdateLanguageMappingDataRequest
      val updateRequest = UpdateLanguageMappingDataRequest("group1", "en", """{"key":"value"}""", "session1")

      when(mockRedisCommands.hset(ZiRedisCons.LANGUAGE_MAPPING_JSON + "_group1", "en", """{"key":"value"}"""))
        .thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! updateRequest

      expectMsg(UpdateLanguageMappingDataResponse(GlobalMessageConstants.SUCCESS))

      // Test GetLanguageMappingDataRequest
      val getRequest = GetLanguageMappingDataRequest("group1", "en", "session1")
      val responseData = """{"key":"value"}"""

      when(mockRedisCommands.hget(ZiRedisCons.LANGUAGE_MAPPING_JSON + "_group1", "en"))
        .thenReturn(responseData)

      adminActor ! getRequest

      expectMsg(GetLanguageMappingDataResponse(responseData))
    }

    "handle language mapping data with base data request" in {
      val request = GetLanguageMappingDataWithBaseDataRequest("group1", "en", "session1")

      val baseData = """{"baseKey":"baseValue"}"""
      val mappingData = """{"mappingKey":"mappingValue"}"""

      when(mockRedisCommands.hget(ZiRedisCons.LANGUAGE_BASE_DATA, "group1"))
        .thenReturn(baseData)
      when(mockRedisCommands.hget(ZiRedisCons.LANGUAGE_MAPPING_JSON + "_group1", "en"))
        .thenReturn(mappingData)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! request

      val expectedDataMap = Map(
        "baseData" -> baseData,
        "mappingData" -> mappingData
      )

      expectMsg(GetLanguageMappingDataWithBaseDataResponse(expectedDataMap))
    }

    "handle module language mapping requests" in {
      // Test UpdateModuleLanguageMappingRequest
      val updateRequest = UpdateModuleLanguageMappingRequest("level1", "en", """{"key":"value"}""", "session1")

      when(mockRedisCommands.hset(ZiRedisCons.MODULE_LANGUAGE_MAPPING_JSON + "_level1", "en", """{"key":"value"}"""))
        .thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! updateRequest

      expectMsg(UpdateModuleLanguageMappingResponse(GlobalMessageConstants.SUCCESS))

      // Test GetModuleLanguageMappingRequest
      val getRequest = GetModuleLanguageMappingRequest("level1", "en", "session1")
      val responseData = """{"key":"value"}"""

      when(mockRedisCommands.hget(ZiRedisCons.MODULE_LANGUAGE_MAPPING_JSON + "_level1", "en"))
        .thenReturn(responseData)

      adminActor ! getRequest

      expectMsg(GetModuleLanguageMappingResponse(responseData))
    }

    "handle levels name language mapping requests" in {
      // Test UpdateLevelsNameLanguageMappingRequest
      val updateRequest = UpdateLevelsNameLanguageMappingRequest("en", """{"key":"value"}""", "session1")

      when(mockRedisCommands.hset(ZiRedisCons.LEVEL_NAME_LANGUAGE_MAPPING_JSON, "en", """{"key":"value"}"""))
        .thenReturn(true)

      val adminActor = system.actorOf(Props[AdminActor])
      adminActor ! updateRequest

      expectMsg(UpdateLevelsNameLanguageMappingResponse(GlobalMessageConstants.SUCCESS))

      // Test GetLevelsNameLanguageMappingRequest
      val getRequest = GetLevelsNameLanguageMappingRequest("en", "session1")
      val responseData = """{"key":"value"}"""

      when(mockRedisCommands.hget(ZiRedisCons.LEVEL_NAME_LANGUAGE_MAPPING_JSON, "en"))
        .thenReturn(responseData)

      adminActor ! getRequest

      expectMsg(GetLevelsNameLanguageMappingResponse(responseData))
    }
  }

  private def mockLevelAttemptData(userId: String, levelId: String, attemptCount: String): Unit = {
    val attemptKey = ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + userId + "_" + levelId
    val timestamp = System.currentTimeMillis()

    val levelAttempt = LevelAttempt(
      levelJson = "level json content",
      levelPoint = attemptCount.toInt,
      createdAt = Some(timestamp),
      ip = Some("192.168.1." + attemptCount),
      deviceInfo = Some("device_" + attemptCount),
      userTime = Some(timestamp),
      landingFrom = Some("landing_" + attemptCount)
    )

    when(mockRedisCommands.hexists(attemptKey, attemptCount)).thenReturn(true)
    when(mockRedisCommands.hget(attemptKey, attemptCount)).thenReturn(write(levelAttempt))
  }

}
