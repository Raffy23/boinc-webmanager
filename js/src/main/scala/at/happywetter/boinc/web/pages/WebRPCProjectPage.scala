package at.happywetter.boinc.web.pages

import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.webrpc.ServerStatus
import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.definitions.components.{FloatingMenu, TableTheme}
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.css.definitions.pages.LoginPageStyle
import at.happywetter.boinc.web.facade.Implicits.RichWindow
import at.happywetter.boinc.web.util.FetchHelper.FetchRequest
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.{AuthClient, DashboardMenuBuilder, ErrorDialogUtil, FetchHelper}
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLSelectElement

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem

/**
  * Created by:
  *
  * @author Raphael
  * @version 03.11.2017
  */
object WebRPCProjectPage extends Layout {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  override val path: String = "project_overview"

  private val projects: Var[Map[String, String]] = Var(Map.empty)
  private val currentSelection: Var[Option[Either[ServerStatus, Exception]]] = Var(None)
  private val selectorPlaceholder: Var[String] = Var("fetching_projects".localize)

  private var currentRequest: Option[FetchRequest[ServerStatus]] = None

  def uri(projectUrl: String) = s"/webrpc/status?server=${dom.window.encodeURIComponent(projectUrl)}"

  override def beforeRender(params: Dictionary[String]): Unit = {
    currentSelection := None
    selectorPlaceholder := "fetching_projects".localize
  }

  override def already(): Unit = {
    NProgress.start()
    onRender()
  }

  override def onRender(): Unit = {
    DashboardMenu.selectByMenuId("dashboard_webrpc")

    ClientManager.getClients.foreach(clients => {
      Future.sequence(
        clients.map(client =>
          client
            .asQueryOnlyHealthy()
            .getProjects
            .map(_.map(p => (p.url, p.name)))
            .recover { case _: Exception => List.empty }
        )
      ).foreach { projects =>
        this.projects := projects.flatten.toMap
        this.selectorPlaceholder := "project_new_default_select".localize

        NProgress.done(true)
      }
    })
  }

  def convertDaemonStatus(status: ServerStatus): Elem =
    <table class={TableTheme.table.htmlClass}>
      <thead class={BoincClientStyle.inTextIcon.htmlClass}><tr><th>Host</th><th>Command</th><th>Status</th></tr></thead>
      <tbody>{ status.daemon_status.map{ daemon => <tr><td>{daemon.host}</td><td>{daemon.command}</td><td>{daemon.status}</td></tr>} }</tbody>
    </table>

  def convertTasks(status: ServerStatus): Elem =
    <table class={TableTheme.table.htmlClass}>
      <thead class={BoincClientStyle.inTextIcon.htmlClass}><tr><th>Task</th><th>In progress</th><th>Unsent</th></tr></thead>
      <tbody>{ status.tasks_by_app.map { tasks => <tr><td>{tasks.name}</td><td>{tasks.in_progress}</td><td>{tasks.unsent}</td></tr>} }</tbody>
    </table>

  def convertDBStatus(status: ServerStatus): Elem =
    <table class={TableTheme.table.htmlClass}>
      <tbody>
        <tr><td>{"computing_power".localize}</td><td>{status.database_file_states.current_floating_point_speed}</td></tr>
        <tr><td>{"hosts_registered_24h".localize}</td><td>{status.database_file_states.hosts_registered_in_past_24_hours}</td></tr>
        <tr><td>{"hosts_with_credit".localize}</td><td>{status.database_file_states.hosts_with_credit}</td></tr>
        <tr><td>{"hosts_with_recent_credit".localize}</td><td>{status.database_file_states.hosts_with_recent_credit}</td></tr>
        <tr><td>{"users_registered_24h".localize}</td><td>{status.database_file_states.users_registered_in_past_24_hours}</td></tr>
        <tr><td>{"users_with_credit".localize}</td><td>{status.database_file_states.users_with_credit}</td></tr>
        <tr><td>{"users_with_recent_credit".localize}</td><td>{status.database_file_states.users_with_recent_credit}</td></tr>
        <tr><td>{"results_in_progess".localize}</td><td>{status.database_file_states.results_in_progress}</td></tr>
        <tr><td>{"results_ready_to_send".localize}</td><td>{status.database_file_states.results_ready_to_send}</td></tr>
        <tr><td>{"results_waiting_for_deletion".localize}</td><td>{status.database_file_states.results_waiting_for_deletion}</td></tr>
        <tr><td>{"wu_assimilation".localize}</td><td>{status.database_file_states.workunits_waiting_for_assimilation}</td></tr>
        <tr><td>{"wu_deletion".localize}</td><td>{status.database_file_states.workunits_waiting_for_deletion}</td></tr>
        <tr><td>{"wu_validation".localize}</td><td>{status.database_file_states.workunits_waiting_for_validation}</td></tr>
        <tr><td>{"backlog_in_h".localize}</td><td>{status.database_file_states.transitioner_backlog_hours}</td></tr>
      </tbody>
    </table>


  override def render: Elem =
    <div id="project_overview">

      <div class={FloatingMenu.root.htmlClass} style="border-left:solid 1px #AAA;border-right:solid 1px #AAA;border-top:solid 1px #AAA;margin-top:-6px">
        {
          <select class={LoginPageStyle.input.htmlClass} style="margin:0" id="project" onchange={jsOnChangeListener}>
            <option disabled={true} selected="selected">{selectorPlaceholder}</option>
            {
              projects.map(_.toList.sortBy(_._2.toLowerCase()).map(project => <option value={project._1}>{project._2}</option>))
            }
          </select>
        }
      </div>

      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-cloud" aria-hidden="true"></i>
        {"project_overview_header".localize}
      </h2>

      {
        currentSelection.map {
          case None => <br/>
          case Some(Right(ex)) =>
            <div>
                <i class="fa fa-exclamation-triangle" aria-hidden="true"></i>
                <b>{"webrpc_could_not_load_project_status".localize}</b>
            </div>
          case Some(Left(status)) =>
            <div>
              <h3>{"daemon_status".localize}</h3>
              { convertDaemonStatus(status) }

              <h3>{"task_status".localize}</h3>
              { convertTasks(status) }

              <h3>{"database_status".localize}</h3>
              { convertDBStatus(status) }

            </div>
        }
      }

    </div>

  private lazy val jsOnChangeListener: Event => Unit = event => {
    NProgress.start()
    currentSelection := None
    currentRequest.foreach(_.controller.abort())

    val resp = FetchHelper.getCancelable[ServerStatus](uri(event.target.asInstanceOf[HTMLSelectElement].value))
    currentRequest = Some(resp)

    resp.future.map { status =>
      currentRequest = None
      currentSelection := Some(Left(status))
      NProgress.done(true)
    }.recover {
      case _: js.JavaScriptException =>
        // Do nothing since new request already started
        dom.console.log("Aborted fetch request ...")

      case ex: Exception =>
        NProgress.done(true)
        currentSelection := Some(Right(ex))
    }

  }

}
