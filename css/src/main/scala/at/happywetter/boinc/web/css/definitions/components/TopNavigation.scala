package at.happywetter.boinc.web.css.definitions.components

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object TopNavigation extends StyleDefinitions:

  implicit override protected[this] val prefix: String = "topnav"

  val nav = CSSIdentifier("")
  val bigScreenOnly = CSSIdentifier("big_screen")
  val active = CSSIdentifier("active")

  override private[css] def styles =
    Seq(nav, bigScreenOnly, active)
