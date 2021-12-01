package at.happywetter.boinc.web.pages.component.topnav
import mhtml.{Rx, Var}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 09.07.2020
 */
object SettingsTopNavigation extends TopNavigation {

  override protected var selected: Var[String] = Var("")

  override val componentId: String = "settings_top_navigation"

  override protected val links: List[(String, String, String)] = List(
    ("", "head_menu_main", "fas fa-address-card"),
    ("hosts", "head_menu_hosts", "fa fa-tag"),
  )

  override protected def link(nav: String): Rx[String] = Var(s"/view/settings/$nav")

}
