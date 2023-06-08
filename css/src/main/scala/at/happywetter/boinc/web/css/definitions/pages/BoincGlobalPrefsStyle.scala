package at.happywetter.boinc.web.css.definitions.pages

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincGlobalPrefsStyle extends StyleDefinitions:

  implicit override protected[this] val prefix: String = "boinc_global_prefs"

  val rootPane = CSSIdentifier("")
  val input = CSSIdentifier("input")
  val h4Padding = CSSIdentifier("h4_padding")

  override private[css] def styles =
    Seq(rootPane, input, h4Padding)
