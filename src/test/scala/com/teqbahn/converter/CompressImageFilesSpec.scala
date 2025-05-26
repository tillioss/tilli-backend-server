package com.teqbahn.converter

import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.awt.image.BufferedImage
import java.io.{File, FileOutputStream}
import java.util.Random
import javax.imageio.ImageIO

class CompressImageFilesSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {

  val tempDir = new File("target" + File.separator + "test-temp")
  val sourcePath: String = tempDir.getPath + File.separator + "source"
  val destPath: String = tempDir.getPath + File.separator + "dest"
  val nestedPath: String = sourcePath + File.separator + "nested"

  override def beforeEach(): Unit = {
    tempDir.mkdirs()
    new File(sourcePath).mkdirs()
    new File(nestedPath).mkdirs()
    new File(destPath).mkdirs()

    new File(sourcePath, "file1.txt").createNewFile()
    new File(sourcePath, "file2.jpg").createNewFile()
    new File(nestedPath, "file3.json").createNewFile()
    new File(nestedPath, "file4.png").createNewFile()
  }

  override def afterEach(): Unit = {
    FileUtils.deleteDirectory(tempDir)
  }

  "getFileSizeMegaBytes" should "return correct file size in MB" in {
    val file = new File("test.jpg")
    val compressedFileSize = CompressImageFiles.getFileSizeMegaBytes(file)

    compressedFileSize shouldBe 0.0f
  }

  it should "return correct file size for non-zero file" in {
    val testFile = new File(s"$sourcePath/test_file.txt")
    val content = Array.fill[Byte](2 * 1024 * 1024)(1) // 2MB file
    val fos = new FileOutputStream(testFile)
    fos.write(content)
    fos.close()

    val fileSizeMB = CompressImageFiles.getFileSizeMegaBytes(testFile)

    fileSizeMB shouldBe 2.0f +- 0.1f
  }

  "doImageCompression" should "compress JPEG images larger than 1MB" in {
    val testImageFile = createLargeJpegImage(s"$sourcePath/large.jpg")
    val outputFile = new File(s"$destPath/large.jpg")

    val initialSize = CompressImageFiles.getFileSizeMegaBytes(testImageFile)
    initialSize should be > 1.0f

    CompressImageFiles.doImageCompression(testImageFile.getPath, outputFile.getPath)

    outputFile.exists() shouldBe true
    val compressedSize = CompressImageFiles.getFileSizeMegaBytes(outputFile)
    compressedSize should be < initialSize
  }

  it should "copy small jpg files without compression" in {
    val smallImage = createTestJpegImage(s"$sourcePath/small.jpg", 100, 100)
    val outputFile = new File(s"$destPath/small.jpg")

    CompressImageFiles.doImageCompression(smallImage.getPath, outputFile.getPath)

    outputFile.exists() shouldBe true
    outputFile.length() shouldBe smallImage.length()
  }

  it should "copy non-jpg files without compression" in {
    val nonJpgFile = new File(s"$sourcePath/test.png")
    val fos = new FileOutputStream(nonJpgFile)
    fos.write(Array[Byte](1, 2, 3, 4))
    fos.close()

    val outputFile = new File(s"$destPath/test.png")

    CompressImageFiles.doImageCompression(nonJpgFile.getPath, outputFile.getPath)

    outputFile.exists() shouldBe true
    outputFile.length() shouldBe nonJpgFile.length()
  }

  it should "handle non-existent source files" in {
    val nonExistentFile = new File(s"$sourcePath/non_existent.jpg")
    val outputFile = new File(s"$destPath/non_existent.jpg")

    CompressImageFiles.doImageCompression(nonExistentFile.getPath, outputFile.getPath)

    outputFile.exists() shouldBe false
  }

  private def createTestJpegImage(path: String, width: Int, height: Int): File = {
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = bufferedImage.createGraphics()
    g.fillRect(0, 0, width, height)
    g.dispose()

    val file = new File(path)
    ImageIO.write(bufferedImage, "jpg", file)
    file
  }

  private def createLargeJpegImage(path: String): File = {
    val width = 2000
    val height = 2000
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = bufferedImage.createGraphics()

    // Create a colorful image to avoid high compression
    val random = new Random(42)
    for (x <- 0 until width; y <- 0 until height) {
      val color = new java.awt.Color(
        random.nextInt(256),
        random.nextInt(256),
        random.nextInt(256)
      )
      bufferedImage.setRGB(x, y, color.getRGB)
    }

    g.dispose()

    val file = new File(path)
    ImageIO.write(bufferedImage, "jpg", file)

    // Ensure the file is over 1MB
    if (CompressImageFiles.getFileSizeMegaBytes(file) <= 1.0f) {
      file.delete()
      createLargerJpegImage(path, width * 2, height * 2)
    } else {
      file
    }
  }

  private def createLargerJpegImage(path: String, width: Int, height: Int): File = {
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = bufferedImage.createGraphics()

    val random = new Random(42)
    for (x <- 0 until width by 2; y <- 0 until height by 2) {
      val color = new java.awt.Color(
        random.nextInt(256),
        random.nextInt(256),
        random.nextInt(256)
      )

      val xEnd = Math.min(x + 2, width - 1)
      val yEnd = Math.min(y + 2, height - 1)

      for (i <- x to xEnd; j <- y to yEnd) {
        bufferedImage.setRGB(i, j, color.getRGB)
      }
    }

    g.dispose()

    val file = new File(path)
    ImageIO.write(bufferedImage, "jpg", file)
    file
  }

}
