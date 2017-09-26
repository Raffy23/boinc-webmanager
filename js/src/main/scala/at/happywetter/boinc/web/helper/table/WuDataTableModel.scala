package at.happywetter.boinc.web.helper.table

import at.happywetter.boinc.shared.BoincRPC.WorkunitAction
import at.happywetter.boinc.shared.{App, Result, Task}
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater, FetchResponseException}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.component.DataTable.{StringColumn, TableColumn}
import at.happywetter.boinc.web.pages.component.dialog.{OkDialog, SimpleModalDialog}
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.storage.{AppSettingsStorage, ProjectNameCache, TaskSpecCache}
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom.raw.Event
import rx.async._
import rx.{Ctx, Rx, Var}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Date
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.09.2017
  */
object WuDataTableModel {

  class ReactiveResult(result: Result)(implicit boinc: BoincClient, ctx: Ctx.Owner) {
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

    val progress: Rx[Double] = Rx {
      activeTask().map(_.done).getOrElse(0D)
    }

    val pastTime: Rx[Double] = Rx {
      activeTask().map(_.time).getOrElse(0D)
    }

    val appName: Var[String] = Var("")
    val app: Rx[Option[App]] =
      AppSettingsStorage.get(boinc.hostname, result.wuName).flatMap(wu => {
        appName() = wu.map(_.appName).getOrElse("")
        TaskSpecCache.get(boinc.hostname, wu.get.appName)
      }).toRx(None)


    val uiStatus: Rx[String] = Rx {
      state(); supsended()
      prettyPrintStatus(result) + prettyPrintAppStatus(app())
    }
  }

  class WuTableRow(val result: ReactiveResult)(implicit boinc: BoincClient,ctx: Ctx.Owner) extends DataTable.TableRow {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._

