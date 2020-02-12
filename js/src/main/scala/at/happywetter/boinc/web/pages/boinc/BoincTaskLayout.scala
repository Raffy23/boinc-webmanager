package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.boincrpc.Result
import at.happywetter.boinc.web.css.definitions.pages.{BoincClientStyle => BoincClientStyle}
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
import Ordering.Double.TotalOrdering

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

  private class HeaderBarData {
    var toCompute: Int = 0
    var inProgress: Int = 0
    var inTransfer: Int = 0
    var finished: Int = 0
    var allTasks: Int = 0

    def updateToCompute(value: Int): HeaderBarData = { toCompute = toCompute + value; this }
    def updateInProgress(value: Int): HeaderBarData = { inProgress = inProgress + value; this }
    def updateInTransfer(value: Int): HeaderBarData = { inTransfer = inTransfer + value; this }
    def updateFinished(value: Int): HeaderBarData = { finished = finished + value; this }
    def updateAllTasks(value: Int): HeaderBarData = { allTasks = allTasks + value; this }
  }

  private val headBarData  = Var(new HeaderBarData)

  private def loadResults(): Unit = {
    NProgress.start()

    boinc.getTasks(active = false).map(results => {
      dataTable.reactiveData := results.sortBy(f => f.activeTask.map(t => -t.done).getOrElse(0D))

      val data = new HeaderBarData
      results.foreach(x => updateTaskHeadline(data, x))
      data.updateAllTasks(results.size)

      headBarData := data

      NProgress.done(true)
    }).recover(ErrorDialogUtil.showDialog)
  }

  override def render: Elem = {
    <div id="workunits">
      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-tasks" aria-hidden="true"></i>
        {"workunit_header".localize}
        <span style="font-size:16px">
          {
            new Tooltip(
              Var("tasks_in_progress".localize),
              <span>
                {headBarData.map(_.inProgress)}
                <i class="fa fa-cogs" style="margin-left:2px"></i>
              </span>
            ).toXML
          }
          {
            new Tooltip(
              Var("tasks_in_transfer".localize),
              <span>
                {headBarData.map(_.inTransfer)}
                <i class="fa fa-exchange-alt" style="margin-left:2px"></i>
              </span>
            ).toXML
          }
          {
            new Tooltip(
              Var("tasks_to_compute".localize),
              <span>
                {headBarData.map(_.toCompute)}
                <i class="fa fa-tasks" style="margin-left:2px"></i>
              </span>
            ).toXML
          }
          {
            new Tooltip(
              Var("all_tasks".localize),
              <span>
                {headBarData.map(_.allTasks)}
                <i class="fa fa-globe" style="margin-left:2px"></i>
              </span>
            ).toXML
          }
        </span>
      </h2>
      {dataTable.component}
    </div>
  }

  private def updateTaskHeadline(data: HeaderBarData, result: Result, _value: Int = 1): Unit = {
    Result.State(result.state) match {
      case Result.State.Result_New => data.updateToCompute(_value)
      case Result.State.Result_Files_Downloaded =>

        result.activeTask.foreach(task => Result.ActiveTaskState(task.activeTaskState) match {
          case Result.ActiveTaskState.PROCESS_EXECUTING => data.updateInProgress(_value)
          case Result.ActiveTaskState.PROCESS_SUSPENDED => data.updateToCompute(_value)
          case Result.ActiveTaskState.PROCESS_EXITED => data.updateToCompute(_value)
          case Result.ActiveTaskState.PROCESS_UNINITIALIZED =>  data.updateToCompute(_value)
          case _ => data.updateFinished(_value)
        })

        if (result.activeTask.isEmpty)
          data.updateToCompute(_value)

      case Result.State.Result_Files_Uploaded => data.updateInTransfer(_value)
      case Result.State.Result_Files_Uploading => data.updateInTransfer(_value)
      case Result.State.Result_File_Downloading => data.updateInTransfer(_value)
      case Result.State.Result_Upload_Failed => data.updateInTransfer(_value)
      case x => dom.console.log("Unknown Result state: " + x)
    }
  }


  private def updateActiveTasks(): Unit = {
    val client = ClientManager.clients(boincClientName)

    client.getTasks().foreach(result => {
      result.foreach(task => {
        dataTable.reactiveData.now.find(_.result.name.now == task.name).foreach{ current =>
          Result.State(current.result.state.now) match {
            case Result.State.Result_New => headBarData.update(_.updateToCompute(-1))
            case Result.State.Result_Files_Downloaded =>

              current.result.activeTask.map(_.foreach(task => Result.ActiveTaskState(task.activeTaskState) match {
                case Result.ActiveTaskState.PROCESS_EXECUTING => headBarData.update(_.updateInProgress(-1))
                case Result.ActiveTaskState.PROCESS_SUSPENDED => headBarData.update(_.updateToCompute(-1))
                case Result.ActiveTaskState.PROCESS_EXITED => headBarData.update(_.updateToCompute(-1))
                case Result.ActiveTaskState.PROCESS_UNINITIALIZED =>  headBarData.update(_.updateToCompute(-1))
                case _ => headBarData.update(_.updateFinished(-1));
              }))

              current.result.activeTask.map(x =>
                if( x.isEmpty)
                  headBarData.update(_.updateToCompute(-1))
              )

            case Result.State.Result_Files_Uploaded => headBarData.update(_.updateInTransfer(-1))
            case Result.State.Result_Files_Uploading => headBarData.update(_.updateInTransfer(-1))
            case Result.State.Result_File_Downloading => headBarData.update(_.updateInTransfer(-1))
            case Result.State.Result_Upload_Failed => headBarData.update(_.updateInTransfer(-1))
          }


          current.result.activeTask := task.activeTask
          current.result.remainingCPU := task.remainingCPU
          current.result.supsended := task.supsended
          current.result.state := task.state
        }

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
