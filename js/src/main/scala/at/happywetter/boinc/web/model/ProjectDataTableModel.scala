package at.happywetter.boinc.web.model

import org.scalajs.dom.Event
import org.scalajs.dom.MouseEvent
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.xml.Elem

import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction
import at.happywetter.boinc.shared.boincrpc.Project
import at.happywetter.boinc.web.boincclient.BoincClient
import at.happywetter.boinc.web.boincclient.BoincFormatter
import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.definitions.components.BasicModalStyle
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.css.definitions.pages.BoincProjectStyle
import at.happywetter.boinc.web.pages.component.ContextMenu
import at.happywetter.boinc.web.pages.component.DataTable
import at.happywetter.boinc.web.pages.component.DataTable.DoubleColumn
import at.happywetter.boinc.web.pages.component.DataTable.StringColumn
import at.happywetter.boinc.web.pages.component.DataTable.TableColumn
import at.happywetter.boinc.web.pages.component.Tooltip
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.dialog.ProjectInfoDialog
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.RichRx._
import at.happywetter.boinc.web.util.StatisticPlatforms
import at.happywetter.boinc.web.util.XMLHelper.toXMLTextNode

import mhtml.Rx
import mhtml.Var

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.09.2017
  */
object ProjectDataTableModel:

  implicit private class BoolVar(_var: Var[Boolean]):
    def mapBoolean[T](_true: T, _false: T): Rx[T] = _var.map(x => if (x) _true else _false)

  class ReactiveProject(val data: Project):
    val dontRequestWork: Var[Boolean] = Var(data.dontRequestWork)
    val suspended: Var[Boolean] = Var(data.suspended)

  class ProjectTableRow(val project: ReactiveProject)(implicit boinc: BoincClient, table: DataTable[ProjectTableRow])
      extends DataTable.TableRow:

    override val columns: List[DataTable.TableColumn] = List(
      new TableColumn(
        Var(
          <a href={project.data.url} onclick={AppRouter.openExternal} class={BoincProjectStyle.link.htmlClass}>
          {updateCache(project.data)}
        </a>
        ),
        this
      ) {
        override def compare(that: TableColumn): Int =
          project.data.name.compare(that.datasource.asInstanceOf[ProjectTableRow].project.data.name)
      },
      new StringColumn(Var(project.data.userName)),
      new StringColumn(Var(project.data.teamName)),
      new DoubleColumn(Var(project.data.userTotalCredit)),
      new DoubleColumn(Var(project.data.hostAvgCredit)),
      new TableColumn(
        Var(
          <div>
          {
            new Tooltip(
              project.dontRequestWork.mapBoolean("project_allow_more_work".localize,
                                                 "project_dont_allow_more_work".localize
              ),
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
        ),
        this
      ) {
        override def compare(that: TableColumn): Int = ???
      }
    )

    override val contextMenuHandler: (Event) => Unit = event => {
      val contextMenu = new ContextMenu("project-" + project.data.hashCode() + "-context-menu")
      project.data.guiURLs.foreach(url => {
        contextMenu.addMenu(url.url, url.name, AppRouter.openExternal)
      })

      event.preventDefault()
      contextMenu.renderToBody().display(event.asInstanceOf[MouseEvent])
    }

    private lazy val jsToggleWorkAction: (Event) => Unit = event => {
      event.preventDefault()
      NProgress.start()

      val action = if (!project.dontRequestWork.now) ProjectAction.NoMoreWork else ProjectAction.AllowMoreWork

      boinc
        .project(project.data.url, action)
        .map(result => {
          NProgress.done(true)

          if (!result)
            new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
              .renderToBody()
              .show()
          else {
            project.dontRequestWork.update(!_)
          }
        })
        .recover(ErrorDialogUtil.showDialog)
    }

    private lazy val jsRefreshAction: (Event) => Unit = event => {
      event.preventDefault()
      NProgress.start()

      boinc
        .project(project.data.url, ProjectAction.Update)
        .map(result => {
          NProgress.done(true)

          if (!result)
            new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
              .renderToBody()
              .show()

        })
        .recover(ErrorDialogUtil.showDialog)
    }

    private lazy val jsPauseAction: (Event) => Unit = event => {
      event.preventDefault()
      NProgress.start()

      val action = project.suspended.mapBoolean(ProjectAction.Resume, ProjectAction.Suspend).now

      boinc
        .project(project.data.url, action)
        .map(result => {
          NProgress.done(true)

          if (!result)
            new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
              .renderToBody()
              .show()
          else
            project.suspended.update(!_)

        })
        .recover(ErrorDialogUtil.showDialog)
    }

    private lazy val jsShowDetailsAction: (Event) => Unit = { event =>
      event.preventDefault()
      ProjectInfoDialog(project, jsDeleteProjectAction).renderToBody().show()
    }

    private lazy val jsDeleteProjectAction: Event => Unit = { event =>
      NProgress.start()

      boinc.project(project.data.url, action = ProjectAction.Remove).map { result =>
        if (!result)
          new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
            .renderToBody()
            .show()
        else
          table.reactiveData.update(_.filterNot(_ eq this))

        NProgress.done(true)
      }
    }

  def convert(project: Project)(implicit boinc: BoincClient, table: DataTable[ProjectTableRow]): ProjectTableRow =
    new ProjectTableRow(new ReactiveProject(project))

  private[this] def updateCache(project: Project): String =
    ProjectNameCache.save(project.url, project.name)
    project.name