    override val columns = List(
      new StringColumn(result.project),
      new TableColumn( Rx {
        span(BoincClientLayout.Style.progressBar,
          JsDom.tags2.progress(
            value := result.progress(),
            max := 1,
          ),

          span(style := "float:right", (result.progress.now*100D).toString.split("\\.")(0) + " %")
        )
      }, this) {
        override def compare(that: TableColumn) = result.progress.now.compare(that.datasource.asInstanceOf[WuTableRow].result.progress.now)
      },
      new StringColumn(Rx { result.uiStatus() }),
      new TableColumn(Rx { BoincFormater.convertTime(result.pastTime()) }, this) {
        override def compare(that: TableColumn) = result.pastTime.now.compare(that.datasource.asInstanceOf[WuTableRow].result.pastTime.now)
      },
      new TableColumn(Rx { BoincFormater.convertTime(result.remainingCPU()) }, this) {
        override def compare(that: TableColumn) = result.remainingCPU.now.compare(that.datasource.asInstanceOf[WuTableRow].result.remainingCPU.now)
      },
      new TableColumn(Rx { BoincFormater.convertDate(new Date(result.reportDeadline()*1000)) }, this) {
        override def compare(that: TableColumn) = result.reportDeadline.now.compare(that.datasource.asInstanceOf[WuTableRow].result.reportDeadline.now)
      },
      new StringColumn(Rx { result.app().map(_.userFriendlyName).getOrElse(result.wuName.now).asInstanceOf[String] }),
      new TableColumn(Rx { div(
        new Tooltip(if(result.supsended()) "state_continue".localize else "state_stop".localize,
          a(href:="#", onclick := { (event: Event) => {
            event.preventDefault()
            NProgress.start()

            val action = if(result.supsended.now) WorkunitAction.Resume else WorkunitAction.Suspend
            boinc.workunit(result.projectURI, result.name.now, action).map(response => {
              if (!response) {
                new OkDialog("dialog_error_header".localize, List("not_succ_action".localize))
                  .renderToBody().show()

              } else {
                result.supsended() = !result.supsended.now
                NProgress.done(true)
              }
            }).recover(ErrorDialogUtil.showDialog)
          }},
            i(`class` := s"fa fa-${ if(result.supsended.now) "play" else "pause" }-circle-o")
          ),
          tooltipId = Some("tooltip-"+result.name.now)
        ).render(),

        new Tooltip("workunit_cancel".localize,
          a(href:="#", i(`class` := "fa fa-stop-circle-o"),
            onclick := {
              (event: Event) => {
                event.preventDefault()

                new SimpleModalDialog(
                  bodyElement = div(
                    "workunit_dialog_cancel_content".localize,p(
                      b("workunit_dialog_cancel_details".localize),br(),
                      table(
                        tbody(
                          tr(td("workunit_dialog_cancel_project".localize), td(result.project.now)),
                          tr(td("workunit_dialog_cancel_name".localize), td(result.name.now)),
                          tr(td("workunit_dialog_cancel_remaining_time".localize), td(BoincFormater.convertTime(result.remainingCPU.now)))
                        )
                      )
                    )
                  ),
                  okAction = (dialog: SimpleModalDialog) => {
                    dialog.hide()
                    boinc.workunit(result.projectURI, result.name.now, WorkunitAction.Abort).map(result => {
                      if (!result) {
                        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
                          .renderToBody().show()
                      } //TODO: Update Tasks Table
                    }).recover {
                      case _: FetchResponseException =>
                        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
                          .renderToBody().show()
                    }
                  },
                  abortAction = (dialog: SimpleModalDialog) => {dialog.hide()},
                  headerElement = div(h3("workunit_dialog_cancel_header".localize))
                ).renderToBody().show()
              }
            }
          )
        ).render(),

        new Tooltip("project_properties".localize,
          a(href:="#", i(`class` := "fa fa-info-circle"),
            onclick := {
              (event: Event) => {
                event.preventDefault()

                result.app.now.foreach(app => {
                  new OkDialog("workunit_dialog_properties".localize + " " + result.name.now, List(
                    table(TableTheme.table,
                      tbody(
                        tr(td(b("wu_dialog_appname".localize)), td(app.userFriendlyName)),
                        tr(td(b("wu_dialog_xml_appname".localize)), td(result.appName.now)),
                        tr(td(b("wu_dialog_name".localize)), td(result.name.now)),
                        tr(td(b("wu_dialog_status".localize)), td(result.uiStatus.now)),
                        tr(td(b("wu_dialog_deadline".localize)), td(BoincFormater.convertDate(result.reportDeadline.now))),
                        result.activeTask.now.map(task => {
                          List(
                            tr(td(b("wu_dialog_checkpoint_time".localize)), td(BoincFormater.convertTime(task.checkpoint))),
                            tr(td(b("wu_dialog_cpu_time".localize)), td(BoincFormater.convertTime(task.cpuTime))),
                            tr(td(b("wu_dialog_run_time".localize)), td(BoincFormater.convertTime(task.time))),
                            tr(td(b("wu_dialog_progress".localize)), td((task.done*100).formatted("%.4f %%"))),
                            tr(td(b("wu_dialog_used_ram".localize)), td(BoincFormater.convertSize(task.workingSet))),
                            tr(td(b("wu_dialog_used_disk".localize)), td(BoincFormater.convertSize(task.swapSize))),
                            tr(td(b("wu_dialog_slot".localize)), td(task.slot)),
                            tr(td(b("wu_dialog_pid".localize)), td(task.pid)),
                            tr(td(b("wu_dialog_version".localize)), td(task.appVersionNum)),
                          )
                        }),
                        tr(td(b("wu_dialog_plan_class".localize)), td(result.plan.now))
                      )
                    )
                  )).renderToBody().show()
                })
              }}
          )
        ).render()
      )}, this ) {
        override def compare(that: TableColumn): Int = ???
      }
    )
  }

  def convert(result: Result)(implicit boinc: BoincClient): WuTableRow = Rx.unsafe {
    new WuTableRow(new ReactiveResult(result))
  }.now



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
