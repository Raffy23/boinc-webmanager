package at.happywetter.boinc.web.css.definitions

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object Misc extends StyleDefinitions:

  implicit override protected[this] val prefix: String = "misc"

  val centeredText = CSSIdentifier("center-text")

  override private[css] def styles =
    Seq(centeredText)
