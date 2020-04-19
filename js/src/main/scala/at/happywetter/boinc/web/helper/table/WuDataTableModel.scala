package at.happywetter.boinc.web.helper.table

import at.happywetter.boinc.shared.boincrpc.BoincRPC.WorkunitAction
import at.happywetter.boinc.shared.boincrpc.{App, Result, Task}
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater}
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.XMLHelper.toXMLTextNode
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.pages.component.DataTable.{LinkColumn, StringColumn, TableColumn}
import at.happywetter.boinc.web.pages.component.dialog.{OkDialog, SimpleModalDialog}
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.storage.{AppSettingsStorage, ProjectNameCache, TaskSpecCache}
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import mhtml.{Rx, Var}
import org.scalajs.dom.raw.Event

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import BoincFormater.Implicits._
import at.happywetter.boinc.web.css.definitions.components.TableTheme

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.09.2017
  */
object WuDataTableModel {

  class ReactiveResult(result: Result)(implicit boinc: BoincClient) {
    val activeTask: Var[Option[Task]] = Var(result.activeTask)

    val projectURI: String= result.project
    val project: Rx[String] = ProjectNameCache.get(result.project).map(_.getOrElse(result.project)).toRx(result.project)
    val name: Var[String] = Var(result.name)
    val wuName: Var[String] = Var(result.wuName)
    val platfrom: Var[String] = Var(result.platfrom)
    val version: Var[String] = Var(result.version)
    val plan: Var[String] = Var(result.plan)

    val state: Var[Int] = Var(result.state)
    val supsended: Var[Boolean] = Var(result.supsended)

    val remainingCPU: Var[Double] = Var(result.remainingCPU)
    val reportDeadline: Var[Double] = Var(result.reportDeadline)

    val progress: Rx[Double] = activeTask.map(_.map(_.done).getOrElse(0D))
    val pastTime: Rx[Double] = activeTask.map(_.map(_.time).getOrElse(0D))

    val appName: Var[String] = Var("")
    val app: Rx[Option[App]] =
      AppSettingsStorage.get(boinc.hostname, result.wuName).flatMap(wu => {
        appName := wu.map(_.appName).getOrElse("")
        TaskSpecCache.get(boinc.hostname, wu.get.appName)
      }).toRx(None)

    val uiStatus: Rx[String] =
      Var(prettyPrintStatus(result)).zip(app.map(x => prettyPrintAppStatus(x))).map(x => x._1 + x._2)
  }

  class WuTableRow(val result: ReactiveResult)(implicit boinc: BoincClient) extends DataTable.TableRow {

    override val columns = List(
      new LinkColumn(result.project.map(name => (name, result.projectURI))),
      new TableColumn( Rx {
        <span class={BoincClientStyle.progressBar.htmlClass}>
          <progress value={result.progress.map(_.toString)} max="1"></progress>
          <span>
            {
              result.progress.map(value => (value*100D).toString.split("\\.")(0) + " %")
            }
          </span>
        </span>
      }, this) {
        override def compare(that: TableColumn): Int = result.progress.now.compare(that.datasource.asInstanceOf[WuTableRow].result.progress.now)
      },
      new StringColumn(result.uiStatus),
      new StringColumn(result.pastTime.map(_.toTime)) {
        override def compare(that: TableColumn): Int = result.pastTime.now.compare(that.datasource.asInstanceOf[WuTableRow].result.pastTime.now)
      },
      new StringColumn(result.remainingCPU.map(_.toTime)) {
        override def compare(that: TableColumn): Int = result.remainingCPU.now.compare(that.datasource.asInstanceOf[WuTableRow].result.remainingCPU.now)
      },
      new StringColumn(result.reportDeadline.map(_.toDate)) {
        override def compare(that: TableColumn): Int = result.reportDeadline.now.compare(that.datasource.asInstanceOf[WuTableRow].result.reportDeadline.now)
      },
      new StringColumn(result.app.map(_.map(_.userFriendlyName).getOrElse(result.wuName.now))),
      new TableColumn(Rx {
        <div>
          {
            new Tooltip(
              result.supsended.map(v => if(v) "state_continue".localize else "state_stop".localize),
              <a href="#" onclick={jsPauseAction}>
                <i class={result.supsended.map(v => if(v) "play" else "pause").map(x => s"fas fa-$x-circle")}></i>
              </a>
            ).toXML
          }{
            new Tooltip(
              Var("workunit_cancel".localize),
              <a href="#" onclick={(event: Event) => {
                event.preventDefault()
                cancelDialog.renderToBody().show()
              }}>
                <i class="fas fa-stop-circle"></i>
              </a>
            ).toXML
          }{
            new Tooltip(
              Var("project_properties".localize),
              <a href="#" onclick={(event: Event) => {
                event.preventDefault()
                projectPropertiesDialog.renderToBody().show()
              }}>
                <i class="fas fa-info-circle"></i>
              </a>
            ).toXML
          }
        </div>
      }, this ) {
        override def compare(that: TableColumn): Int = ???
      }
    )

    private lazy val cancelDialog = new SimpleModalDialog(
      bodyElement = {
        <div>
          {"workunit_dialog_cancel_content".localize}
          <p>
            <b>{"workunit_dialog_cancel_details".localize}</b><br></br>
            <table class={TableTheme.table.htmlClass}>
              <tbody>
                <tr><td>{"workunit_dialog_cancel_project".localize}</td><td>{result.project}</td></tr>
                <tr><td>{"workunit_dialog_cancel_name".localize}</td><td>{result.name}</td></tr>
                <tr><td>{"workunit_dialog_cancel_remaining_time".localize}</td><td>{result.remainingCPU.map(_.toTime)}</td></tr>
              </tbody>
            </table>
          </p>
        </div>
      },
      okAction = (dialog: SimpleModalDialog) => {
        dialog.hide()
        boinc.workunit(result.projectURI, result.name.now, WorkunitAction.Abort).map(result => {
          if (!result) {
            new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
              .renderToBody().show()
          } //TODO: Update Tasks Table
        }).recover(ErrorDialogUtil.showDialog)
      },
      abortAction = (dialog: SimpleModalDialog) => {dialog.hide()},
      headerElement = {<h3>{"workunit_dialog_cancel_header".localize}</h3>}
    )

