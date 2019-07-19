package at.happywetter.boinc.web.helper.table

import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction
import at.happywetter.boinc.shared.boincrpc.Project
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.boinc.{BoincClientLayout, BoincProjectLayout}
import at.happywetter.boinc.web.pages.component.DataTable.{DoubleColumn, StringColumn, TableColumn}
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.{ContextMenu, DataTable, Tooltip}
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.{ErrorDialogUtil, StatisticPlatforms}
import mhtml.{Rx, Var}
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.Event

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.XMLHelper.toXMLTextNode

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.09.2017
  */
object ProjectDataTableModel {

  private implicit class BoolVar(_var: Var[Boolean]) {
    def mapBoolean[T](_true: T, _false: T): Rx[T] = _var.map(x => if(x) _true else _false)
  }

  class ReactiveProject(val data: Project) {
    val dontRequestWork: Var[Boolean] = Var(data.dontRequestWork)
    val suspended: Var[Boolean] = Var(data.suspended)
  }

  class ProjectTableRow(val project: ReactiveProject)(implicit boinc: BoincClient) extends DataTable.TableRow {
    
    override val columns: List[DataTable.TableColumn] = List(
      new TableColumn(Var(
        <a href={project.data.url} onclick={AppRouter.openExternal} class={BoincProjectLayout.Style.link.htmlClass}>
          {updateCache(project.data)}
        </a>
      ), this) {
        override def compare(that: TableColumn): Int = project.data.name.compare(that.datasource.asInstanceOf[ProjectTableRow].project.data.name)
      },
      new StringColumn(Var(project.data.userName)),
      new StringColumn(Var(project.data.teamName)),
      new DoubleColumn(Var(project.data.hostAvgCredit)),
      new DoubleColumn(Var(project.data.userTotalCredit)),
      new TableColumn( Var(
        <div>
          {
            new Tooltip(
              project.dontRequestWork.mapBoolean("project_allow_more_work".localize, "project_dont_allow_more_work".localize),
              <a href="#change-project-state" onclick={jsToggleWorkAction}>
                {
                  project.dontRequestWork.mapBoolean(
                    <i class="fas fa-lock" style="color:#dc5050;font-size:20px"></i>,
                    <i class="fas fa-unlock" style="color:#2bab41;font-size:20px"></i>
                  )
                }
              </a>
            ).toXML
          }{
            new Tooltip(
              project.suspended.mapBoolean("project_running".localize, "project_suspended".localize),
              <a href="#refresh-project" onclick={jsPauseAction}>
                <i class={project.suspended.mapBoolean("play", "pause").map(x => s"fas fa-$x-circle")}></i>
              </a>
            ).toXML
          }{
          new Tooltip(
            Var("project_refresh".localize),
            <a href="#pause-project" onclick={jsRefreshAction}>
              <i class="fa fa-fw fa-sync" style="font-size:20px"></i>
            </a>
          ).toXML
          }{
            new Tooltip(
              Var("project_properties".localize),
              <a href="#project-properties" onclick={jsShowDetailsAction}>
                <i class="fa fa-info-circle"></i>
              </a>
            ).toXML
          }

        </div>
      ), this) {
        override def compare(that: TableColumn): Int = ???
      }

    )

    override val contextMenuHandler: (Event) => Unit = (event) => {
      val contextMenu = new ContextMenu("project-"+project.data.hashCode()+"-context-menu")
      project.data.guiURLs.foreach( url => {
        contextMenu.addMenu(url.url, url.name, AppRouter.openExternal)
      })

      event.preventDefault()
      contextMenu.renderToBody().display(event.asInstanceOf[MouseEvent])
    }

    private lazy val jsToggleWorkAction: (Event) => Unit = (event) => {
      event.preventDefault()
      NProgress.start()

      val action = if (!project.dontRequestWork.now) ProjectAction.NoMoreWork else ProjectAction.AllowMoreWork

      boinc.project(project.data.url, action).map(result => {
        NProgress.done(true)

        if (!result)
          new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
            .renderToBody().show()
        else {
          project.dontRequestWork.update(!_)
        }
      }).recover(ErrorDialogUtil.showDialog)
    }

