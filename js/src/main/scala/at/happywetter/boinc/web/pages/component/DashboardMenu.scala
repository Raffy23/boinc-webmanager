package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.pages.{Dashboard, HardwarePage, SettingsPage, WebRPCProjectPage}
import at.happywetter.boinc.web.css.definitions.pages.{DashboardMenuStyle => Style}
import at.happywetter.boinc.web.pages.swarm.BoincSwarmPage
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.ServerConfig
import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLAnchorElement, HTMLElement}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.07.2017
  */
object DashboardMenu {

  private val viewState: Var[String] = Var("display:block")
  private val hwMenuEntry: Rx[Elem] =
    ServerConfig.config.map { config =>
      if (config.hardware) {
        <li class={Style.elem.htmlClass}>
          <a href={HardwarePage.link} onclick={masterSelectionListener}
             data-navigo={true} data-menu-id="dashboard_hardware">
            <i style="margin-right:14px" class="fa fa-microchip"></i>{"dashboard_hardware".localize}
          </a>
        </li>
      } else {
        <li><span id="config-hardware-disabled"></span></li>
      }
    }

  class MenuEntry(val name: String, val href: String)
  case class TopLevelEntry(override val name: String, override val href: String, reference: Option[String]=None) extends MenuEntry(name, href)
  case class SublistEntry(override val name: String, override val href: String, visible: Var[Boolean] = Var(false),
                          subMenuRef: String, reference: Option[String]=None,
                          subMenu: Var[List[SubMenuEntry]] = Var(List.empty)) extends MenuEntry(name, href)
  case class SubMenuEntry(override val name: String, override val href: String, reference: Option[String]=None) extends MenuEntry(name, href)

  private val menuComputers: Var[List[MenuEntry]] = Var(List.empty)
  private val menuGroups: Var[List[MenuEntry]] = Var(List.empty)

  def component: Elem = {
    <ul class={Style.menu.htmlClass} id="dashboard-menu" style={viewState}>
      <li class={Style.elem.htmlClass}>
        <a href={Dashboard.link} onclick={masterSelectionListener}
           data-navigo={true} data-menu-id="dashboard">
          <i class="fa fa-tachometer-alt" aria-hidden="true"></i>
          {"dashboard_menu_home".localize}
        </a>
      </li>
      <li class={Style.elem.htmlClass}>
        <a href={BoincSwarmPage.link} onclick={masterSelectionListener}
           data-navigo={true} data-menu-id="swarm_control">
          <i class="fa fa-industry" aria-hidden="true"></i>
          {"dashboard_swarm_control".localize}
        </a>
      </li>

      {hwMenuEntry}

      <li class={Style.elem.htmlClass}>
        <a href={SettingsPage.link} onclick={masterSelectionListener}
           data-navigo={true} data-menu-id="settings">
          <i class="fa fa-cog" aria-hidden="true"></i>
          {"dashboard_menu_settings".localize}
        </a>
      </li>

      <li class={Style.elem.htmlClass}>
        <a href={WebRPCProjectPage.link} onclick={masterSelectionListener}
           data-navigo={true} data-menu-id="dashboard_webrpc">
          <i class="fa fa-cloud" aria-hidden="true"></i>
          {"dashboard_webrpc".localize}
        </a>
      </li>

    <li><hr id="menu-entry-spliter"/></li>

      <li class={Style.elem.htmlClass}>
        <h2 style="padding-left:5px">
          <i class="fa fa-desktop" aria-hidden="true" style="margin-right:8px"></i>
          {"dashboard_menu_computers".localize}
        </h2>
      </li>
      {
        menuComputers.map(_.sortBy(_.name).map {
          case entry: TopLevelEntry =>
            <li class={Style.elem.htmlClass}>
              <a href={entry.href} data-menu-id={entry.reference} data-navigo={true}
                 onclick={selectionListener}>
                {entry.name}
              </a>
            </li>

          case entry: SublistEntry =>
            <li class={Seq(Style.elem.htmlClass, Style.clickable.htmlClass).mkString(" ")}>
              <a class={ Style.clickable.htmlClass} data-menu-id={entry.reference}
                 data-menu-ref={entry.subMenuRef} onclick={subMenuListener(entry)}>
                <i class={entry.visible.map(icon => s"fa fa-caret-${if(icon) "down" else "right"}")}></i>
                {entry.name}
                <span class={Style.subMenuHosts.htmlClass}>{entry.subMenu.map(_.size)}</span>
              </a>

              <ul data-submenu-id={entry.subMenuRef} style={entry.visible.map(v => s"display:${if(v) "block" else "none"}")}>
                {entry.subMenu.map(_.map(entry =>
                <li class={Style.elem.htmlClass}>
                  <a href={entry.href} data-menu-id={entry.reference} data-navigo={true} onclick={selectionListener} style="width:unset">
                    {entry.name}
                  </a>
                </li>
              ))}
              </ul>
            </li>
        })
      }
      <li class={Style.elem.htmlClass}>
        <h2 style="padding-left:5px">
          <i class="fa fa-cubes" aria-hidden="true" style="margin-right:8px"></i>
          {"dashboard_menu_groups".localize}
        </h2>
      </li>
      {
        menuGroups.map(_.sortBy(_.name).map {
          case entry: TopLevelEntry =>
            <li class={Style.elem.htmlClass}>
              <a href={entry.href} data-menu-id={entry.reference} data-navigo={true}
                 onclick={selectionListener}>
                {entry.name}
              </a>
            </li>

          case entry: SublistEntry =>
            <li class={Seq(Style.elem.htmlClass, Style.clickable.htmlClass).mkString(" ")}>
              <a class={ Style.clickable.htmlClass} data-menu-id={entry.reference}
                 data-menu-ref={entry.subMenuRef} onclick={subMenuListener(entry)}>
                <i class={entry.visible.map(icon => s"fa fa-caret-${if(icon) "down" else "right"}")}></i>
                {entry.name}
                <span class={Style.subMenuHosts.htmlClass}>{entry.subMenu.map(_.size)}</span>
              </a>

              <ul data-submenu-id={entry.subMenuRef} style={entry.visible.map(v => s"display:${if(v) "block" else "none"}")}>
                {entry.subMenu.map(_.map(entry =>
                  <li class={Style.elem.htmlClass}>
                    <a href={entry.href} data-menu-id={entry.reference} data-navigo={true} onclick={selectionListener} style="width:unset">
                      {entry.name}
                    </a>
                  </li>
                ))}
              </ul>
            </li>
        })
      }
    </ul>
  }

