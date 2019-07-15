package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.helper.ServerConfig
import at.happywetter.boinc.web.pages.{Dashboard, HardwarePage, SettingsPage, WebRPCProjectPage}
import at.happywetter.boinc.web.pages.swarm.BoincSwarmPage
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

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

    private val menuMargin = 10

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
      zIndex :=! "99",

      media.maxWidth(690 px)(
        top(100 px)
      )
    )

    val elem = style(
      unsafeChild("a")(
        display.block,
        width(207 px),
        textDecoration := "none",
        padding(menuMargin px, 15 px),
        boxSizing.borderBox,
        color(c"#333"),

        &.hover(
          backgroundColor(c"#74a9d8"),
          color.white
        ),

        unsafeChild("i")(
          marginRight(menuMargin px)
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

    val subMenuHosts = style(
      float.right,
      margin(0 px, (-1 * menuMargin + 5) px, 0 px, 0 px),
      fontSize.smaller,
      backgroundColor(c"#757575"),
      color.white,
      minWidth(20 px),
      border(1 px, solid, c"#757575"),
      borderRadius(6 px),
      textAlign.center
    )
  }

  private val viewState: Var[String] = Var("display:block")
  private val hwMenuEntry: Var[Elem] = Var(<li><span id="config-hardware-disabled"></span></li>)
  processSeverConfig()

  trait MenuEntry
  case class TopLevelEntry(name: String, href: String, reference: Option[String]=None) extends MenuEntry
  case class SublistEntry(name: String, href: String, visible: Var[Boolean] = Var(false),
                          subMenuRef: String, reference: Option[String]=None,
                          subMenu: Var[List[SubMenuEntry]] = Var(List.empty)) extends MenuEntry
  case class SubMenuEntry(name: String, href: String, reference: Option[String]=None) extends MenuEntry

  private val menuEntries: Var[List[MenuEntry]] = Var(List.empty[MenuEntry])

  def component: Elem = {
    <ul class={Style.menu.htmlClass} id="dashboard-menu" style={viewState}>
      <li class={Style.elem.htmlClass}>
        <a href={Dashboard.link} onclick={masterSelectionListener}
           data-navigo={true} data-menu-id="dashboard">
          <i class="fa fa-tachometer"></i>
          {"dashboard_menu_home".localize}
        </a>
      </li>
      <li class={Style.elem.htmlClass}>
        <a href={BoincSwarmPage.link} onclick={masterSelectionListener}
           data-navigo={true} data-menu-id="swarm_control">
          <i class="fa fa-industry"></i>
          {"dashboard_swarm_control".localize}
        </a>
      </li>

      {hwMenuEntry}

      <li class={Style.elem.htmlClass}>
        <a href={SettingsPage.link} onclick={masterSelectionListener}
           data-navigo={true} data-menu-id="settings">
          <i class="fa fa-cog"></i>
          {"dashboard_menu_settings".localize}
        </a>
      </li>

      <li class={Style.elem.htmlClass}>
        <a href={WebRPCProjectPage.link} onclick={masterSelectionListener}
           data-navigo={true} data-menu-id="dashboard_webrpc">
          <i class="fa fa-cloud"></i>
          {"dashboard_webrpc".localize}
        </a>
      </li>


    <li><hr id="menu-entry-spliter"/></li>

      <li class={Style.elem.htmlClass}>
        <h2 style="padding-left:5px">
          <i class="fa fa-cubes"  style="margin-right:8px"></i>
          {"dashboard_menu_computers".localize}
        </h2>
      </li>
      {
        menuEntries.map(_.map {
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

  def processSeverConfig(): Unit = {
    ServerConfig.get.foreach(config => {
      if (config == null)
        dom.console.error("ServerConfig is null!")

      if (config.hardware) {
        hwMenuEntry :=
          <li class={Style.elem.htmlClass}>
            <a href={HardwarePage.link} onclick={masterSelectionListener}
               data-navigo={true} data-menu-id="dashboard_hardware">
              <i style="margin-right:14px" class="fa fa-microchip"></i>{"dashboard_hardware".localize}
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
      import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
      elements.forEach((node, _, _) => {
        menuNode.removeChild(node.parentNode)
      })
    }
  }

  def addMenu(linkUrl: String, elementName: String, reference: Option[String] = None): Unit =
    menuEntries.update(_ :+ TopLevelEntry(elementName, linkUrl, reference))

  def addSubMenu(elementName: String, menuReference: String, reference: Option[String] = None): Unit =
    menuEntries.update(_ :+ SublistEntry(elementName, "#", Var(false), menuReference, reference))

  def addSubMenuItem(linkUrl: String, elementName: String, submenu: String, reference: Option[String] = None): Unit = {
    def find(menu: List[MenuEntry]): SublistEntry =
      menu.find(x =>
        x.isInstanceOf[SublistEntry] && x.asInstanceOf[SublistEntry].subMenuRef == submenu
      ).get.asInstanceOf[SublistEntry]

    menuEntries.update(menu => {
      find(menu).subMenu.update(_ :+ SubMenuEntry(elementName, linkUrl, reference))
      menu
    })
  }

  private val selectionListener: (Event) => Unit = (event) => onMenuItemClick(event)
  private val masterSelectionListener: (Event) => Unit = (event) => onMenuItemClick(event)

  private def subMenuListener(entry: SublistEntry): (Event) => Unit = (event) => entry.visible.update(!_)

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
