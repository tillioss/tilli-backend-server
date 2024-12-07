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
  }
} 