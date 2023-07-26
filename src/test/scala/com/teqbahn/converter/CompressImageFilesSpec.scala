package com.teqbahn.converter

import org.scalatest.flatspec.AnyFlatSpec

class CompressImageFilesSpec extends AnyFlatSpec {

  "getFileSizeMegaBytes" should "return correct file size in MB" in {
    val fileSizeBytes = 1024 * 1024 // 1 MB
    val file = new java.io.File("test.jpg")
    val compressedFileSize = CompressImageFiles.getFileSizeMegaBytes(file)

    assert(compressedFileSize === 0.0f) 
  }
}
