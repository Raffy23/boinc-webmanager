package at.happywetter.boinc.web.pages
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction
import at.happywetter.boinc.shared.rpc.jobs
import at.happywetter.boinc.shared.rpc.jobs.{Job, Running}
import at.happywetter.boinc.web.JobManagerClient
import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.definitions.pages.{BoincClientStyle, BoincProjectStyle => Style}
import at.happywetter.boinc.web.model.JobTableModel.JobTableRow
import at.happywetter.boinc.web.pages.WebRPCProjectPage.removeTrailingSlash
import at.happywetter.boinc.web.pages.component.dialog.{JobAddDialog, OkDialog}
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
    ("table_hosts".localize, true),
    ("table_mode".localize, true),
    ("table_action".localize, true),
    ("", false)
  )

  private val dataTable: DataTable[JobTableRow] = new DataTable[JobTableRow](tableHeaders, paged = true)

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
        <i class="fas fa-calendar-alt" aria-hidden="true"></i>
        {"job_manager_header".localize}
      </h2>

      <div class={Style.floatingHeadbar.htmlClass}>
        {
        new Tooltip(
          Var("job_new_tooltip".localize),
          <a href="#add-job" class={Style.floatingHeadbarButton.htmlClass} onclick={jsJobAddAction}>
            <i class="fa fa-plus-square"></i>
          </a>
        ).toXML
        }
      </div>

      {dataTable.component}
    </div>


  private val jsJobAddAction: (Event) => Unit = (event) => {
    event.preventDefault()
    new JobAddDialog(jobOpt => {
      jobOpt.foreach(job => dataTable.reactiveData.update(l => job :: l))
      if (jobOpt.isEmpty) {
        new OkDialog("error", List(<p>{"could_not_create_job".localize}</p>)).renderToBody().show()
      }
    }).renderToBody().show()

    /*
    NProgress.start()

    // TODO: THIS:
    val j = Job(None, "TEST", jobs.Once, jobs.BoincProjectAction(List("Localhost"), "<invalid>", ProjectAction.Update), Running)
    val r = JobManagerClient.create(j)

    r.map(job => dataTable.reactiveData.update(l => job :: l))
     .recover(_.printStackTrace())
     .foreach(_ => NProgress.done(true))
    */
  }
}
