package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.helper.ServerConfig
import at.happywetter.boinc.web.routes.AppRouter.{DashboardLocation, HardwareLocation, SettingsLocation, SwarmControlLocation}
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.xml.Elem
import scalacss.ProdDefaults._

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

  private val viewState: Var[String] = Var("display:block")
  private val hwMenuEntry: Var[Elem] = Var(<span id="config-hardware-disabled"></span>)
  processSeverConfig()

  case class MenuEntry(name: String, href: String, icon: Option[String]=None,
                       reference: Option[String]=None, subMenuRef: Option[String] = None,
                       subMenu: ListBuffer[MenuEntry] = new ListBuffer())
  private val menuEntries: Var[List[MenuEntry]] = Var(List.empty[MenuEntry])

  def component: Elem = {
    <ul class={Style.menu.htmlClass} id="dashboard-menu" style={viewState}>
      <li class={Style.elem.htmlClass}>
        <a href={DashboardLocation.link} onclick={masterSelectionListener}
           data-navigo="true" data-menu-id="dashboard">
          <i class="fa fa-tachometer"></i>
          {"dashboard_menu_home".localize}
        </a>
      </li>
      <li class={Style.elem.htmlClass}>
        <a href={SwarmControlLocation.link} onclick={masterSelectionListener}
           data-navigo="true" data-menu-id="swarm_control">
          <i class="fa fa-industry"></i>
          {"dashboard_swarm_control".localize}
        </a>
      </li>

      {hwMenuEntry}

      <li class={Style.elem.htmlClass}>
        <a href={SettingsLocation.link} onclick={masterSelectionListener}
           data-navigo="true" data-menu-id="settings">
          <i class="fa fa-cog"></i>
          {"dashboard_menu_settings".localize}
        </a>
      </li>

      <li class={Style.elem.htmlClass}>
        <h2 style="padding-left:5px">
          <i class="fa fa-cubes"  style="margin-right:8px"></i>
          {"dashboard_menu_computers".localize}
        </h2>
      </li>

      <span id="menu-entry-spliter"></span>
      {
        menuEntries.map(_.map(entry =>
          <li class={Style.elem.htmlClass}>
            <a href={entry.href} data-menu-id={entry.reference} data-navigo="true"
               onclick={entry.subMenuRef.map(_ => subMenuListener).getOrElse(selectionListener)}>
              {entry.icon.map(icon => <i class={s"fa fa-$icon"}></i>)}
              {entry.name}
              {entry.subMenuRef.map(name =>
                <ul data-submenu-id={name} style="display:none">
                  {entry.subMenu.map(entry =>
                    <li class={Style.elem.htmlClass}>
                      <a href={entry.href} data-menu-id={entry.reference} data-navigo="true" onclick={selectionListener}>
                        {entry.icon.map(icon => <i class={s"fa fa-$icon"}></i>)}
                        {entry.name}
                      </a>
                    </li>
                  )}
                </ul>
              )}
            </a>
          </li>
        ))
      }

    </ul>
  }

  def show(): Unit = viewState := "display:block"
  def hide(): Unit = viewState := "display:none"

  def processSeverConfig(): Unit = {
    ServerConfig.get.foreach(config => {
      if (config == null)
        dom.console.error("ServerConfig is null!")

      if (config.hardware) {
        hwMenuEntry :=
          <li class={Style.elem.htmlClass}>
            <a href={HardwareLocation.link} onclick={masterSelectionListener}
               data-navigo="true" data-menu-id="dashboard_hardware">
              <i class="fa fa-microchip"></i>{"dashboard_hardware".localize}
            </a>
          </li>
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
    menuEntries.update(_ :+ MenuEntry(elementName, linkUrl, icon, reference))

    if (selected != null) {
      selectMenuItemByContent(selected)
    }
  }

  def addSubMenu(elementName: String, menuReference: String, reference: Option[String] = None, icon: Option[String] = Some("caret-down")): Unit = {
    menuEntries.update(_ :+ MenuEntry(elementName, "#", icon, reference, Some(menuReference)))
  }

  def addSubMenuItem(linkUrl: String, elementName: String, submenu: String, reference: Option[String] = None, icon: Option[String] = None): Unit = {
    menuEntries.update(menu => {
      menu.find(_.subMenuRef.contains(submenu)).get.subMenu.append(
        MenuEntry(elementName, linkUrl, icon, reference)
      )

      menu
    })

    if (selected != null) {
      selectMenuItemByContent(selected)
    }
  }

  private val selectionListener: (Event) => Unit = (event) => onMenuItemClick(event)
  private val masterSelectionListener: (Event) => Unit = (event) => onMenuItemClick(event)

  private val subMenuListener: (Event) => Unit = (event) => {
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
      if (li.firstChild != null && li.firstChild.textContent == content) {
        li.firstChild.asInstanceOf[HTMLElement].setAttribute("class",Style.active.htmlClass)
        marked = true
      } else if(li.firstChild == null && li.textContent == content) {
        li.firstChild.asInstanceOf[HTMLElement].setAttribute("class",Style.active.htmlClass)
        marked = true
      }
    })

    if (!marked) selected = content
    else selected = null
  }

}
