
package com.teqbahn.utils

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

class ZiFunctionsSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  implicit val system: ActorSystem = ActorSystem("ZiFunctionsSpec")

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "getId" should "return a non-empty string" in {
    val id = ZiFunctions.getId()
    id should not be empty
  }

  it should "return a valid UUID string" in {
    val id = ZiFunctions.getId()
    noException should be thrownBy UUID.fromString(id)
  }

  it should "return unique values on subsequent calls" in {
    val id1 = ZiFunctions.getId()
    val id2 = ZiFunctions.getId()
    id1 should not equal id2
  }

  "getCreatedAt" should "return a timestamp greater than 0" in {
    val createdAt = ZiFunctions.getCreatedAt()
    createdAt should be > 0L
  }

  it should "return a timestamp close to current time" in {
    val before = System.currentTimeMillis()
    val createdAt = ZiFunctions.getCreatedAt()
    val after = System.currentTimeMillis()

    createdAt should be >= before
    createdAt should be <= after
  }

  "printNodeInfo" should "not throw exceptions" in {
    val probe = TestProbe()
    noException should be thrownBy ZiFunctions.printNodeInfo(probe.ref, "test message")
  }

  "jsonStrToMap" should "convert valid JSON string to Map" in {
    val jsonStr = """{"name":"John", "age":30, "city":"New York"}"""
    val result = ZiFunctions.jsonStrToMap(jsonStr)

    result should be (Map("name" -> "John", "age" -> 30, "city" -> "New York"))
  }

  it should "handle nested JSON objects" in {
    val jsonStr = """{"name":"John", "address":{"street":"Main St", "city":"New York"}}"""
    val result = ZiFunctions.jsonStrToMap(jsonStr)

    result should contain key "address"
    result("address").asInstanceOf[Map[String, Any]] should contain key "street"
    result("address").asInstanceOf[Map[String, Any]]("street") should be ("Main St")
  }

  it should "handle JSON arrays" in {
    val jsonStr = """{"name":"John", "hobbies":["reading", "coding"]}"""
    val result = ZiFunctions.jsonStrToMap(jsonStr)

    result should contain key "hobbies"
    result("hobbies").asInstanceOf[List[String]] should contain allOf ("reading", "coding")
  }
}
