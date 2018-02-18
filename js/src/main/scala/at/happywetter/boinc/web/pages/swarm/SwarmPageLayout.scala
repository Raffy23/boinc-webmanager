package at.happywetter.boinc.web.pages.swarm

import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.pages.{Layout, PageLayout}
import at.happywetter.boinc.web.pages.boinc.BoincClientLayout
import at.happywetter.boinc.web.pages.component.topnav.SwarmTopNavigation
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.routes.AppRouter.LoginPageLocation

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.pages.component.DashboardMenu

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.02.2018
  */
abstract class SwarmPageLayout extends Layout {

  override def before(done: js.Function0[Unit]): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    AuthClient.tryLogin.foreach {
      case true => done()
      case false => AppRouter.navigate(LoginPageLocation)
    }
  }

  override def render: Elem = {
    <div id="swarm">
      <h2 class={BoincClientLayout.Style.pageHeader.htmlClass}>
        <i class="fa fa-industry">
          {"swarm_header".localize + " - "}
          <small id="subheader">{header.localize}</small>
        </i>
      </h2>

      {renderChildView}
    </div>
  }

  override def beforeRender(params: Dictionary[String]): Unit = {
    PageLayout.showMenu()

    SwarmTopNavigation.render(path)
    PageLayout.nav := SwarmTopNavigation.component.now

    DashboardMenu.selectByMenuId("swarm_control")
  }

  def renderChildView: Elem

  val header: String
}
