package at.happywetter.boinc.web.pages.boinc
import at.happywetter.boinc.shared.Result
import at.happywetter.boinc.web.boincclient._
import at.happywetter.boinc.web.helper.DataModelConverter._
import at.happywetter.boinc.web.helper.WuDataTableModel.WuTableRow
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.component.{BoincPageLayout, DataTable}
import at.happywetter.boinc.web.routes.{Hook, NProgress}
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom

import scala.collection.mutable
import scala.scalajs.js
import scalatags.JsDom.TypedTag

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
class BoincTaskLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private val tableHeaders = List(
    "table_project".localize,
    "table_progress".localize,
    "table_status".localize,
    "table_past_time".localize,
    "table_remain_time".localize,
    "table_expiry_date".localize,
    "table_application".localize,
    "" // Action Bar
  )


  private var refreshHandle: Int = _
  private var fullSyncHandle: Int = _
  private var dataTable: DataTable[WuTableRow] = _

  override def onRender(client: BoincClient): Unit = onViewRender((node) => {root.appendChild(node.render)})

  private def onViewRender(renderAction: (TypedTag[dom.html.Div]) => Unit): Unit = {
    val projectUris = new mutable.TreeSet[String]()

    boinc.getTasks(active = false).map(results => {
      val sortedResults = results.sortBy(f => f.activeTask.map(t => -t.done).getOrElse(0D))
      renderAction(renderView(projectUris, sortedResults))
    }).recover(ErrorDialogUtil.showDialog)
  }

  private def renderView(projectUris: mutable.Set[String], results: List[Result]): TypedTag[dom.html.Div] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    dataTable = new DataTable(tableHeaders, results)
    NProgress.done(true)

    div( id := "workunits",
      h2(BoincClientLayout.Style.pageHeader, i(`class` := "fa fa-tasks"), "workunit_header".localize),
      dataTable.component
    )
  }

  private def updateActiveTasks(): Unit = {
    val client = ClientManager.clients(boincClientName)

    client.getTasks().foreach(result => {
      result.foreach(task => {
        val current = dataTable.tableData.find(_.result.name.now == task.name)

        current.foreach(current => {
          current.result.activeTask() = task.activeTask
          current.result.remainingCPU() = task.remainingCPU
          current.result.supsended() = task.supsended
          current.result.state() = task.state
        })
      })
    })
  }

  private def syncTaskViewWithServer(): Unit = {
    NProgress.start()

    //TODO: Do a smart update of table rows, not view re-rendering
    val oldNode = dom.document.getElementById("workunits")
    onViewRender((newNode) => {root.replaceChild(newNode.render, oldNode)})
  }

  override val routerHook = Some(new Hook {
    override def before(done: js.Function0[Unit]): Unit = {
      NProgress.start()
      done()
    }

    override def after(): Unit = {
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
  })

  override val path = "tasks"
}
