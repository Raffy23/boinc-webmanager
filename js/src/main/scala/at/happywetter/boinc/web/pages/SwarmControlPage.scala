package at.happywetter.boinc.web.pages
import at.happywetter.boinc.web.boincclient.{ClientManager, FetchResponseException}
import at.happywetter.boinc.web.css.TopNavigation
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.helper.XMLHelper._
import at.happywetter.boinc.web.pages.BoincClientLayout.Style
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.swarm.{BoincSwarmPage, ProjectSwarmPage, SwarmSubPage}
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.routes.AppRouter.{LoginPageLocation, SwarmControlLocation}
import at.happywetter.boinc.web.util.DashboardMenuBuilder
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 13.09.2017
  */
object SwarmControlPage extends Layout {
  override val path: String = "swarm"

  override def before(done: js.Function0[Unit]): Unit = {
    AuthClient.tryLogin.foreach {
      case true => done()
      case false => AppRouter.navigate(LoginPageLocation)
    }
  }

  private val subPages = List(
    ("boinc", "head_menu_boinc".localize, "fa fa-id-card-o"),
    ("projects", "head_menu_projects".localize, "fa fa-tag"),

    //TODO: Implement Pages:
    //("tasks", "head_menu_tasks".localize, "fa fa-tasks"),
    //("transfers", "head_menu_transfers".localize, "fa fa-exchange"),
    //("statistics", "head_menu_statistics".localize, "fa fa-area-chart"),
    //("disk", "head_menu_disk".localize, "fa fa-pie-chart"),
    //("global_prefs", "head_menu_prefs".localize, "fa fa-cogs")
  )

  override def beforeRender(params: Dictionary[String]): Unit = {
    val nav = dom.document.getElementById("navigation")
    nav.innerHTML = ""

    nav.appendChild({
      import scalacss.ScalatagsCss._
      import scalatags.JsDom.all._

      ul(TopNavigation.nav, id := "swarm_top_navbar",
        subPages.map { case (nav, name, icon) =>
          li(Style.in_text_icon,
            a(
              href := s"${SwarmControlLocation.link}/$nav",
              i(`class` := icon), span(TopNavigation.invisible_on_small_screen, name),
              data("navigo") := "",
              `class` := (if (path == nav) TopNavigation.active.htmlClass else "")
            )
          )
        }
      ).render
    })
  }

  override def onRender(): Unit = {
    ClientManager.readClients().map(clients => {
      DashboardMenuBuilder.renderClients(clients)

      DashboardMenu.selectByReference("swarm_control")
      AppRouter.router.updatePageLinks()
    }).recover {
      case _: FetchResponseException =>

        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }
  }

  override def render: Elem = {
   <div>
     {
       AppRouter.current.split("/").last match {
         case "boinc"     => renderSubPage(BoincSwarmPage)
         case "projects"  => renderSubPage(ProjectSwarmPage)
         case _ =>
           <div>
              <h4 class={BoincClientLayout.Style.pageHeader.htmlClass}>{"not_found".localize}</h4>
           </div>
       }
     }
   </div>
  }

  private def renderSubPage(page: SwarmSubPage): Elem = {
    <div id="swarm">
      <h2 class={BoincClientLayout.Style.pageHeader.htmlClass}>
        <i class="fa fa-industry">
          {"swarm_header".localize + " - "}
          <small id="subheader">{page.header}</small>
        </i>
      </h2>

      {page.render}
    </div>
  }
}
