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

  implicit class TranslatableString(private val str: String) extends AnyVal {
    def localize: String = LanguageDataProvider.languageData(Locale.current).getOrElse(str, {println(s"[Warning]: Could not translate: $str into ${Locale.current}"); str})
    def localizeTags(nodes: Node*): Seq[Node] = {
      val captureGroup = "(\\$\\d+)".r

      val indexNodes = nodes.toIndexedSeq
      val fmtString  = str.localize
      val nodeList   = captureGroup.findAllMatchIn(fmtString).map{ matchResult =>
        (matchResult.start, indexNodes(matchResult.group(1).drop(1).toInt), matchResult.end)
      }.foldLeft((0, List.empty[Node])) { case ((start, nodeList), (mStart, node, mEnd)) =>
        (mEnd, node :: xml.Text(fmtString.slice(start, mStart)) :: nodeList)
      }

      (
        if (nodeList._1 < fmtString.length)
          xml.Text(fmtString.substring(nodeList._1)) :: nodeList._2
        else
          nodeList._2
      ).reverse
    }
  }

  implicit class TranslatableBoolean(private val bool: Boolean) extends AnyVal {
    def localize: String = LanguageDataProvider.languageData(Locale.current).getOrElse(bool.toString, bool.toString)
  }

  implicit class HtmlString(private val str: String) extends AnyVal {
    import at.happywetter.boinc.web.helper.XMLHelper._

    def toTags: Seq[Node] =
      if (str.contains("\n")) {
        str.split("\n").flatMap(data => Seq(data.toXML, <br/>))
      } else {
        Seq(str)
      }
  }

  implicit class TranslatableAppError(private val e: ApplicationError) extends AnyVal {
    def localize: String = e.reason.localize
  }

  implicit class TranslatableFetchException(private val ex: FetchResponseException) extends AnyVal {
    def localize: String = s"${ex.statusCode}: ${ex.reason.localize}"
  }

}
