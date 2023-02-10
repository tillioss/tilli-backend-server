

package com.teqbahn.actors.excel


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

import com.fasterxml.jackson.databind.{JsonNode}
import com.fasterxml.jackson.databind.node.ArrayNode
import java.sql.Timestamp
import scala.collection.immutable.ListMap
import org.joda.time.{DateTime, Days}
import scala.collection.mutable.ListBuffer
import java.io.{FileOutputStream}
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.fasterxml.jackson.databind.ObjectMapper







class GenerateExcel extends Actor {
  implicit val formats = DefaultFormats
  import scala.collection.JavaConverters._


    def receive = {
      case gameCsvFileGenrateRequest : GameCsvFileGenrateRequest =>
       
       var userFolderPath= StarterMain.fileSystemPath + "/excel/"+gameCsvFileGenrateRequest.userId
        var path1 = userFolderPath
        var fileName = gameCsvFileGenrateRequest.userId+".xls"
        var fileOutPutPath = path1+"/"+fileName 
        val createdAt = new Timestamp((new Date).getTime).getTime
        var folderPathExist = new java.io.File(userFolderPath).exists
        if(folderPathExist)
        {
        val name : String = ".+\\.xls"
        val files = getListOfFiles(userFolderPath)
        .map(f => f.getName)
        .filter(_.matches(name))
              
        if(files.length > 0)
        {      
          files.foreach((element:String) => {
            var xlsFilePath = userFolderPath+"/"+element
            StarterMain.deleteFilePath(xlsFilePath)    
          })
          
        }
      
        }

      
        StarterMain.createDir(path1)    
        val fileOutputStream = new FileOutputStream(new File(fileOutPutPath))
        val workbook = new HSSFWorkbook
        val sheet = workbook.createSheet("GenerateExcel")       

        var  userLevelComplete = redisCommands.hget(ZiRedisCons.USER_GAME_STATUS_JSON, gameCsvFileGenrateRequest.userId)
        if(userLevelComplete !=null)
        {
    
        var userGameStatus: UserGameStatus = read[UserGameStatus](userLevelComplete)     
        val userDataStr = redisCommands.hget(ZiRedisCons.USER_JSON, gameCsvFileGenrateRequest.userId)
        val userData = read[User](userDataStr)
        var userChildAge="-"
      
        if(userData.genderOfChild !=None){
          userChildAge=userData.genderOfChild.get
        }
        var rownum = 0
        rownum = rownum + 1  

        var row = sheet.createRow(rownum) 
        var colnums=0

        var browserCellIndex=0
        var browserRowIndex=row
        var fromCellIndex=0
        var fromRowIndex=row
        var osCellIndex=0
        var osRowIndex=row



        var cells = row.createCell(colnums)
        cells.setCellValue("UserId")
        
        colnums +=1
        cells = row.createCell(colnums)
        cells.setCellValue(gameCsvFileGenrateRequest.userId.toString)

        rownum = rownum + 1  
        row = sheet.createRow(rownum)    
        colnums=0  
        cells = row.createCell(colnums);
        cells.setCellValue("OS")
        
        colnums +=1    
        osCellIndex=colnums
        osRowIndex=row
      
        
        rownum = rownum + 1  
        row = sheet.createRow(rownum)    
        colnums=0  
        cells = row.createCell(colnums);
        cells.setCellValue("Broser")
        
        colnums +=1  
        browserCellIndex =colnums 
        browserRowIndex=row
        
      
        rownum = rownum + 1 
        row = sheet.createRow(rownum)  
        colnums=0  
        cells = row.createCell(colnums)    
        cells.setCellValue("From")    
            
        colnums +=1
        fromCellIndex=colnums
        fromRowIndex=row
        cells = fromRowIndex.createCell(fromCellIndex)
        cells.setCellValue("-")
          
          
        rownum = rownum + 1  
        row = sheet.createRow(rownum)    
        colnums=0  
        cells = row.createCell(colnums);
        cells.setCellValue("Age")
        
        colnums +=1    
        cells = row.createCell(colnums);
        cells.setCellValue(userData.ageOfChild.toString) 

        rownum = rownum + 1 
        row = sheet.createRow(rownum)  
        colnums=0  
        cells = row.createCell(colnums)    
        cells.setCellValue("Gender")    
            
        colnums +=1
        cells = row.createCell(colnums)
        cells.setCellValue(userChildAge)

        rownum = rownum + 1 
        row = sheet.createRow(rownum)  
        colnums=0  
        cells = row.createCell(colnums)    
        cells.setCellValue("Total Module Completed")    
            
        colnums +=1
        cells = row.createCell(colnums)
        cells.setCellValue(userGameStatus.level.toString)

        rownum = rownum + 1 
        row = sheet.createRow(rownum)  
        colnums=0  
        cells = row.createCell(colnums)    
        cells.setCellValue("Trust Points")    
            
        colnums +=1
        cells = row.createCell(colnums)
        cells.setCellValue("0")

        rownum = rownum + 2  
        row = sheet.createRow(rownum)


        var headerColnum = 0;
        var headerCell = row.createCell(headerColnum);
        headerCell.setCellValue("Date")

        headerColnum += 1
        headerCell = row.createCell(headerColnum);
        headerCell.setCellValue("Level Name")

        headerColnum += 1
        headerCell = row.createCell(headerColnum);
        headerCell.setCellValue("Theme Name")

        headerColnum += 1
        headerCell = row.createCell(headerColnum);
        headerCell.setCellValue("Status")

        headerColnum += 1
        headerCell = row.createCell(headerColnum);
        headerCell.setCellValue("User Action")

        headerColnum += 1
        headerCell = row.createCell(headerColnum);
        headerCell.setCellValue("Attempt")

        headerColnum += 1
        headerCell = row.createCell(headerColnum);
        headerCell.setCellValue("Module Points")

        headerColnum += 1
        headerCell = row.createCell(headerColnum);
        headerCell.setCellValue("Start Time")

        headerColnum += 1
        headerCell = row.createCell(headerColnum);
        headerCell.setCellValue("End Time")  

        val startDate = gameCsvFileGenrateRequest.startDate
        val endDate = gameCsvFileGenrateRequest.endDate

        val sDate = DateTime.parse(startDate)
        val eDate = DateTime.parse(endDate)
        println("sDate-->"+sDate+"-->eDate"+eDate)
        val daysCount = Days.daysBetween(sDate, eDate).getDays() + 1
        println("daysCount-->"+daysCount)
        var getDeviceInfo=""
        var getLandingForm="-"

        (0 until daysCount).map(sDate.plusDays(_)).foreach(d => {
        val dateStr = getDateStr(d) 
        var attemptMap: Map[String, LevelAttemptObject] = Map.empty
        var totalSize: Long = 0
        var filterkey = ZiRedisCons.USER_DATE_WISE_ATTEMPT_DATA_LIST+"_"+gameCsvFileGenrateRequest.userId+"_"+dateStr       
          val fromIndex = 0;
          totalSize = redisCommands.llen(filterkey)      
          if (totalSize > 0) {
            if (totalSize > fromIndex) {
              val lisOfIds = redisCommands.lrange(filterkey, fromIndex,- 1).asScala.toList
              for (idStr <- lisOfIds) {
                  var idArr = idStr.split("_")
                  if (idArr.length > 2) {
                    var userId = idArr(0)
                    var levelId = idArr(1)
                    var attemptCount = idArr(2)                            
                    if (redisCommands.hexists(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + userId + "_" + levelId, attemptCount)) {
                      var attemptJsonStr = redisCommands.hget(ZiRedisCons.USER_GAME_ATTEMPT_JSON + "_" + userId + "_" + levelId, attemptCount)
                      val levelAttempt: LevelAttempt = read[LevelAttempt](attemptJsonStr)

                      if(getDeviceInfo.length == 0 )
                      {
                        var deviceInfo=levelAttempt.deviceInfo
                        if( deviceInfo !=null && deviceInfo !=None){
                          getDeviceInfo=deviceInfo.get
                          var osInformation=getOSDetails(getDeviceInfo)
                          var browserInformation=getBrowserDetails(getDeviceInfo)

                          cells = browserRowIndex.createCell(browserCellIndex);
                          cells.setCellValue(browserInformation) 

                          cells = osRowIndex.createCell(osCellIndex);
                          cells.setCellValue(osInformation)  
                          
                          
                        }
                        
                      }

                      if(getLandingForm.length == 1)
                      {
                        var readLandingForm=levelAttempt.landingFrom                   
                        if(readLandingForm !=null && readLandingForm !=None)
                        {
                        getLandingForm =readLandingForm.get
                        if(getLandingForm.length > 0 )
                        {
                        cells = fromRowIndex.createCell(fromCellIndex)
                        cells.setCellValue(getLandingForm)
                        }
                      
                        }
                      }

                      var levelName = ""
                      var levelJsonData=levelAttempt.levelJson

                      if (redisCommands.hexists(ZiRedisCons.LEVEL_JSON, levelId)) {
                          var levelDataStr = redisCommands.hget(ZiRedisCons.LEVEL_JSON, levelId)
                          val levelData: GameLevel = read[GameLevel](levelDataStr)
                          levelName = levelData.name
                      } 

                      var objectMapper: ObjectMapper = new ObjectMapper()
                      var levlJsonNode: JsonNode = objectMapper.readTree(levelJsonData)
                      var getDynamicData=levlJsonNode.get("dynamic")
                      var gameST=""
                      var gameET="-"                  
                      if(getDynamicData !=null && getDynamicData!=None){                  
                      var gameCompleteStatus=""
                      if(levlJsonNode.has("status"))
                      {
                        gameCompleteStatus=levlJsonNode.get("status").textValue()
                      }                  
                      var gameStartTime=levlJsonNode.get("startTime")
                      var gameStartEndTime=levlJsonNode.get("endTime")
                      gameST = gameStartTime.toString()                                   

                      var getDynamicThemes=getDynamicData.get("dynamicThemes")
                      var dynamicThemesCopy= jsonStrToMap(getDynamicThemes.toString)
                      var copyDynamicForLoop : Map[String,Any]=dynamicThemesCopy.asInstanceOf[Map[String,Any]] 
                      
                        var loopIndex = 0;
                        var colnum = 0; 
                        for( loopIndex <- 1 to copyDynamicForLoop.size.toInt){
                          var decrementIndex  = loopIndex -1
                          var currentThemeData=getDynamicThemes.get(decrementIndex.toString)
                          var themeName= currentThemeData.get("themeName").textValue()
                          var getUserActionText= currentThemeData.get("userActionText")
                          var userActionTextString="-"

                          if(decrementIndex == 0)
                          {
                          gameST=timeStampToDateAndTime(gameST)
                                                  
                          }else{
                            gameST="-"
                          }

                          if(loopIndex == copyDynamicForLoop.size.toInt)
                          {
                          gameET=gameStartEndTime.toString()
                          gameET=timeStampToDateAndTime(gameET)
                          }else
                          {
                            gameET="-"
                          }

                          if(getUserActionText!=null && getUserActionText!=None)
                          {
                            userActionTextString=getUserActionText.textValue()
                          }
                          
                          rownum = rownum + 1                                                     
                          val row = sheet.createRow(rownum);
                          var cell = row.createCell(colnum);
                          cell.setCellValue(dateStr.toString)
                          colnum += 1 
                          
                          cell = row.createCell(colnum);
                          cell.setCellValue(levelName)
                          colnum += 1 

                          
                          cell = row.createCell(colnum);
                          cell.setCellValue(themeName.toString) 
                          colnum += 1 

                          
                          cell = row.createCell(colnum);
                          cell.setCellValue(gameCompleteStatus) 
                          colnum += 1 

                          
                          cell = row.createCell(colnum);
                          cell.setCellValue(userActionTextString)
                          colnum += 1 

                          
                          cell = row.createCell(colnum);
                          cell.setCellValue(attemptCount.toString)
                          colnum += 1 

                          
                          cell = row.createCell(colnum);
                          cell.setCellValue("-")
                          colnum += 1 

                          
                          cell = row.createCell(colnum);
                          cell.setCellValue(gameST.toString)
                          colnum += 1 
                          
                          cell = row.createCell(colnum);
                          cell.setCellValue(gameET.toString)
                          colnum += 1
                          colnum=0
                                                                        
                        }                   

                      }else{
                      
                      var gameStartTime=levlJsonNode.get("startTime")
                      var gameStartEndTime=levlJsonNode.get("endTime")
                      if(levlJsonNode.has("stages"))
                      {
                      var getStaticThems = levlJsonNode.get("stages")
                      //  println("getStaticThems",getStaticThems.size)
                      var storyPointCalculate = 0
                      
                      var storyIndex = 0
                      var loopIndex = 0
                      for( loopIndex <- 1 to getStaticThems.size){ 
                        var innerIndex = loopIndex-1
                        var currentThemeData = getStaticThems.get(innerIndex)
                        var themeName = currentThemeData.get("theme").textValue()
                        if(themeName == "StoryCard"){
                          // println("Index ->"+loopIndex+"<--themeName -->"+themeName)
                          storyIndex = storyIndex + 1
                          var storyTextShow= "Story "+storyIndex.toString 
                          if(currentThemeData.get("content") != null)
                          {
                            /*
                            if(currentThemeData.get("storyPoints") != null)
                            {
                              storyPointCalculate = storyPointCalculate.toInt + currentThemeData.get("storyPoints").toString().toInt
                            }*/

                            var contentDataSize = currentThemeData.get("content").size
                            var colnum = 0
                            var contentLoopIndex = 0 
                            for( contentLoopIndex <- 1 to contentDataSize){ 
                                var contentInnerIndex = contentLoopIndex-1
                                var storyContentData = currentThemeData.get("content")
                                var storyThemeData = storyContentData.get(contentInnerIndex)
                                var stroyThemeName =   storyThemeData.get("theme").textValue()  
                                var stroyInsideGetContent = storyThemeData.get("content")  
                                                            
                                gameST = "-"
                                gameET = "-"
                              
                                if(currentThemeData.get("endTime") != null && contentLoopIndex == contentDataSize && getStaticThems.size == loopIndex)
                                {
                                  gameET = timeStampToDateAndTime(currentThemeData.get("endTime").toString)                             
                                }else{
                                  gameET = "-"
                                }
                                                                                                                                      

                                if(stroyThemeName == "AudioQuizScreen")
                                {
                                  var audioLoopIndex = 0                                    
                                  var audioQuizeLength = stroyInsideGetContent.get("feelingsDataList").size
                                  for( audioLoopIndex <- 1 to audioQuizeLength){ 
                                    var audioInnerIndex = audioLoopIndex-1                               
                                    var contentQuestion = stroyInsideGetContent.get("feelingsDataList").get(audioInnerIndex)                                  
                                      var qusTxt = "-"
                                      var ansTxt = "-"
                                      if(contentQuestion.get("questionText") != null)
                                      { 
                                      var audioQuestionText =  contentQuestion.get("questionText").textValue()   
                                      if(audioQuestionText != null){
                                        qusTxt = audioQuestionText 
                                      }                                                                                                
                                      }
                                      if(contentQuestion.get("results") != null){
                                      var audioAnswerText =  contentQuestion.get("results").textValue()  
                                      if(audioAnswerText != null)
                                      {
                                        ansTxt = audioAnswerText
                                      }  
                                                                  
                                      }

                                      if(currentThemeData.get("startTime") != null && storyIndex == 1 && audioInnerIndex == 0)
                                      {                                
                                      
                                      gameST = timeStampToDateAndTime(currentThemeData.get("startTime").toString)
                                      
                                      }
                                      else
                                      {
                                      gameST = "-"
                                      }

                                      rownum = rownum + 1                                                     
                                      val row = sheet.createRow(rownum);
                                      var cell = row.createCell(colnum);
                                      cell.setCellValue(dateStr.toString)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(levelName)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(qusTxt) 
                                      colnum += 1 

                                      cell = row.createCell(colnum);
                                      cell.setCellValue("Complete") 
                                      colnum += 1 

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(ansTxt) 
                                      colnum += 1 
                                    

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(attemptCount.toString)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1 

                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(gameST)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1
                                      colnum = 0

                                  }
                                }
                                if(stroyThemeName == "DropToSelection")
                                {
                                  
                                  if(stroyInsideGetContent.get("chooseAnswer") != null && stroyInsideGetContent.get("circleSelect") != null)
                                  {
                                    var chooseAnswer = stroyInsideGetContent.get("chooseAnswer").textValue() 
                                    var circleSelect = stroyInsideGetContent.get("circleSelect").textValue() 
                                    if(chooseAnswer != null && circleSelect != null)
                                    {                        
                                      var storyScore = "-"
                                      if(currentThemeData.get("storyPoints") != null)
                                      {
                                        storyScore = currentThemeData.get("storyPoints").toString()
                                      }

                                      var correctAnsTxt = "-"
                                      var wrongAnsTxt = "-"
                                      var completeStatusCorrect = "-"
                                      var completeStatusWrong = "-"
                                      if(chooseAnswer == "Correct"){
                                        correctAnsTxt = "Correct"
                                        wrongAnsTxt = "-"
                                        completeStatusCorrect ="Complete"
                                        }
                                        else{
                                          correctAnsTxt = "-"
                                          wrongAnsTxt = "Wrong"
                                          completeStatusWrong ="Complete"
                                        }

                                    /*circle choose*/
                                      rownum = rownum + 1       
                                      var row = sheet.createRow(rownum);
                                      var cell = row.createCell(colnum);
                                      cell.setCellValue(dateStr.toString)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(levelName)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(storyTextShow +" Trust Circle") 
                                      colnum += 1 

                                      cell = row.createCell(colnum);
                                      cell.setCellValue("Complete") 
                                      colnum += 1 

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(circleSelect) 
                                      colnum += 1 
                                    

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(attemptCount.toString)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1 

                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1
                                      colnum = 0

                                    /*circle choose*/

                                    /*circle score*/

                                      rownum = rownum + 1       
                                      row = sheet.createRow(rownum);
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(dateStr.toString)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(levelName)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(storyTextShow +" Trust Circle Score") 
                                      colnum += 1 

                                      cell = row.createCell(colnum);
                                      cell.setCellValue("Complete") 
                                      colnum += 1 

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(storyScore) 
                                      colnum += 1 
                                    

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(attemptCount.toString)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1 

                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1
                                      colnum = 0

                                    /*circle score*/
                                    /*circle choose type*/
                                      rownum = rownum + 1       
                                      row = sheet.createRow(rownum);
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(dateStr.toString)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(levelName)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("Correct") 
                                      colnum += 1 

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(completeStatusCorrect) 
                                      colnum += 1 

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(correctAnsTxt) 
                                      colnum += 1 
                                    

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(attemptCount.toString)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1 

                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1
                                      colnum = 0


                                      rownum = rownum + 1       
                                      row = sheet.createRow(rownum);
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(dateStr.toString)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(levelName)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("Wrong") 
                                      colnum += 1 

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(completeStatusWrong) 
                                      colnum += 1 

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(wrongAnsTxt) 
                                      colnum += 1 
                                    

                                      cell = row.createCell(colnum);
                                      cell.setCellValue(attemptCount.toString)
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1 

                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue("-")
                                      colnum += 1 
                                      
                                      cell = row.createCell(colnum);
                                      cell.setCellValue(gameET.toString)
                                      colnum += 1
                                      colnum = 0
                                    /*circle choose type*/


                                    }                              

                                  }                                                                 
                                    
                                }
                                                                                        

                            }                      

                          }
                        }                    

                      }               

                      }
                      
                      }
            

                    }              

                }

              }
            }
          }

        })
      

        workbook.write(fileOutputStream)         
        workbook.close()

        println("path-->"+fileOutPutPath)
        var excelFileCheck = StarterMain.fileExistCheck(fileOutPutPath)
        println("**excelFileCheck***-->"+excelFileCheck)
        if(excelFileCheck)
        {
          
            val excelFileProcessData = redisCommands.hget(ZiRedisCons.USER_EXCEL_SHEET_STATUS +"_"+ gameCsvFileGenrateRequest.userId, gameCsvFileGenrateRequest.userId)
            if(checkIsNotEmpty(excelFileProcessData))
            {
              val data = read[ExcelSheetGenerateStatus](excelFileProcessData)
              var data_New=data.copy(processStatus = GlobalMessageConstants.COMPLETED,userId = gameCsvFileGenrateRequest.userId,createdAt = createdAt)
              println("createdAt 2-->"+createdAt)
              redisCommands.hset(ZiRedisCons.USER_EXCEL_SHEET_STATUS +"_"+gameCsvFileGenrateRequest.userId, gameCsvFileGenrateRequest.userId, write(data_New))

            }

        }

          
        }

    case _ => println("Unknown message")
}


