package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.boincrpc.{CCState, FileTransfer}
import at.happywetter.boinc.web.boincclient.BoincFormater
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var

import scala.util.Try
import scala.xml.Elem
import BoincFormater.Implicits._
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.helper.table.FileTransferTableModel.FileTransferTableRow
import at.happywetter.boinc.web.helper.table.WuDataTableModel.WuTableRow
import at.happywetter.boinc.web.pages.component.DataTable
import at.happywetter.boinc.web.util.GlobalOptions
import org.scalajs.dom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 30.08.2017
  */
class BoincFileTransferLayout extends BoincClientLayout {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  override val path = "transfers"

  private def tableHeaders = List(
    ("table_project".localize, true),
    ("table_task".localize, true),
    ("table_transfer_size".localize, true),
    ("table_transfer".localize, true),
    ("table_speed".localize, true),
    ("table_transfer_time".localize, true),
    ("table_status".localize, false)
  )

  private val ccState: Var[Option[CCState]] = Var(Option.empty)
  private val dataTable = new DataTable[FileTransferTableRow](tableHeaders, paged = true)

  private var syncTimerID: Int = _

  private def loadData(refresh: Boolean = true): Unit = {
    if (!refresh)
      boinc.getCCState.foreach(state => ccState := Some(state))

    boinc.getFileTransfer.foreach{ fileTransfers =>
      dataTable.reactiveData := fileTransfers.map(new FileTransferTableRow(_, ccState))
    }
  }

  override def already(): Unit = {
    loadData(false)
  }

  override def leave(): Unit = {
    dom.window.clearInterval(syncTimerID)
  }

  override def after(): Unit = {
    syncTimerID = dom.window.setTimeout(() => loadData(), GlobalOptions.refreshDetailPageTimeout)
  }

  override def render: Elem = {
    loadData(false)

    <div id="file_transfer">
      <h3 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-exchange-alt" aria-hidden="true"></i>
        {"file_transfer_header".localize}
      </h3>
      { dataTable.component }
    </div>
  }


}
