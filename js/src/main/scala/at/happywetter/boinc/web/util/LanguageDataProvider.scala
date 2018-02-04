package at.happywetter.boinc.web.util

import at.happywetter.boinc.web.helper.FetchHelper
import at.happywetter.boinc.web.util.I18N.Locale
import org.scalajs.dom.experimental.{Fetch, HttpMethod, RequestInit}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
object LanguageDataProvider {

  val fallbackLanguage: String = Locale.English
  val languageData = new mutable.HashMap[String,Map[String,String]]()
  var available = new ListBuffer[(String, String, String)]

  def bootstrap: Future[Map[String,String]] = {
    fetchAvailableLanguages
      .flatMap(languages => {
        languages.foreach(available += _)
        Locale.current = languages.map(_._1).find(s => s == Locale.current).getOrElse(fallbackLanguage)

        fetchLanguage()
      })
      .map(lang => {
        languageData.put(Locale.current, lang)
        lang
      })
  }

  def loadLanguage(lang: String): Future[Map[String, String]] =
    if (languageData.keys.exists(_ == lang)) Future { languageData(lang) }
    else fetchLanguage(lang).map(data => {languageData.put(lang, data); data})

  def fetchAvailableLanguages: Future[List[(String, String, String)]] =
    Fetch.fetch("/language", RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .flatMap(response => response.text().toFuture)
      .map(data => decode[List[(String, String, String)]](data).toOption.get)

  private def fetchLanguage(lang: String = Locale.current): Future[Map[String, String]] =
    Fetch.fetch("/language/" + lang, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .flatMap(response => response.text().toFuture)
      .map(data => decode[Map[String, String]](data).toOption.get)

}
