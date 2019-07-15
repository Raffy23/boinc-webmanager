package at.happywetter.boinc.web.util

import at.happywetter.boinc.shared.webrpc.ApplicationError
import at.happywetter.boinc.web.boincclient.FetchResponseException
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.xml.Node

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
object I18N {

  object Locale {
    var current: String = getDefault

    val German = "de"
    val English = "en"

    def getDefault: String = dom.window.navigator.language.substring(0,2) // IE: gives xx-XX

    def save(lang: String = current): Unit = {
      current = lang
      dom.window.localStorage.setItem("language", lang)
      setDocumentLanguage(lang)
    }

    def load: String = {
      val sessionLang = dom.window.localStorage.getItem("language")
      if (sessionLang == null) {
        setDocumentLanguage(getDefault)

        current
      } else {
        setDocumentLanguage(sessionLang)
        current = sessionLang

        current
      }
    }

    def setDocumentLanguage(lang: String): Unit =
      dom.document.getElementsByTagName("html")(0).asInstanceOf[HTMLElement].setAttribute("lang", lang)

  }


  implicit class TranslatableString(str: String) {
    def localize: String = LanguageDataProvider.languageData(Locale.current).getOrElse(str, {println(s"[Warning]: Could not translate: $str into ${Locale.current}"); str})
  }

  implicit class TranslatableBoolean(bool: Boolean) {
    def localize: String = LanguageDataProvider.languageData(Locale.current).getOrElse(bool.toString, bool.toString)
  }

  implicit class HtmlString(str: String) {
    import at.happywetter.boinc.web.helper.XMLHelper._

    def toTags: Seq[Node] =
      if (str.contains("\n")) {
        str.split("\n").flatMap(data => Seq(data.toXML, <br/>))
      } else {
        Seq(str)
      }
  }

  implicit class TranslatableAppError(e: ApplicationError) {
    def localize: String = e.reason.localize
  }

  implicit class TranslatableFetchException(ex: FetchResponseException) {
    def localize: String = s"${ex.statusCode}: ${ex.reason.localize}"
  }

}
