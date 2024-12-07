package com.teqbahn.global

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GlobalConstantsSpec extends AnyWordSpec with Matchers {
  
  "GlobalConstants" should {
    "have correct status constants" in {
      GlobalConstants.PUBLISH shouldBe "publish"
      GlobalConstants.UNVERIFIED shouldBe "UnVerified"
      GlobalConstants.ACTIVE shouldBe "active"
      GlobalConstants.DELETED shouldBe "deleted"
    }

    "have correct action constants" in {
      GlobalConstants.ADD shouldBe "add"
      GlobalConstants.EDIT shouldBe "edit"
      GlobalConstants.REMOVE shouldBe "remove"
    }

    "have correct boolean constants" in {
      GlobalConstants.NO shouldBe "no"
      GlobalConstants.YES shouldBe "yes"
    }

    "have correct channel type constants" in {
      GlobalConstants.channelType1 shouldBe "Channel"
      GlobalConstants.channelType2 shouldBe "Direct"
    }

    "have correct mode constants" in {
      GlobalConstants.STATIC shouldBe "Static"
      GlobalConstants.DYNAMIC shouldBe "Dynamic"
    }

    "have correct Tilli API constants" in {
      GlobalConstants.TILLIAPIACCESSPASSWORD shouldBe "GHq5lO3g7TS2Ltj"
      GlobalConstants.TILLIAPIACCESSKEY shouldBe "tilli"
    }
  }
} 