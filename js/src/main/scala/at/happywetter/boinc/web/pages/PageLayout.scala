package at.happywetter.boinc.web.pages

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.HTMLElement
import scala.scalajs.js
import scala.xml.Elem
import scala.xml.Node

import at.happywetter.boinc.web.css.definitions.components.{PageLayoutStyle => Style}
import at.happywetter.boinc.web.pages.component.topnav.TopNavigation
import at.happywetter.boinc.web.util.AuthClient
import at.happywetter.boinc.web.util.ServerConfig
import at.happywetter.boinc.web.util.XMLHelper._

import mhtml.Var

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.07.2017
  */
object PageLayout:

  var navComponentID: String = ""
  val nav: Var[Node] = Var("")

  val hamburgerMenuAction: Event => Unit = event => {
    val menu = dom.document.getElementById("dashboard-menu").asInstanceOf[HTMLElement]

    if (menu.style.display == "none") showMenu()
    else hideMenu()
  }

  val heading: Elem =
    <header class={Style.heading.htmlClass}>
      <h1 class={Style.headerText.htmlClass}>
        <i class="fa fa-bars" style="margin-right:5px" aria-hidden="true" onclick={hamburgerMenuAction}/>
        Boinc Webmanager
      </h1>
      <div id="navigation">{nav}</div>
    </header>

  def showMenu(): Unit =
    val menu = dom.document.getElementById("dashboard-menu").asInstanceOf[HTMLElement]
    val content = dom.document.getElementById("client-container").asInstanceOf[HTMLElement]

    menu.style.display = "block"

    if (dom.window.outerWidth > 690)
      content.style.marginLeft = "229px"

  def hideMenu(): Unit =
    val menu = dom.document.getElementById("dashboard-menu").asInstanceOf[HTMLElement]
    val content = dom.document.getElementById("client-container").asInstanceOf[HTMLElement]

    menu.style.display = "none"

    if (dom.window.outerWidth > 690) content.style.marginLeft = "20px"
    else content.style.marginLeft = "5px"

  def setNav(nav: TopNavigation): Unit =
    if (navComponentID != nav.componentId)
      navComponentID = nav.componentId
      this.nav := nav.component

  def clearNav(): Unit =
    navComponentID = ""
    nav := ""
