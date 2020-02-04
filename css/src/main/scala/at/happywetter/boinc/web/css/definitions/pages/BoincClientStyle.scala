package at.happywetter.boinc.web.css.definitions.pages

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincClientStyle extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "boinc_client_layout"

  val pageHeader = CSSIdentifier("page_header")
  val pageHeaderSmall = CSSIdentifier("page_header_small")
  val h4 = CSSIdentifier("h4")
  val h4WithoutLine = CSSIdentifier("h4_without_line")
  val content = CSSIdentifier("content")
  val inTextIcon = CSSIdentifier("in_text_icon")
  val progressBar = CSSIdentifier("progress_bar")

  override private[css] def styles =
    Seq(pageHeader, pageHeaderSmall, h4, h4WithoutLine, content, inTextIcon, progressBar)

}
