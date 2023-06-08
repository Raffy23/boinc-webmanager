package at.happywetter.boinc.util.webjar

import java.util.Properties

import scala.util.Using

/**
 * Created by: 
 *
 * @author Raphael
 * @version 06.11.2020
 */
object ResourceResolver:

  def searchForResourceProps(name: String, prefix: String = ""): Option[Properties] =
    val loader = getClass.getClassLoader
    val path = s"META-INF/maven/org.webjars${if prefix.isEmpty then "" else "." + prefix}/$name/pom.properties"

    println(path)
    Using(loader.getResourceAsStream(path)) { pom =>
      val prop = new Properties()
      prop.load(pom)

      prop
    }.fold(e => { e.printStackTrace(); throw e }, Some(_))

  def getResourceRoot(prop: Properties): String =
    s"META-INF/resources/webjars/${prop.get("artifactId")}/${prop.get("version")}/"

  def getResourceRoot(repo: String, name: String): String =
    searchForResourceProps(name, repo).map(getResourceRoot).get
