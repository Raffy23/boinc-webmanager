package at.happywetter.boinc.web.pages
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction
import at.happywetter.boinc.shared.rpc.jobs
import at.happywetter.boinc.shared.rpc.jobs.Job
import at.happywetter.boinc.web.JobManagerClient
import at.happywetter.boinc.web.css.definitions.pages.{BoincClientStyle, BoincProjectStyle => Style}
import at.happywetter.boinc.web.model.JobTableModel.JobTableRow
import at.happywetter.boinc.web.pages.component.{DashboardMenu, DataTable, Tooltip}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N.TranslatableString
import mhtml.Var
import org.scalajs.dom.Event

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Dictionary
import scala.xml.Elem

object JobManagerPage extends Layout {

  override val path: String = "jobs"

  private val tableHeaders: List[(String, Boolean)] = List(
    ("table_job_name".localize, true),
    ("table_mode".localize, true),
    ("table_action".localize, true),
    ("", false)
  )

  private val dataTable: DataTable[JobTableRow] = new DataTable[JobTableRow](tableHeaders)

  override def beforeRender(params: Dictionary[String]): Unit = {
    JobManagerClient
      .all()
      .map(dataTable.reactiveData := _)
      .foreach(_ => NProgress.done(true))
  }

  override def already(): Unit = {
    JobManagerClient.all().map(dataTable.reactiveData := _)
  }

  override def onRender(): Unit = {
    DashboardMenu.selectByMenuId("jobs")
  }

  override def render: Elem =
    <div id="job_manager">
      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fas fa-cogs" aria-hidden="true"></i>
        {"job_manager_header".localize}
      </h2>

      <div class={Style.floatingHeadbar.htmlClass}>
        {
        new Tooltip(
          Var("job_new_tooltip".localize),
          <a href="#add-job" class={Style.floatingHeadbarButton.htmlClass} onclick={jsProjectAddAction}>
            <i class="fa fa-plus-square"></i>
          </a>
        ).toXML
        }
      </div>

      {dataTable.component}
    </div>


  private val jsProjectAddAction: (Event) => Unit = (event) => {
    event.preventDefault()
    NProgress.start()

    // TODO: THIS:
    val j = Job(None, jobs.Once, jobs.BoincProjectAction("Localhost", "<invalid>>", ProjectAction.Update))
    val r = JobManagerClient.create(j)

    r.map(job => dataTable.reactiveData.update(l => job :: l))
     .recover(_.printStackTrace())
     .foreach(_ => NProgress.done(true))

  }
}
