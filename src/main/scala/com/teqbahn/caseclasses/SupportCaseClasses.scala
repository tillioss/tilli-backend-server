package com.teqbahn.caseclasses

import com.teqbahn.global.{GlobalConstants}

case class GameLevel(id: String, name: String, image: GameFileObject, color: String, sortOrder: Integer)

case class Theme(id: String, name: String, image: GameFileObject, themeType: Option[String] = Option(GlobalConstants.STATIC), gameFile:Option[FileObject]= None)

case class GameFileObject(id: String, title: String, fileName: String, fileType: String)

case class FileObject(processType: String,fileName: String,fileType: String,origFileName:String)

case class ExcelSheetGenerateStatus(userId: String,createdAt: Long,processStatus : String)

case class FeedBackCaptureData(activity: String,count: Int)
