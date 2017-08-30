package at.happywetter.boinc.web.util

import org.scalajs.dom

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

    def getDefault: String = dom.window.navigator.language

    def save(lang: String = current): Unit = {
      current = lang
      dom.window.sessionStorage.setItem("language", lang)
    }

    def load: String = {
      val sessionLang = dom.window.sessionStorage.getItem("language")
      if (sessionLang == null)
        current
      else {
        current = sessionLang
        current
      }
    }

  }


  implicit class TranslatableString(str: String) {
    def localize: String = LanguageDataProvider.languageData(Locale.current).getOrElse(str, str)
  }

  implicit class HtmlString(str: String) {
    import scalatags.JsDom.all._


    def toTags: List[Modifier] =
      if (str.contains("\n")) {
        val data = str.split("\n")

        List(data(0), br(), data(1))
      } else {
        List(str)
      }

  }

}
