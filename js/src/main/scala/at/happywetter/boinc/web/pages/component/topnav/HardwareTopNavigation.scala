package at.happywetter.boinc.web.pages.component.topnav

import mhtml.{Rx, Var}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.02.2018
  */
object HardwareTopNavigation extends TopNavigation:

  protected var selected: Var[String] = Var("hosts")

  override protected val links = List(
    ("sensors", "head_menu_hosts", "fa-solid fa-computer"),
    ("actions", "head_menu_control", "fa-solid fa-cubes-stacked")
  )

  override val componentId: String = "hardware_page_top_nav"

  override protected def link(nav: String): Rx[String] = Var(s"/view/hardware/$nav")
