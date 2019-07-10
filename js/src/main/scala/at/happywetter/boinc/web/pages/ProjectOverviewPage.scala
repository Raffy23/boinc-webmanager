package at.happywetter.boinc.web.pages

import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.webrpc.ServerStatus
import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.hacks.Implicits.RichWindow
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.{AuthClient, FetchHelper}
import at.happywetter.boinc.web.pages.boinc.BoincClientLayout
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.{DashboardMenuBuilder, ErrorDialogUtil}
import mhtml.{Rx, Var}
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.{Elem, Node}
import at.happywetter.boinc.web.helper.XMLHelper._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
object ProjectOverviewPage extends Layout {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  override val path: String = "project_overview"

  override def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit = {
    AuthClient.validateAction(done)
  }

  private val projects: Var[List[(String, Rx[Option[ServerStatus]])]] = Var(List.empty)

  def uri(projectUrl: String) = s"/api/webrpc/status?server=${dom.window.encodeURIComponent(projectUrl)}"

  override def beforeRender(params: Dictionary[String]): Unit = {}

  override def onRender(): Unit = {
    ClientManager.readClients().map(clients => {
      DashboardMenuBuilder.renderClients(clients)

      DashboardMenu.selectByMenuId("dashboard_project_overview")
      AppRouter.router.updatePageLinks()
    }).recover(ErrorDialogUtil.showDialog)

    NProgress.start()
    ClientManager.getClients.foreach(clients => {
      Future.sequence(
        clients.map(client =>
          client.getProjects.map(_.map(p => (p.name, p.url))).recover { case _: Exception => List() }
        )
      ).foreach{ projects =>
        this.projects := projects.flatten.distinct.map { p =>
          (p._1, FetchHelper.get[ServerStatus](uri(p._2)).map(Some(_).asInstanceOf[Option[ServerStatus]]).toRx(None.asInstanceOf[Option[ServerStatus]]))
        }

        NProgress.done(true)
      }
    })
  }

  def loadingPlaceholder: Node = <i class={"fa fa-spinner fa-pulse"} color="#428bca"></i>

  def convertDaemonStatus(status: ServerStatus): Elem =
    <table>
      <thead><tr><th>Host</th><th>Status</th></tr></thead>
      <tbody>{ status.daemon_status.map{ daemon => <tr><td>{daemon.host}</td><td>{daemon.status}</td></tr>} }</tbody>
    </table>

  def convertTasks(status: ServerStatus): Elem =
    <table>
      <thead><tr><th>Task</th><th>In progress</th><th>Unsent</th></tr></thead>
      <tbody>{ status.tasks_by_app.map { tasks => <tr><td>{tasks.name}</td><td>{tasks.in_progress}</td><td>{tasks.unsent}</td></tr>} }</tbody>
    </table>

  def convertDBStatus(status: ServerStatus): Elem =
    <div> {status.database_file_states.toString} </div>

  override def render: Elem = {
    <div id="project_overview">
      <h2 class={BoincClientLayout.Style.pageHeader.htmlClass}>
        <i class="fa fa-wrench"></i>
        {"project_overview_header".localize}
      </h2>

      <table class={TableTheme.table.htmlClass}>
        <thead class={BoincClientLayout.Style.in_text_icon.htmlClass}>
          <tr><th>Project</th><th>Hosts</th><th>Tasks</th><th>Database</th></tr>
        </thead>
        <tbody>
          {
            projects.map { projects =>
              projects.map { project =>
                <tr>
                  <td>{project._1}</td>
                  <td>{ project._2.map(option => option.map(convertDaemonStatus).getOrElse(loadingPlaceholder)) }</td>
                  <td>{ project._2.map(option => option.map(convertTasks).getOrElse(loadingPlaceholder)) }</td>
                  <td>{ project._2.map(option => option.map(convertDBStatus).getOrElse(loadingPlaceholder)) }</td>
                </tr>
              }
            }
          }
        </tbody>
      </table>

    </div>
  }
}
