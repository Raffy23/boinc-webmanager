package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.shared.ServerSharedConfig
import at.happywetter.boinc.web.helper.ServerConfig
import at.happywetter.boinc.web.routes.AppRouter.{DashboardLocation, HardwareLocation, SettingsLocation, SwarmControlLocation}
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.language.postfixOps
import scala.scalajs.js
import scalacss.ProdDefaults._
import scalatags.JsDom
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.07.2017
  */
object DashboardMenu {
  object Style extends StyleSheet.Inline {
    import dsl._

    val menu = style(
      position.fixed,
      overflowX.auto,
      bottom.`0`,
      top(50 px),
      listStyleType := "none",
      margin.`0`,
      padding(15 px, 0 px, 0 px, 0 px),
      backgroundColor(c"#e6e6e6"),
      width(207 px),
      border :=! "1px solid #EEE",
      boxShadow := " 0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22)",

      media.maxWidth(690 px)(
        top(100 px)
      )
    )

    val elem = style(
      unsafeChild("a")(
        display.block,
        width(207 px),
        textDecoration := "none",
        padding(10 px, 15 px),
        boxSizing.borderBox,
        color(c"#333"),

        &.hover(
          backgroundColor(c"#74a9d8"),
          color.white
        ),

        unsafeChild("i")(
          marginRight(10 px)
        )
      )
    )

    val active = style(
      backgroundColor(c"#428bca"),
      color :=! "white !important"
    )

    val clickable = style(
      cursor.pointer
    )
  }

  def component: JsDom.TypedTag[HTMLElement] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    computedMenuEntries()
    ul(Style.menu, id := "dashboard-menu",
      li(Style.elem,
        a(
          href := DashboardLocation.link,
          i(`class` := "fa fa-tachometer"), "dashboard_menu_home".localize,
          data("navigo") := "", data("menu-id") := "dashboard",
          onclick := masterSelectionListener
        )
      ),

      li(Style.elem,
        a(
          href := SwarmControlLocation.link,
          i(`class` := "fa fa-industry"), "dashboard_swarm_control".localize,
          data("navigo") := "", data("menu-id") := "swarm_control",
          onclick := masterSelectionListener
        )
      ),

      span(id := "hw-menu-entry-placeholder"),

      li(Style.elem,
        a(
          href := SettingsLocation.link,
          i(`class` := "fa fa-cog"), "dashboard_menu_settings".localize,
          data("navigo") := "", data("menu-id") := "settings",
          onclick := masterSelectionListener
        )
      ),

      li(Style.elem, h2(style :="padding-left: 5px", i(`class` := "fa fa-cubes", style:="margin-right:8px"), "dashboard_menu_computers".localize))
    )

  }

  def computedMenuEntries(): Unit = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    ServerConfig.get.foreach(config => {
      if (config.hardware) {
        dom.document.getElementById("hw-menu-entry-placeholder").appendChild(
          li(Style.elem,
            a(
              href := HardwareLocation.link,
              i(`class` := "fa fa-microchip"), "dashboard_hardware".localize,
              data("navigo") := "", data("menu-id") := "hardware",
              onclick := masterSelectionListener
            )
          ).render
        )
      }
    })
  }

  def onMenuItemClick(event: Event): Unit = {
    val element = dom.document.querySelector(s"ul[id='dashboard-menu'] a[class='${Style.active.htmlClass}']")
    if( element != null)
      element.setAttribute("class", "")

    val me = event.target.asInstanceOf[HTMLElement]
    me.setAttribute("class", Style.active.htmlClass)
  }

  def selectByReference(reference: String): Unit = {
    val element = dom.document.querySelector(s"ul[id='dashboard-menu'] a[class='${Style.active.htmlClass}']")
    if( element != null)
      element.setAttribute("class", "")

    dom.document
      .querySelector("ul[id='dashboard-menu'] a[data-menu-id='"+reference+"']")
      .asInstanceOf[HTMLElement]
      .classList.add(Style.active.htmlClass)
  }

  def removeMenuReferences(reference: String): Unit = {
    val menuNode = dom.document.getElementById("dashboard-menu")
    val elements = dom.document.querySelectorAll("ul[id='dashboard-menu'] a[data-menu-id='"+reference+"']")

    if(elements != null) {
      import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
      elements.forEach((node, _, _) => {
        menuNode.removeChild(node.parentNode)
      })
    }
  }

  def addMenu(linkUrl: String, elementName: String, reference: Option[String] = None, icon: Option[String] = None): Unit = {
    val newElement = buildMenuItem(linkUrl, elementName, reference, icon)

    dom.document.getElementById("dashboard-menu").appendChild(newElement.render)
    if (selected != null) {
      selectMenuItemByContent(selected)
    }
  }

  def addSubMenu(elementName: String, menuReference: String, reference: Option[String] = None, icon: Option[String] = Some("caret-down")): Unit = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    val subMenu = li(Style.elem, Style.clickable,
      a(icon.map(n => i(`class` := s"fa fa-$n")), elementName, reference.map(r => data("menu-id") := r), data("menu-ref") := menuReference), onclick := subMenuListener,
      ul(data("submenu-id") := menuReference, style := "display:none"
      )
    ).render

    dom.document.getElementById("dashboard-menu").appendChild(subMenu)
  }

  def addSubMenuItem(linkUrl: String, elementName: String, submenu: String, reference: Option[String] = None, icon: Option[String] = None): Unit = {
    val newElement = buildMenuItem(linkUrl, elementName, reference, icon)

    dom.document.querySelector(s"#dashboard-menu ul[data-submenu-id='$submenu'").appendChild(newElement.render)
    if (selected != null) {
      selectMenuItemByContent(selected)
    }
  }

  private val selectionListener: js.Function1[Event, Unit] = (event) => onMenuItemClick(event)
  private val masterSelectionListener: js.Function1[Event, Unit] = (event) => {
    dom.document.getElementById("navigation").innerHTML = ""
    onMenuItemClick(event)
  }

  private val subMenuListener: js.Function1[Event, Unit] = (event) => {
    val target = event.target.asInstanceOf[HTMLElement].getAttribute("data-menu-ref")
    val element = dom.document.querySelector(s"#dashboard-menu ul[data-submenu-id='$target']").asInstanceOf[HTMLElement]

    if (element.style.display == "") element.style.display = "none"
    else element.style.display = ""
  }

  private var selected: String = _
  def selectMenuItemByContent(content: String): Unit = {
    val rootElement = dom.document.getElementById("dashboard-menu")
    var marked = false

    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
    rootElement.childNodes.forEach((li, _, _) => {
      if (li.firstChild.textContent == content) {
        li.firstChild.asInstanceOf[HTMLElement].setAttribute("class",Style.active.htmlClass)
        marked = true
      }
    })

    if (!marked) selected = content
    else selected = null
  }

  private def buildMenuItem(linkUrl: String, elementName: String, reference: Option[String] = None, icon: Option[String] = None) = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    li(Style.elem,
      a(href := linkUrl,
        icon.map(n => i(`class` := s"fa fa-$n")), elementName,
        reference.map(r => data("menu-id") := r), data("navigo") := "",
        onclick := selectionListener
      )
    )
  }

}
