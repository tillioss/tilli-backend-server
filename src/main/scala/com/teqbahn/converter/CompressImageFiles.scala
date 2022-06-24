package com.teqbahn.converter

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.{File, FileInputStream, FileNotFoundException, FileOutputStream, IOException, InputStream, OutputStream}
import java.util

import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream
import org.apache.commons.io.FileUtils

object CompressImageFiles {

  def RecursivePrint(arr: Array[File], index: Int, level:Int)
  {
    if(index == arr.length)
      return;

    if(arr(index).isFile() ) {
      val fileName = arr(index).getName();
      if(!fileName.contains(".json") && !fileName.contains(".DS_Store")){
        val filePath = arr(index).getPath();
        println("**********************************************Start");
        println("fileName --> " +fileName);
        println("filePath --> " +filePath);
        val newFilePath = filePath.replace("/merged/tilliPlayers/", "/merged/migration/")
        doImageCompression(filePath, newFilePath)
        println("**********************************************");
        println("**********************************************END")
      }
    }
    else if(arr(index).isDirectory()) {
      val dirPath = arr(index).getPath();
      if(dirPath.contains("activity") || dirPath.contains("activityCover") ||  dirPath.contains("common") || dirPath.contains("gameCover") || dirPath.contains("profile")) {
        val newDirPath = dirPath.replace("/merged/tilliPlayers/", "/merged/migration/")
        println("[" + dirPath + "]");
        println("[" + newDirPath + "]");
        val newFile = new File(newDirPath)
        if(!newFile.exists()){
          newFile.mkdirs()
        }
        RecursivePrint(arr(index).listFiles(), 0, level + 1);
      }
    }
    var index1 = index+1
    RecursivePrint(arr,index1, level);
  }




  def doImageCompression(srcPath : String, destPath : String): Unit = {
    try {
      var imageFile : File = new File(srcPath);
      var compressedImageFile : File = new File(destPath);
      if(imageFile.exists() || imageFile.isFile) {
        var fileSizeByMB = getFileSizeMegaBytes(imageFile)
        if(fileSizeByMB > 1 && (imageFile.getName.contains(".jpg") || imageFile.getName.contains(".jpeg"))){
          println(getFileSizeMegaBytes(imageFile));
          var is : InputStream = new FileInputStream(imageFile);
          var os : OutputStream = new FileOutputStream(compressedImageFile);
          var quality : Float = 0.5f;
          var image : BufferedImage = ImageIO.read(is);
          var writers : util.Iterator[ImageWriter] = ImageIO.getImageWritersByFormatName("jpg");
          if (!writers.hasNext())
            throw new IllegalStateException("No writers found");

          var writer : ImageWriter = writers.next().asInstanceOf[ImageWriter];
          var ios : ImageOutputStream = ImageIO.createImageOutputStream(os);
          writer.setOutput(ios);

          var param : ImageWriteParam = writer.getDefaultWriteParam();
          param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
          param.setCompressionQuality(quality);

          writer.write(null, new IIOImage(image, null, null), param);

          is.close();
          os.close();
          ios.close();
          writer.dispose();
        } else {
          FileUtils.copyFile(imageFile, compressedImageFile)
        }
      }
    } catch {
      case e: FileNotFoundException => println("Couldn't find that file.")
      case e: IOException => println("Had an IOException trying to read that file")
      case e: IllegalStateException => println(e)
      case e: Exception => println(e)
    }
  }

  def getFileSizeMegaBytes(file: File): Float = {
    return  file.length() / (1024 * 1024);
  }
}
