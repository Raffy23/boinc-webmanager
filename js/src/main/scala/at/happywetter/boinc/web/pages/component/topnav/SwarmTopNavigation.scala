package at.happywetter.boinc.web.pages.component.topnav

import mhtml.{Rx, Var}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.02.2018
  */
object SwarmTopNavigation extends TopNavigation {

  override protected var selected = "boinc"

  override protected val links = List(
    ("boinc", "head_menu_boinc", "fa fa-id-card-o"),
    ("projects", "head_menu_projects", "fa fa-tag"),

    //TODO: Implement Pages:
    //("tasks", "head_menu_tasks".localize, "fa fa-tasks"),
    //("transfers", "head_menu_transfers".localize, "fa fa-exchange"),
    //("statistics", "head_menu_statistics".localize, "fa fa-area-chart"),
    //("disk", "head_menu_disk".localize, "fa fa-pie-chart"),
    //("global_prefs", "head_menu_prefs".localize, "fa fa-cogs")
  )

  override protected val componentId: String = "swarm_page_top_nav"

  override protected def link(nav: String): Rx[String] = Var(s"/view/swarm/$nav")
}
