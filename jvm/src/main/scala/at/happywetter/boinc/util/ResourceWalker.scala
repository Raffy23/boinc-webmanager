package at.happywetter.boinc.util

import java.net.URI
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

  val RESOURCE_ROOT = "/"
  lazy val resourceURI: URI = ResourceWalker.getClass.getResource(RESOURCE_ROOT).toURI


  def listFiles(path: String): List[String] = {
    val fileWalkerPath =
      if (resourceURI.getScheme == "jar") pathFromJar
      else Paths.get(resourceURI)

    Files
      .walk(Paths.get(fileWalkerPath.toString+path), 1)
      .iterator()
      .asScala
      .toList
      .map(p => p.getFileName.toString)
  }

  private def pathFromJar: Path =
    FileSystems.newFileSystem(resourceURI, Collections.emptyMap[String, Any]).getPath(RESOURCE_ROOT)


}
