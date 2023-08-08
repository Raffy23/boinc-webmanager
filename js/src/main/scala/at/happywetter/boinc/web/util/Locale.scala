package at.happywetter.boinc.web.util

import org.scalajs.dom

object Locale:

  opaque type Language = String
  object Language:
    def apply(str: String): Language = str

  extension (language: Language)
    def value: String = language
    def ==(other: Language): Boolean = language.value == other.value

  val German = Language("de")
  val English = Language("en")

  val FallbackLanguage = English

  def getDefault: Language =
    Language(dom.window.navigator.language.substring(0, 2)) // unsafe, IE: gives xx-XX

  def setDocumentLanguage(lang: Language): Unit =
    dom.window.document.getElementById("html") match {
      case null    => throw new Exception("Unable to find <html> tag in document")
      case htmlTag => htmlTag.asInstanceOf[dom.Element].setAttribute("lang", lang.value)
    }
