package at.happywetter.boinc.web.css.definitions.pages

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object LoginPageStyle extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "login_page"

  val content   = CSSIdentifier("content")
  val input     = CSSIdentifier("input")
  val headerBar = CSSIdentifier("header_bar")
  val button    = CSSIdentifier("button")

  override private[css] def styles =
    Seq(content, input, headerBar, button)

}
