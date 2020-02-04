package at.happywetter.boinc.web.css.definitions.components

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object PageLayoutStyle extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "page-layout"

  val heading         = CSSIdentifier("heading")
  val headerText      = CSSIdentifier("header_text")
  val versionField    = CSSIdentifier("version_field")
  val footer          = CSSIdentifier("footer")
  val clientContainer = CSSIdentifier("client_container")

  override private[css] def styles =
    Seq(heading, headerText, versionField, footer, clientContainer)

}
