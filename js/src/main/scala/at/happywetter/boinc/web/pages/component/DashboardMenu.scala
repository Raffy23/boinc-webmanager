package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.routes.AppRouter.DashboardLocation
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.language.postfixOps
import scalacss.DevDefaults._
import scalatags.JsDom
import at.happywetter.boinc.web.util.I18N._

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
      height(100 %%),
      top(50 px),
      listStyleType := "none",
      margin.`0`,
      padding(15 px, 0 px, 0 px, 0 px),
      backgroundColor(c"#e6e6e6"),
      width(207 px),
      border :=! "1px solid #EEE",
      boxShadow := " 0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22)"
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
        )
      )
    )

    val active = style(
      backgroundColor(c"#428bca"),
      color :=! "white !important",
    )
  }


  lazy val component: JsDom.TypedTag[HTMLElement] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    ul(Style.menu, id := "dashboard-menu",
      li(Style.elem,
        a(href := DashboardLocation.link, "dashboard_menu_home".localize, data("navigo") := "",
        onclick := { (event: Event) => {
          dom.document.getElementById("navigation").innerHTML = ""
          onMenuItemClick(event)
        }})
      ),

      li(Style.elem,
        a(href :="#settings", "dashboard_menu_settings".localize,
        onclick := { (event: Event) => {
          dom.document.getElementById("navigation").innerHTML = ""
          onMenuItemClick(event)
        }})
      ),

      li(Style.elem, h2(style :="padding-left: 5px", "Computer"))
    )

  }

  def onMenuItemClick(event: Event): Unit = {
    val element = dom.document.querySelector(s"ul[id='dashboard-menu'] a[class='${Style.active.htmlClass}']")
    if( element != null)
      element.setAttribute("class", "")

    val me = event.target.asInstanceOf[HTMLElement]
    me.setAttribute("class",Style.active.htmlClass)
  }

  def removeMenuReferences(reference: String): Unit = {
    val menuNode = dom.document.getElementById("dashboard-menu")
    val elements = dom.document.querySelectorAll("ul[id='dashboard-menu'] a[data-menu-id='"+reference+"']")

    if(elements != null) {
      println(elements)

      import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
      elements.forEach((node, _, _) => menuNode.removeChild(node.parentNode))
    }
  }

  def addMenu(linkUrl: String, elementName: String, reference: Option[String] = None): Unit = {
    val newElement: JsDom.TypedTag[HTMLElement] = {
      import scalacss.ScalatagsCss._
      import scalatags.JsDom.all._

      li(Style.elem,
        a(href := linkUrl,
          elementName,
          reference.map(r => data("menu-id") := r), data("navigo") := "",
          onclick := { (event: Event) => { onMenuItemClick(event) }}
        )
      )
    }

    dom.document.getElementById("dashboard-menu").appendChild(newElement.render)
    if (selected != null) {
      selectMenuItemByContent(selected)
    }
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

}