 def jsonStrToMap(jsonStr: String): Map[String, Any] = {
    implicit val formats = org.json4s.DefaultFormats
    parse(jsonStr).extract[Map[String, Any]]
  }
  
    def getDateStr(d: DateTime): String = {
    d.getYear + "-" +getDoubeDigit(d.getMonthOfYear) + "-" + getDoubeDigit(d.getDayOfMonth)
  }
  
  def getDoubeDigit(d: Int): String = {
    "%02d".format(d)
  }

  def timeStampToDateAndTime(d: String): String = {
      var dateAndTime=""
      var ds=d.toLong
      val sDate = new DateTime(ds) 
      var currentYear=sDate.getYear()
      var currentMonth=getDoubeDigit(sDate.getMonthOfYear())
      var currentDay=getDoubeDigit(sDate.getDayOfMonth())
      var currentHour=sDate.getHourOfDay() 
      var currentMinute=getDoubeDigit(sDate.getMinuteOfHour())
      var currentMilliSeconds=getDoubeDigit(sDate.getSecondOfMinute())
      var typeOfExt="AM" 
      if(currentHour >= 12)
      {
      typeOfExt= "PM"
        if(currentHour > 12)
        {
          currentHour=(currentHour.toInt-12)
        }    
      }
      dateAndTime=currentYear+"-"+currentMonth+"-"+currentDay+","+currentHour +":"+currentMinute+":"+currentMilliSeconds+" "+typeOfExt
      
      dateAndTime
  }

