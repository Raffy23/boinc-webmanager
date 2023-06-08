package at.happywetter.boinc.web.css.definitions.pages

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincStatisticsStyle extends StyleDefinitions:

  implicit override protected[this] val prefix: String = "boinc_statistics"

  val button = CSSIdentifier("button")
  val active = CSSIdentifier("active")

  override private[css] def styles =
    Seq(button, active)
