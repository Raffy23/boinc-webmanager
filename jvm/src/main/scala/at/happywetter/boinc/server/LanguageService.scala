package at.happywetter.boinc.server

import java.util
import java.util.Properties

import at.happywetter.boinc.util.ResourceWalker

import scala.io.Source
import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
object LanguageService {

  val languages: List[(String, String, String)] =
    ResourceWalker
      .listFiles("/lang")
      .filter(_.endsWith(".properties"))
      .map(_.split("\\.")(0))
      .map(language => {
        val prop = loadProp(language)
        (language, prop.getProperty("language_name"), prop.getProperty("language_icon"))
      })

  import org.http4s._
  import org.http4s.dsl._
  import prickle._

  def apply(): HttpService = HttpService {

    case GET -> Root => Ok(Pickle.intoString(languages))
    case GET -> Root / lang =>
      Try(
        Ok(
          Pickle.intoString(load(lang))
        )
      ).getOrElse(NotFound())

  }


  private def loadProp(lang: String): Properties = {
    val content = new Properties()
    val path = s"${ResourceWalker.RESOURCE_ROOT}/lang/$lang.properties"
    val ins  = ResourceWalker.getStream(path)

    content.load(ins)
    content
  }

  private def load(lang: String): Map[String, String] = {
    import scala.collection.JavaConverters._
    loadProp(lang).asScala.toMap
  }
}
