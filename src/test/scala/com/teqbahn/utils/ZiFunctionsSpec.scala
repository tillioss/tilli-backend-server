package com.teqbahn.utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ZiFunctionsSpec extends AnyFlatSpec with Matchers {

  "getId" should "return a non-empty string" in {
    val id = ZiFunctions.getId()
    id should not be empty
  }

  "getCreatedAt" should "return a timestamp greater than 0" in {
    val createdAt = ZiFunctions.getCreatedAt()
    createdAt should be > 0L
  }
}
