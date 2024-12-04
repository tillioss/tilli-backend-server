package com.teqbahn.utils

import java.util.{Date, UUID}
import org.apache.pekko.actor.ActorRef
import org.json4s.jackson.JsonMethods.parse

import java.sql.Timestamp

object ZiFunctions {
  def getId(): String = {
    return UUID.randomUUID.toString
  }

  def printNodeInfo(self : ActorRef, msg : String): Unit = {
  }
  def getCreatedAt(): Long = {
    new Timestamp((new Date).getTime).getTime
  }

  def jsonStrToMap(jsonStr: String): Map[String, Any] = {
    implicit val formats = org.json4s.DefaultFormats
    parse(jsonStr).extract[Map[String, Any]]
  }
}
