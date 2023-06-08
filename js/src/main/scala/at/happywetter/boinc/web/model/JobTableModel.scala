package at.happywetter.boinc.web.model

import at.happywetter.boinc.shared.rpc.jobs
import at.happywetter.boinc.shared.rpc.jobs.{Errored, Job, Running, Stopped}
import at.happywetter.boinc.web.JobManagerClient
import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle, TableTheme}
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.pages.component.DataTable.{IntegerColumn, StringColumn, TableColumn}
import at.happywetter.boinc.web.pages.component.dialog.{Dialog, OkDialog}
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N.TranslatableString
import mhtml.Var
import org.scalajs.dom.Event
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.language.implicitConversions
import at.happywetter.boinc.web.util.RichRx._

object JobTableModel:

  class JobTableRow(entry: Job) extends DataTable.TableRow:

    protected val reactiveState: Var[jobs.JobStatus] = Var(entry.status)

    override val columns: List[DataTable.TableColumn] = List(
      new StringColumn(Var(entry.name)),
      new IntegerColumn(Var(entry.action.hosts.length)),
      new StringColumn(Var(entry.mode.toString)),
      new StringColumn(Var(entry.action.toString)),
      new StringColumn(reactiveState.map(_.toString)),
      new TableColumn(
        Var(
          <div>
            {
            new Tooltip(
              reactiveState.map {
                case Running    => "job_stop".localize
                case Stopped    => "job_start".localize
                case Errored(_) => "job_start".localize
              },
              <a href="#change-job-state" onclick={jsChangeStateJobAction}>
                  <i class={
                reactiveState.map {
                  case Running    => "fas fa-stop-circle"
                  case Stopped    => "fas fa-play-circle"
                  case Errored(_) => "fas fa-play-circle"
                }
              }></i>
                </a>
            ).toXML
          }{
            new Tooltip(
              Var("job_info".localize),
              <a href="#job-info" onclick={jsJobDetailAction}>
                  <i class="fas fa-info-circle"></i>
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

    private lazy val jsChangeStateJobAction: (Event) => Unit = event => {
      event.preventDefault()
      NProgress.start()

      val action =
        reactiveState.now match
          case Running    => JobManagerClient.stop(entry).map(_ => Stopped)
          case Stopped    => JobManagerClient.start(entry).map(_ => Running)
          case Errored(_) => JobManagerClient.start(entry).map(_ => Running)

      action
        .foreach { status =>
          this.reactiveState.update(_ => status)
          NProgress.done(true)
        }
    }

    private lazy val jsJobDetailAction: (Event) => Unit = event => {
      event.preventDefault()

      new OkDialog(
        "job_details".localize + entry.name,
        List(
          <table class={TableTheme.table.htmlClass}>
            <thead></thead>
            <tbody>
              <tr><td><b>{"job_id".localize}</b></td><td>{entry.id.get.toString}</td></tr>
              <tr><td><b>{"job_name".localize}</b></td><td>{entry.name}</td></tr>
              <tr><td><b>{"job_status".localize}</b></td><td>{entry.status.toString}</td></tr>
              <tr><td><b>{"job_mode".localize}</b></td><td>{entry.mode.toString}</td></tr>
              <tr><td><b>{"job_action".localize}</b></td><td>{entry.action.toString}</td></tr>
              <tr><td><b>{"hosts".localize}</b></td>
                <td>
                  <ul>
                    {
            entry.action.hosts.map(host => <li>{host}</li>)
          }
                  </ul>
                </td>
              </tr>
            </tbody>
          </table>,
          <h4 class={BoincClientStyle.h4.htmlClass}>{"project_actions".localize}</h4>,
          <div>
            <ul class={BasicModalStyle.actionList.htmlClass}>
              <li>
                <a class={BasicModalStyle.action.htmlClass} href="#deleteJob"
                   onclick={jsDeleteJobAction}>
                  {"job_delete".localize}
                </a>
              </li>
            </ul>
          </div>
        )
      ).renderToBody()
        .show()

    }

    private val jsDeleteJobAction: (Event) => Unit = event => {
      event.preventDefault()
      NProgress.start()

      JobManagerClient
        .delete(entry)
        .foreach { _ =>
          weakTableRef.reactiveData.update(_.filterNot(_ == this))
          Dialog.hideByID("modal-dialog-type1")
          NProgress.done(true)
        }
    }

  implicit def job2TableRow(job: Job): JobTableRow = new JobTableRow(job)

  implicit def jobList2TableRows(jobs: List[Job]): List[JobTableRow] = jobs.map(job2TableRow)
