package at.happywetter.boinc.web.pages
import at.happywetter.boinc.web.boincclient.{ClientManager, FetchResponseException}
import at.happywetter.boinc.web.css.TopNavigation
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.pages.BoincClientLayout.Style
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.swarm.{BoincSwarmPage, ProjectSwarmPage, SwarmSubPage}
import at.happywetter.boinc.web.routes.AppRouter.{DashboardLocation, LoginPageLocation, SwarmControlLocation}
import at.happywetter.boinc.web.routes.{AppRouter, Hook, LayoutManager}
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 13.09.2017
  */
object SwarmControlPage extends Layout {
  override val path: String = "swarm"
  override val staticComponent: Option[JsDom.TypedTag[HTMLElement]] = None
  override val routerHook: Option[Hook] = Some(new Hook {
    override def already(): Unit = {
      LayoutManager.render(SwarmControlPage.this)
    }

    override def before(done: js.Function0[Unit]): Unit = {
      import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

      AuthClient.tryLogin.foreach {
        case true => done()
        case false => AppRouter.navigate(LoginPageLocation)
      }

    }

    override def leave(): Unit = {}
    override def after(): Unit = {}
  })

  private val subPages = List(
    ("boinc", "head_menu_boinc".localize, "fa fa-id-card-o"),
    ("projects", "head_menu_projects".localize, "fa fa-tag"),
    ("tasks", "head_menu_tasks".localize, "fa fa-tasks"),
    ("transfers", "head_menu_transfers".localize, "fa fa-exchange"),
    ("statistics", "head_menu_statistics".localize, "fa fa-area-chart"),
    ("disk", "head_menu_disk".localize, "fa fa-pie-chart"),
    ("global_prefs", "head_menu_prefs".localize, "fa fa-cogs")
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
      DashboardMenu.removeMenuReferences("boinc-client-entry")
      clients.foreach(client =>
        DashboardMenu.addMenu(s"${AppRouter.href(DashboardLocation)}/$client",client, Some("boinc-client-entry"))
      )

      DashboardMenu.selectByReference("swarm_control")
      AppRouter.router.updatePageLinks()
    }).recover {
      case _: FetchResponseException =>
        import scalatags.JsDom.all._
        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }
  }

  override def render: Option[JsDom.TypedTag[HTMLElement]] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    println(AppRouter.current)

    Some(
      div(
        DashboardMenu.component.render,
        div(id := "client-container", PageLayout.Style.clientContainer,

          AppRouter.current.split("/").last match {
            case "boinc" => renderSubPage(BoincSwarmPage)
            case "projects" => renderSubPage(ProjectSwarmPage)
            case _ => renderSubPage(BoincSwarmPage)
          }

        )
      )
    )
  }

  private def renderSubPage(page: SwarmSubPage): JsDom.TypedTag[HTMLElement] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    div( id := "swarm",

      h2(BoincClientLayout.Style.pageHeader,
        i(`class` := "fa fa-industry"), "swarm_header".localize, " - ",
        small(id := "subheader", page.header)
      ),

      page.render
    )
  }
}
