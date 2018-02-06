package at.happywetter.boinc.web.pages.boinc
import at.happywetter.boinc.web.boincclient._
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.table.DataModelConverter._
import at.happywetter.boinc.web.helper.table.WuDataTableModel.WuTableRow
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.component.DataTable
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom

import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
class BoincTaskLayout extends BoincClientLayout {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  override val path = "tasks"

  private def tableHeaders = List(
    ("table_project".localize, true),
    ("table_progress".localize, true),
    ("table_status".localize, true),
    ("table_past_time".localize, true),
    ("table_remain_time".localize, true),
    ("table_expiry_date".localize, true),
    ("table_application".localize, true),
    ("", false) // Action Bar
  )

  private var refreshHandle: Int = _
  private var fullSyncHandle: Int = _
  private val dataTable: DataTable[WuTableRow] = new DataTable[WuTableRow](tableHeaders)

  private def loadResults(): Unit = {
    NProgress.start()

    boinc.getTasks(active = false).map(results => {
      dataTable.reactiveData := results.sortBy(f => f.activeTask.map(t => -t.done).getOrElse(0D))
      NProgress.done(true)
    }).recover(ErrorDialogUtil.showDialog)
  }

  override def render: Elem = {
    <div id="workunits">
      <h2 class={BoincClientLayout.Style.pageHeader}>
        <i class="fa fa-tasks"></i>
        {"workunit_header".localize}
      </h2>
      {dataTable.component}
    </div>
  }

  private def updateActiveTasks(): Unit = {
    val client = ClientManager.clients(boincClientName)

    client.getTasks().foreach(result => {
      result.foreach(task => {
        val current = dataTable.tableData.find(_.result.name.now == task.name)

        current.foreach(current => {
          current.result.activeTask := task.activeTask
          current.result.remainingCPU := task.remainingCPU
          current.result.supsended := task.supsended
          current.result.state := task.state
        })
      })
    })
  }

  private def syncTaskViewWithServer(): Unit = loadResults()

  override def after(): Unit = {
    loadResults()

    refreshHandle = dom.window.setInterval(() => updateActiveTasks(), 5000)
    fullSyncHandle = dom.window.setInterval(() => syncTaskViewWithServer(), 600000)
  }

  override def leave(): Unit = {
    dom.window.clearInterval(refreshHandle)
    dom.window.clearInterval(fullSyncHandle)
  }

  override def already(): Unit = {
    syncTaskViewWithServer()
  }

}
