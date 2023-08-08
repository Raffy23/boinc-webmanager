package at.happywetter.boinc.web.pages.swarm

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem

import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.pages.component.topnav.HardwareTopNavigation
import at.happywetter.boinc.web.pages.component.topnav.SwarmTopNavigation
import at.happywetter.boinc.web.pages.{Layout, PageLayout}
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.{AuthClient, DashboardMenuBuilder}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.02.2018
  */
abstract class HardwarePageLayout extends Layout:

  override def link: String = "/view/hardware/" + path

  override def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit =
    AuthClient.validateAction(done)

  override def already(): Unit = onRender()

  override def render: Elem =
    <div id="hardware">
      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-microchip" aria-hidden="true"></i>
        {"hardware_header".localize + " - "}
        <small id="subheader">{header}</small>
      </h2>

      {renderChildView}
    </div>

  override def beforeRender(params: Dictionary[String]): Unit =
    PageLayout.showMenu()

    DashboardMenuBuilder.renderClients()
    HardwareTopNavigation.render(Some(path))

    DashboardMenu.selectByMenuId("dashboard_hardware")

  def renderChildView: Elem

  val header: String
