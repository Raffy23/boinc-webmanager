package at.happywetter.boinc.web.css.definitions.components

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object Dialog extends StyleDefinitions:

  implicit override protected[this] val prefix: String = "dialog"

  val header = CSSIdentifier("header")
  val button = CSSIdentifier("button")

  override private[css] def styles =
    Seq(header, button)
