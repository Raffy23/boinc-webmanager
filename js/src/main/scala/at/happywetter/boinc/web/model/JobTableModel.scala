package at.happywetter.boinc.web.model

import at.happywetter.boinc.shared.rpc.jobs.Job
import at.happywetter.boinc.web.JobManagerClient
import at.happywetter.boinc.web.pages.JobManagerPage
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.pages.component.DataTable.{StringColumn, TableColumn}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N.TranslatableString
import mhtml.Var
import org.scalajs.dom.raw.Event

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

object JobTableModel {

  class JobTableRow(entry: Job) extends DataTable.TableRow {
    override val columns: List[DataTable.TableColumn] = List(
      new StringColumn(Var(entry.id.get.toString)),
      new StringColumn(Var(entry.mode.toString)),
      new StringColumn(Var(entry.action.toString)),
      new TableColumn(
        Var (
          <div>
            {
              new Tooltip(
                Var("job_stop".localize),
                <a href="#stop-job" onclick={jsStopJobAction}>
                  <i class="fas fa-pause-circle"></i>
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

        )
        , this) {
        override def compare(that: TableColumn): Int = ???
      }
    )

    private lazy val jsStopJobAction: (Event) => Unit = (event) => {
      NProgress.start()

      JobManagerClient
        .delete(entry)
        .foreach { _ =>
          weakTableRef.reactiveData.update(_.filterNot(_ == this))
          NProgress.done(true)
        }
    }
    private lazy val jsJobDetailAction: (Event) => Unit = (event) => { /* TODO: Implement function */ }
  }

  implicit def job2TableRow(job: Job): JobTableRow = new JobTableRow(job)

  implicit def jobList2TableRows(jobs: List[Job]): List[JobTableRow] = jobs.map(job2TableRow)

}
