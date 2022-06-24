package com.teqbahn.caseclasses

case class User(userId: String, emailId: String, name: String, password: String, nameOfChild: String, ageOfChild: String, passcode: String, status: String, lastLogin: Option[Long] = None, lastLogout: Option[Long] = None, zipcode: Option[String] = None, genderOfChild: Option[String] = None, createdAt: Option[Long] = None, ip: Option[String] = None, deviceInfo: Option[String] = None) extends Request

case class UserLoginCredential(userId: String, status: String, password: String) extends Request

case class UserGameStatus(points: Integer, feelingTool: Integer, level: Integer)

case class LevelAttempt(levelJson: String, levelPoint: Integer, createdAt: Option[Long] = None, ip: Option[String] = None, deviceInfo: Option[String] = None, userTime: Option[Long] = None, landingFrom: Option[String] = None)

case class ShortUserInfo(userId: String, emailId: String, name: String, nameOfChild: String, ageOfChild: String, status: String, lastLogin: Option[Long] = None, lastLogout: Option[Long] = None, genderOfChild: Option[String] = None, createdAt: Option[Long] = None)

case class ShortAdminInfo(userId: String, emailId: String, name: String, status: String, createdAt: Option[Long] = None)

case class ForgotOtp(userId: String, id: String, email: String, otp: String, createdAt: Long) extends Request