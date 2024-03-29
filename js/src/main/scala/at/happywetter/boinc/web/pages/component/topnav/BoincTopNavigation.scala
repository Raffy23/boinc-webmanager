package at.happywetter.boinc.web.pages.component.topnav

import at.happywetter.boinc.web.pages.boinc.BoincClientLayout
import mhtml.{Rx, Var}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 06.02.2018
  */
object BoincTopNavigation extends TopNavigation {

  val clientName: Var[String] = Var("none")

  override protected var selected: Var[String] = Var("boinc")

  override protected val links = List(
    ("boinc", "head_menu_boinc", "fa fa-address-card"),
    ("messages", "head_menu_messages", "fa fa-envelope"),
    ("projects", "head_menu_projects", "fa fa-tag"),
    ("tasks", "head_menu_tasks", "fa fa-tasks"),
    ("transfers", "head_menu_transfers", "fa fa-exchange-alt"),
    ("statistics", "head_menu_statistics", "fas fa-chart-area"),
    ("disk", "head_menu_disk", "fas fa-chart-pie"),
    ("global_prefs", "head_menu_prefs", "fa fa-cogs")
  )

  override val componentId: String = "boinc_top_navigation"

  override protected def link(nav: String): Rx[String] =
    clientName.map(clientName => BoincClientLayout.link(clientName, nav))

}
