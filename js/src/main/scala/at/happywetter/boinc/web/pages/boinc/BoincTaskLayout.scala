package at.happywetter.boinc.web.pages.boinc
import at.happywetter.boinc.shared.Result
import at.happywetter.boinc.web.boincclient._
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.table.DataModelConverter._
import at.happywetter.boinc.web.helper.table.WuDataTableModel.WuTableRow
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var
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

  private val tasksToCompute  = Var(0)
  private val tasksInProgress = Var(0)
  private val tasksInTransfer = Var(0)
  private val tasksFinished   = Var(0)
  private val allTasks        = Var(0)

  private def loadResults(): Unit = {
    NProgress.start()

    boinc.getTasks(active = false).map(results => {
      dataTable.reactiveData := results.sortBy(f => f.activeTask.map(t => -t.done).getOrElse(0D))

      tasksToCompute := 0
      tasksInTransfer:= 0
      tasksInProgress:= 0

      results.foreach(updateTaskHeadline(_))
      allTasks := results.size

      NProgress.done(true)
    }).recover(ErrorDialogUtil.showDialog)
  }

  override def render: Elem = {
    <div id="workunits">
      <h2 class={BoincClientLayout.Style.pageHeader.htmlClass}>
        <i class="fa fa-tasks"></i>
        {"workunit_header".localize}
        <span style="font-size:16px">
          {
            new Tooltip(
              Var("tasks_in_progress".localize),
              <span>
                {tasksInProgress}
                <i class="fa fa-cogs" style="margin-left:2px"></i>
              </span>
            ).toXML
          }
          {
            new Tooltip(
              Var("tasks_in_transfer".localize),
              <span>
                {tasksInTransfer}
                <i class="fa fa-exchange" style="margin-left:2px"></i>
              </span>
            ).toXML
          }
          {
            new Tooltip(
              Var("tasks_to_compute".localize),
              <span>
                {tasksToCompute}
                <i class="fa fa-tasks" style="margin-left:2px"></i>
              </span>
            ).toXML
          }
          {
            new Tooltip(
              Var("all_tasks".localize),
              <span>
                {allTasks}
                <i class="fa fa-globe" style="margin-left:2px"></i>
              </span>
            ).toXML
          }
        </span>
      </h2>
      {dataTable.component}
    </div>
  }

  private def updateTaskHeadline(result: Result, _value: Int = 1): Unit = {
    Result.State(result.state) match {
      case Result.State.Result_New => tasksToCompute.update(x => x + _value)
      case Result.State.Result_Files_Downloaded =>

        result.activeTask.foreach(task => Result.ActiveTaskState(task.activeTaskState) match {
          case Result.ActiveTaskState.PROCESS_EXECUTING => tasksInProgress.update(x => x + _value)
          case Result.ActiveTaskState.PROCESS_ABORTED => tasksFinished.update(x => x + _value)
          case Result.ActiveTaskState.PROCESS_SUSPENDED => tasksToCompute.update(x => x + _value)
          case Result.ActiveTaskState.PROCESS_EXITED => tasksToCompute.update(x => x + _value)
          case Result.ActiveTaskState.PROCESS_UNINITIALIZED => tasksToCompute.update(x => x + _value)
        })

        if (result.activeTask.isEmpty)
          tasksToCompute.update(x => x + _value)

      case Result.State.Result_Files_Uploaded => tasksInTransfer.update(x => x + _value)
      case Result.State.Result_Files_Uploading => tasksInTransfer.update(x => x + _value)
      case Result.State.Result_File_Downloading => tasksInTransfer.update(x => x + _value)
      case Result.State.Result_Upload_Failed => tasksInTransfer.update(x => x + _value)
    }
  }


  private def updateActiveTasks(): Unit = {
    val client = ClientManager.clients(boincClientName)

    client.getTasks().foreach(result => {
      result.foreach(task => {
        val current = dataTable.tableData.find(_.result.name.now == task.name)

        //TODO: Update Task headline

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
