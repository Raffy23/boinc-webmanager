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
  }


  implicit class TranslateableString(str: String) {

    def translate: String = {
      LanguageDataProvider.languageData(Locale.current).getOrElse(str, str)
    }

  }

}
