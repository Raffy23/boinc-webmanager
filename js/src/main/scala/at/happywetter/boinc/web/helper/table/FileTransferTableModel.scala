package at.happywetter.boinc.web.helper.table

import at.happywetter.boinc.shared.boincrpc.{CCState, FileTransfer}
import at.happywetter.boinc.web.boincclient.BoincFormater
import at.happywetter.boinc.web.pages.component.DataTable
import at.happywetter.boinc.web.pages.component.DataTable.{DoubleColumn, LinkColumn, StringColumn, TableColumn}
import at.happywetter.boinc.web.boincclient.BoincFormater.Implicits._
import mhtml.Var
import at.happywetter.boinc.web.util.I18N._

import scala.util.Try
import scala.xml.Text

/**
 * Created by: 
 *
 * @author Raphael
 * @version 19.04.2020
 */
object FileTransferTableModel {

  class FileTransferTableRow(private val entry: FileTransfer, ccstate: Var[Option[CCState]]) extends DataTable.TableRow {
    override val columns: List[DataTable.TableColumn] = List(
      new LinkColumn(Var((entry.projectName, entry.projectUrl))),
      new StringColumn(Var(entry.name)),
      new TableColumn(content = Var(Text(entry.byte.toSize)), this, dataEntry = Some("number")) {
        override def compare(that: TableColumn): Int = entry.byte.compareTo(that.datasource.asInstanceOf[FileTransferTableRow].entry.byte)
      },
      new TableColumn(content = Var(Text(entry.fileXfer.bytesXfered.toSize)), this, dataEntry = Some("number")) {
        override def compare(that: TableColumn): Int = entry.fileXfer.bytesXfered.compareTo(that.datasource.asInstanceOf[FileTransferTableRow].entry.fileXfer.bytesXfered)
      },
      new TableColumn(content = Var(Text(entry.fileXfer.xferSpeed.toSpeed)), this, dataEntry = Some("number")) {
        override def compare(that: TableColumn): Int = entry.fileXfer.xferSpeed.compareTo(that.datasource.asInstanceOf[FileTransferTableRow].entry.fileXfer.xferSpeed)
      },
      new TableColumn(content = Var(Text(entry.xfer.timeSoFar.toTime)), this, dataEntry = Some("number")) {
        override def compare(that: TableColumn): Int = entry.xfer.timeSoFar.compareTo(that.datasource.asInstanceOf[FileTransferTableRow].entry.xfer.timeSoFar)
      },
      new StringColumn(Var(entry.name)),
      new StringColumn(ccstate.map(ccstate => buildStatusField(entry, ccstate)))
    )
  }

  private def buildStatusField(transfer: FileTransfer, state: Option[CCState]): String = {
    val builder = new StringBuilder()

    if (transfer.xfer.isUpload) builder.append("upload".localize)
    else builder.append("download".localize)

    if (transfer.projectBackoff > 0) {
      builder.append(", ")
      builder.append("retry_at".localize.format(BoincFormater.convertDate(transfer.xfer.nextRequest)))
    }

    Try(
      builder.append(
        FileTransfer.Status(transfer.status) match {
          case FileTransfer.Status.GiveUpDownload => s", ${"failed".localize}"
          case FileTransfer.Status.GiveUpUpload => s", ${"failed".localize}"
          case _ => ""
        }
      )
    ).recover{
      case e: Exception =>
        e.printStackTrace()
        builder.append(", Status: " + transfer.status)
    }

    if (state.map(state => CCState.State(state.networkStatus)).getOrElse(CCState.State.Disabled) == CCState.State.Disabled) {
      builder.append(", ")
      builder.append("disabled".localize)
    }

    builder.toString()
  }

}
