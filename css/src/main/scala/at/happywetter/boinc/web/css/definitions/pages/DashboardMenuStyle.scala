package at.happywetter.boinc.web.css.definitions.pages

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object DashboardMenuStyle extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "dashboard_menu"

  val menu = CSSIdentifier("menu")
  val elem = CSSIdentifier("element")
  val active = CSSIdentifier("active")
  val clickable = CSSIdentifier("clickable")
  val subMenuHosts = CSSIdentifier("sub_menu_hosts")

  override private[css] def styles =
    Seq(menu, elem, active, clickable, subMenuHosts)

}
