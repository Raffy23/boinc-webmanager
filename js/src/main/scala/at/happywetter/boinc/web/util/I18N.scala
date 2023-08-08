package at.happywetter.boinc.web.util

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import scala.collection.immutable.ArraySeq
import scala.xml.Node

import at.happywetter.boinc.shared.boincrpc.ApplicationError
import at.happywetter.boinc.web.boincclient.FetchResponseException
import at.happywetter.boinc.web.util.I18N._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
object I18N:

  object Locale:
    var current: String = getDefault

    val German = "de"
    val English = "en"

    def getDefault: String = dom.window.navigator.language.substring(0, 2) // IE: gives xx-XX

    def save(lang: String = current): Unit =
      current = lang
      dom.window.localStorage.setItem("language", lang)
      setDocumentLanguage(lang)

    def load: String =
      val sessionLang = dom.window.localStorage.getItem("language")
      if (sessionLang == null)
        setDocumentLanguage(getDefault)

        current
      else
        setDocumentLanguage(sessionLang)
        current = sessionLang

        current

    def setDocumentLanguage(lang: String): Unit =
      dom.document.getElementsByTagName("html")(0).asInstanceOf[HTMLElement].setAttribute("lang", lang)

  implicit class TranslatableString(private val str: String) extends AnyVal:
    def localize: String = LanguageDataProvider
      .languageData(Locale.current)
      .getOrElse(str, { println(s"[Warning]: Could not translate: $str into ${Locale.current}"); str })
    def localizeTags(nodes: Node*): Seq[Node] =
      import XMLHelper._
      val captureGroup = "(\\$\\d+)".r

      val indexNodes = nodes.toIndexedSeq
      val fmtString = str.localize
      val nodeList = captureGroup
        .findAllMatchIn(fmtString)
        .map { matchResult =>
          (matchResult.start, indexNodes(matchResult.group(1).drop(1).toInt), matchResult.end)
        }
        .foldLeft((0, Vector.empty[Node])) { case ((start, nodeList), (mStart, node, mEnd)) =>
          (mEnd, nodeList :+ fmtString.slice(start, mStart).toXML :+ node)
        }

      if (nodeList._1 < fmtString.length)
        nodeList._2 :+ fmtString.substring(nodeList._1).toXML
      else
        nodeList._2

  implicit class TranslatableBoolean(private val bool: Boolean) extends AnyVal:
    def localize: String = LanguageDataProvider.languageData(Locale.current).getOrElse(bool.toString, bool.toString)

  implicit class HtmlString(private val str: String) extends AnyVal:
    import XMLHelper._

    def toTags: Seq[Node] =
      if (str.contains("\n"))
        ArraySeq.unsafeWrapArray(str.split("\n")).flatMap(data => Seq(data.toXML, <br/>))
      else
        Seq(str)

  implicit class TranslatableAppError(private val e: ApplicationError) extends AnyVal:
    def localize: String = e.reason.localize

  implicit class TranslatableFetchException(private val ex: FetchResponseException) extends AnyVal:
    def localize: String = s"${ex.statusCode}: ${ex.reason.localize}"