  def show(): Unit = viewState := "display:block"
  def hide(): Unit = viewState := "display:none"

  def onMenuItemClick(event: Event): Unit = {
    val element = dom.document.querySelector(s"ul[id='dashboard-menu'] a[class='${Style.active.htmlClass}']")
    if( element != null)
      element.setAttribute("class", "")

    var me = event.target.asInstanceOf[HTMLElement]
    while (me.nodeName.toLowerCase() != "a") {
      me = me.parentElement
    }

    me.setAttribute("class", Style.active.htmlClass)
  }

  def selectByMenuId(reference: String): Unit = {
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
      import at.happywetter.boinc.web.facade.NodeListConverter.convNodeList
      elements.forEach((node, _, _) => {
        menuNode.removeChild(node.parentNode)
      })
    }
  }

  def addComputer(linkUrl: String, elementName: String, reference: Option[String] = None): Unit =
    menuComputers.update(_ :+ TopLevelEntry(elementName, linkUrl, reference))

  def addGroup(elementName: String, menuReference: String, reference: Option[String] = None): Unit =
    menuGroups.update(_ :+ SublistEntry(elementName, "#", Var(false), menuReference, reference))

  def addComputerToGroup(linkUrl: String, elementName: String, submenu: String, reference: Option[String] = None): Unit = {
    def find(menu: List[MenuEntry]): SublistEntry =
      menu.find(x =>
        x.isInstanceOf[SublistEntry] && x.asInstanceOf[SublistEntry].subMenuRef == submenu
      ).get.asInstanceOf[SublistEntry]

    menuGroups.update(menu => {
      find(menu).subMenu.update(_ :+ SubMenuEntry(elementName, linkUrl, reference))
      menu
    })
  }

  private val selectionListener: (Event) => Unit = (event) => onMenuItemClick(event)
  private val masterSelectionListener: (Event) => Unit = (event) => onMenuItemClick(event)

  private def subMenuListener(entry: SublistEntry): (Event) => Unit = (event) => entry.visible.update(!_)

  def selectMenuItemByContent(content: String): Unit = {
    import at.happywetter.boinc.web.facade.NodeListConverter.convNodeList
    dom.document.querySelectorAll("#dashboard-menu li a").forEach((a, _, _) => {
      val element = a.asInstanceOf[HTMLAnchorElement]

      if (element.text.trim() == content)
        element.classList.add(Style.active.htmlClass)
    })

  }

  def clearSubmenus(): Unit = {
    menuComputers := List.empty
    menuGroups := List.empty
  }

}
