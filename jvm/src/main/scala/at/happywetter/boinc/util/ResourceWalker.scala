package at.happywetter.boinc.util

import java.io.InputStream
import java.net.{URI, URL}
import java.nio.file.{FileSystems, Files, Path, Paths}
import java.util.Collections

import scala.collection.JavaConverters._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
object ResourceWalker {

  val RESOURCE_ROOT = "/resources"
  lazy val resource: URL = ResourceWalker.getClass.getResource(RESOURCE_ROOT)
  lazy val resourceURI: URI = resource.toURI


  def listFiles(path: String): List[String] = {
    println("PATH: " + path)
    println("URI: " + resourceURI)

    val targetPath = resourceURI.getScheme match {
      case "jar" => FileSystems.newFileSystem(resourceURI, Collections.emptyMap[String, Any]).getPath(RESOURCE_ROOT + path)
      case _ => Paths.get(Paths.get(resourceURI).toString+path)
    }

    println(targetPath)
    Files
      .walk(targetPath, 1)
      .iterator()
      .asScala
      .toList
      .map(p => p.getFileName.toString)
  }

  def getStream(file: String): InputStream = ResourceWalker.getClass.getResourceAsStream(file)

}
