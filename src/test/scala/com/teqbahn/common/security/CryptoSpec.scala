package com.teqbahn.common.security

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CryptoSpec extends AnyFlatSpec with Matchers {

  "md5HashString" should "return the MD5 hash of the input string" in {
    val inputString = "Hello, world!"

    // The expected MD5 hash for the input string is precomputed
    val expectedHash = "6cd3556deb0da54bca060b4c39479839"

    val resultHash = Crypto.md5HashString(inputString)

    resultHash should be(expectedHash)
  }
}