    private lazy val jsRefreshAction: (Event) => Unit = (event) => {
      event.preventDefault()
      NProgress.start()

      boinc.project(project.data.url, ProjectAction.Update).map(result => {
        NProgress.done(true)

        if (!result)
          new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
            .renderToBody().show()

      }).recover(ErrorDialogUtil.showDialog)
    }

    private lazy val jsPauseAction: (Event) => Unit = (event) => {
      event.preventDefault()
      NProgress.start()

      val action = project.suspended.mapBoolean(ProjectAction.Resume, ProjectAction.Suspend).now

      boinc.project(project.data.url, action).map(result => {
        NProgress.done(true)

        if (!result)
          new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
            .renderToBody().show()
        else
          project.suspended.update(!_)

      }).recover(ErrorDialogUtil.showDialog)
    }

    private lazy val jsShowDetailsAction: (Event) => Unit = (event) => {
      event.preventDefault()

      new OkDialog("workunit_dialog_properties".localize + " " + project.data.name,
        List(
          <h4 class={BoincClientLayout.Style.h4.htmlClass}>{"project_dialog_general_header".localize}</h4>,
          <table class={TableTheme.table.htmlClass}>
            <tbody>
              <tr>
                <td><b>{"project_dialog_url".localize}</b></td>
                <td>
                  <a class={BoincProjectLayout.Style.link.htmlClass} href={project.data.url} onclick={AppRouter.openExternal}>
                    {project.data.url}
                  </a>
                </td>
              </tr>
              <tr><td><b>{"login_username".localize}</b></td><td>{project.data.userName}</td></tr>
              <tr><td><b>{"project_dialog_teamname".localize}</b></td><td>{project.data.teamName}</td></tr>
              <tr><td><b>{"resource_share".localize}</b></td><td>{project.data.resourceShare}</td></tr>
              <tr><td><b>{"disk_usage".localize}</b></td><td>{BoincFormater.convertSize(project.data.desiredDiskUsage)}</td></tr>
              <tr><td><b>{"project_dialog_cpid".localize}</b></td><td>{project.data.cpid}</td></tr>
              <tr>
                <td><b>{"project_dialog_host_id".localize}</b></td>
                <td>
                  {project.data.hostID}
                  <span style="float:right">
                    <a href={StatisticPlatforms.freedc(project.data.cpid)} onclick={AppRouter.openExternal}>
                      <img src="/files/images/freedc_icon.png" alt="freedc-icon"></img>
                    </a>
                    <a href={StatisticPlatforms.boincStats(project.data.cpid)} onclick={AppRouter.openExternal}>
                      <img src="/files/images/boincstats_icon.png" alt="boincstats-icon"></img>
                    </a>
                  </span>
                </td>

              </tr>
              <tr><td><b>{"project_dialog_paused".localize}</b></td><td>{project.dontRequestWork.map(_.localize)}</td></tr>
              <tr><td><b>{"project_dialog_jobs_succ".localize}</b></td><td>{project.data.jobSucc}</td></tr>
              <tr><td><b>{"project_dialog_jobs_err".localize}</b></td><td>{project.data.jobErrors}</td></tr>
            </tbody>
          </table>,
          <h4 class={BoincClientLayout.Style.h4.htmlClass}>{"project_dialog_credits_header".localize}</h4>,
          <table class={TableTheme.table.htmlClass}>
            <tbody>
              <tr><td><b>{"project_dialog_credits_user".localize}</b></td><td>{project.data.userTotalCredit}</td></tr>
              <tr><td><b>{"project_dialog_credits_uavg".localize}</b></td><td>{project.data.userAvgCredit}</td></tr>
              <tr><td><b>{"project_dialog_credits_host".localize}</b></td><td>{project.data.hostTotalCredit}</td></tr>
              <tr><td><b>{"project_dialog_credits_havg".localize}</b></td><td>{project.data.hostAvgCredit}</td></tr>
            </tbody>
          </table>
        )
      ).renderToBody().show()
    }
  }

  def convert(project: Project)(implicit boinc: BoincClient): ProjectTableRow =
    new ProjectTableRow(new ReactiveProject(project))

  private[this] def updateCache(project: Project): String = {
    ProjectNameCache.save(project.url, project.name)
    project.name
  }
}