   def getOSDetails(userAgent: String): String = {
        var os="";
        if (userAgent.toLowerCase.indexOf("windows") >= 0) {
            os = "Windows"
        } else if (userAgent.toLowerCase.indexOf("mac") >= 0) {
            os = "Mac"
        } else if (userAgent.toLowerCase.indexOf("x11") >= 0) {
            os = "Unix"
        } else if (userAgent.toLowerCase.indexOf("android") >= 0) {
            os = "Android"
        } else if (userAgent.toLowerCase.indexOf("iphone") >= 0) {
            os = "IPhone"
        } else {
            os = "UnKnown, More-Info: " + userAgent
        }

         os
    }

    def getBrowserDetails(userAgent: String): String ={

        var user = userAgent.toLowerCase;
        var browser=""        
        if (user.contains("msie")) {
            var substring = userAgent.substring(userAgent.indexOf("MSIE")).split(";")(0)
            browser = substring.split("\\s+")(0).replace("MSIE", "IE") + "-" + substring.split("\\s+")(1)
        } else if (user.contains("safari") && user.contains("version")) {
            browser = (userAgent.substring(userAgent.indexOf("Safari")).split("\\s+")(0)).split("/")(0) + "-" + (userAgent.substring(userAgent.indexOf("Version")).split("\\s+")(0)).split("/")(1)
        } else if (user.contains("opr") || user.contains("opera")) {
            if (user.contains("opera")){
            browser = (userAgent.substring(userAgent.indexOf("Opera")).split("\\s+")(0)).split("/")(0) + "-" + (userAgent.substring(userAgent.indexOf("Version")).split("\\s+")(0)).split("/")(1)
            }         
            else if (user.contains("opr"))
            {
            browser = ((userAgent.substring(userAgent.indexOf("OPR")).split("\\s+")(0)).replace("/", "-")).replace("OPR", "Opera")
            }               
        } else if (user.contains("chrome")) {            
            browser = (userAgent.substring(userAgent.indexOf("Chrome")).split("\\s+")(0)).replace("/", "-");
        } else if ((user.indexOf("mozilla/7.0") > -1) || (user.indexOf("netscape6") != -1) || (user.indexOf("mozilla/4.7") != -1) || (user.indexOf("mozilla/4.78") != -1) || (user.indexOf("mozilla/4.08") != -1) || (user.indexOf("mozilla/3") != -1)) {
            browser = "Netscape-?"

        } else if (user.contains("firefox")) {
            browser = (userAgent.substring(userAgent.indexOf("Firefox")).split("\\s+")(0)).replace("/", "-")
        } else if (user.contains("rv")) {
            browser = "IE-" + user.substring(user.indexOf("rv") + 3, user.indexOf(")"))
        } else {
            browser = "UnKnown, More-Info: " + userAgent
        }
         browser
    }

    def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
        d.listFiles.filter(_.isFile).toList
    } else {
        List[File]()
    }

   }

   def checkIsNotEmpty(text: String): Boolean = {
    var bool = false
    if (text != null && !text.isEmpty) {
      bool = true
    }
    bool
  }


  }