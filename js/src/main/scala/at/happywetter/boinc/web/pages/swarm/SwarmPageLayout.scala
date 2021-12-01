package at.happywetter.boinc.web.pages.swarm

import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.pages.component.topnav.SwarmTopNavigation
import at.happywetter.boinc.web.pages.{Layout, PageLayout}
import at.happywetter.boinc.web.util.{AuthClient, DashboardMenuBuilder}
import at.happywetter.boinc.web.util.I18N._

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.02.2018
  */
abstract class SwarmPageLayout extends Layout {

  override def link: String = "/view/swarm/" + path

  override def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit = {
    AuthClient.validateAction(done)
  }

  override def render: Elem = {
    <div id="swarm">
      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-industry" aria-hidden="true"></i>
        {"swarm_header".localize + " - "}
        <small id="subheader">{header}</small>
      </h2>

      {renderChildView}
    </div>
  }

  override def beforeRender(params: Dictionary[String]): Unit = {
    PageLayout.showMenu()

    DashboardMenuBuilder.renderClients()
    SwarmTopNavigation.render(Some(path))

    DashboardMenu.selectByMenuId("swarm_control")
  }

  def renderChildView: Elem

  val header: String
}
