package at.happywetter.boinc.web.css.definitions.pages

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincMainHostStyle extends StyleDefinitions:

  implicit override protected[this] val prefix: String = "boinc_main_host"

  val table = CSSIdentifier("table")

  override private[css] def styles =
    Seq(table)