    private def boincApp(f: (App) => String): Rx[String] = result.app.map(_.map(f).getOrElse(""))
    private lazy val projectPropertiesDialog = new OkDialog(
      "workunit_dialog_properties".localize + " " + result.name.now,
      List(
        <table class={TableTheme.table.htmlClass}>
          <tbody>
            <tr><td><b>{"wu_dialog_appname".localize}</b></td><td>{boincApp(_.userFriendlyName)}</td></tr>
            <tr><td><b>{"wu_dialog_xml_appname".localize}</b></td><td>{result.appName}</td></tr>
            <tr><td><b>{"wu_dialog_name".localize}</b></td><td>{result.name}</td></tr>
            <tr><td><b>{"wu_dialog_status".localize}</b></td><td>{result.uiStatus}</td></tr>
            <tr><td><b>{"wu_dialog_deadline".localize}</b></td><td>{result.reportDeadline.map(_.toDate)}</td></tr>
            {
              result.activeTask.map(_.map(task => {
                List( //mthml does not like NodeBuffer -> wrap it into a list
                <tr><td><b>{"wu_dialog_checkpoint_time".localize}</b></td><td>{task.checkpoint.toTime}</td></tr>,
                <tr><td><b>{"wu_dialog_cpu_time".localize}</b></td><td>{task.cpuTime.toTime}</td></tr>,
                <tr><td><b>{"wu_dialog_run_time".localize}</b></td><td>{task.time.toTime}</td></tr>,
                <tr><td><b>{"wu_dialog_progress".localize}</b></td><td>{(task.done*100).formatted("%.4f %%")}</td></tr>,
                <tr><td><b>{"wu_dialog_used_ram".localize}</b></td><td>{task.workingSet.toSize}</td></tr>,
                <tr><td><b>{"wu_dialog_used_disk".localize}</b></td><td>{task.swapSize.toSize}</td></tr>,
                <tr><td><b>{"wu_dialog_slot".localize}</b></td><td>{task.slot}</td></tr>,
                <tr><td><b>{"wu_dialog_pid".localize}</b></td><td>{task.pid}</td></tr>,
                <tr><td><b>{"wu_dialog_version".localize}</b></td><td>{task.appVersionNum}</td></tr>
                )
              }))
            }
            <tr><td><b>{"wu_dialog_plan_class".localize}</b></td><td>{result.plan}</td></tr>
          </tbody>
        </table>
      )
    )

    private lazy val jsPauseAction: (Event) => Unit = (event) => {
      event.preventDefault()
      NProgress.start()

      val action = if(result.supsended.now) WorkunitAction.Resume else WorkunitAction.Suspend
      boinc.workunit(result.projectURI, result.name.now, action).map(response => {
        if (!response) {
          new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
            .renderToBody().show()

        } else {
          result.supsended.update(!_)
          NProgress.done(true)
        }
      }).recover(ErrorDialogUtil.showDialog)
    }
  }

  def convert(result: Result)(implicit boinc: BoincClient): WuTableRow =
    new WuTableRow(new ReactiveResult(result))

  private def prettyPrintStatus(result: Result): String = {
    Result.State(result.state) match {
      case Result.State.Result_New => "boinc_status_new".localize
      case Result.State.Result_Aborted => "boinc_status_aborted".localize
      case Result.State.Result_Compute_Error => "boinc_status_error".localize
      case Result.State.Result_Files_Downloaded =>
        result.activeTask.map(task => Result.ActiveTaskState(task.activeTaskState) match {
          case Result.ActiveTaskState.PROCESS_EXECUTING => "boinc_status_executing".localize
          case Result.ActiveTaskState.PROCESS_ABORTED => "boinc_status_abort".localize
          case Result.ActiveTaskState.PROCESS_SUSPENDED => if (result.supsended) "boinc_status_suspend1".localize else "boinc_status_suspend2".localize
          case Result.ActiveTaskState.PROCESS_EXITED => "boinc_status_exited".localize
          case Result.ActiveTaskState.PROCESS_UNINITIALIZED => "boinc_status_uninit".localize
          case state => state.toString
        }).getOrElse("boinc_state_default".localize)
      case Result.State.Result_Files_Uploaded => "boinc_status_uploaded".localize
      case Result.State.Result_Files_Uploading => "boinc_status_uploading".localize
      case Result.State.Result_File_Downloading => "boinc_status_downloading".localize
      case Result.State.Result_Upload_Failed => "boinc_status_upload_fail".localize
    }
  }

  private def prettyPrintAppStatus(app: Option[App]): String = {
    app.map(app => {
      val stringBuilder = new StringBuilder()

      if (app.nonCpuIntensive || app.version.avgCpus > 1) {
        if (app.nonCpuIntensive)
          stringBuilder.append("boinc_flags_nci".localize)

        if (app.version.avgCpus > 1) {
          stringBuilder.append(" ")
          stringBuilder.append(s" (${app.version.avgCpus} CPUs)")
        }
      }

      stringBuilder.toString()
    }).getOrElse("")
  }
}
