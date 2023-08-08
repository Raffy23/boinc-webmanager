package at.happywetter.boinc.web.model

import org.scalajs.dom
import org.scalajs.dom.Event
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Try
import scala.xml.Text

import at.happywetter.boinc.shared.boincrpc.{CCState, FileTransfer}
import at.happywetter.boinc.web.boincclient.BoincFormatter.Implicits._
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormatter}
import at.happywetter.boinc.web.pages.component.DataTable.{DoubleColumn, LinkColumn, StringColumn, TableColumn}
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.util.I18N._

import mhtml.{Rx, Var}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 19.04.2020
 */
object FileTransferTableModel:

  class FileTransferTableRow(private val entry: FileTransfer, ccstate: Var[Option[CCState]])(implicit
    boinc: BoincClient
  ) extends DataTable.TableRow:
    override val columns: List[DataTable.TableColumn] = List(
      new LinkColumn(Var((entry.projectName, entry.projectUrl))),
      new StringColumn(Var(entry.name)),
      new TableColumn(content = Var(Text(entry.byte.toSize)), this, dataEntry = Some("number")) {
        override def compare(that: TableColumn): Int =
          entry.byte.compareTo(that.datasource.asInstanceOf[FileTransferTableRow].entry.byte)
      },
      new TableColumn(content = Var(Text(entry.fileXfer.bytesXfered.toSize)), this, dataEntry = Some("number")) {
        override def compare(that: TableColumn): Int = entry.fileXfer.bytesXfered
          .compareTo(that.datasource.asInstanceOf[FileTransferTableRow].entry.fileXfer.bytesXfered)
      },
      new TableColumn(content = Var(Text(entry.fileXfer.xferSpeed.toSpeed)), this, dataEntry = Some("number")) {
        override def compare(that: TableColumn): Int = entry.fileXfer.xferSpeed
          .compareTo(that.datasource.asInstanceOf[FileTransferTableRow].entry.fileXfer.xferSpeed)
      },
      new TableColumn(content = Var(Text(entry.xfer.timeSoFar.toTime)), this, dataEntry = Some("number")) {
        override def compare(that: TableColumn): Int =
          entry.xfer.timeSoFar.compareTo(that.datasource.asInstanceOf[FileTransferTableRow].entry.xfer.timeSoFar)
      },
      new StringColumn(ccstate.map(ccstate => buildStatusField(entry, ccstate))),
      new TableColumn(
        Rx {
          <div>
          {
            new Tooltip(Var("retry_file_transfer".localize),
                        <a href="#" onclick={jsRetryAction}>
                <i class="fas fa-redo"></i>
              </a>
            ).toXML
          }
        </div>
        },
        this
      ) {
        override def compare(that: TableColumn): Int = 0
      }
    )

    private lazy val jsRetryAction: Event => Unit = _ => {
      boinc.retryFileTransfer(entry.projectUrl, entry.name).foreach { result =>
        if (!result)
          dom.console.error(s"retryFileTransfer for ${entry.projectUrl} (${entry.name}) failed!")
      }
    }

  private def buildStatusField(transfer: FileTransfer, state: Option[CCState]): String =
    val builder = new StringBuilder()

    if (transfer.xfer.isUpload) builder.append("upload".localize)
    else builder.append("download".localize)

    if (transfer.projectBackoff > 0)
      builder.append(", ")
      builder.append("retry_at".localize.format(BoincFormatter.convertDate(transfer.xfer.nextRequest)))

    Try(
      builder.append(
        FileTransfer.Status(transfer.status) match {
          case FileTransfer.Status.GiveUpDownload => s", ${"failed".localize}"
          case FileTransfer.Status.GiveUpUpload   => s", ${"failed".localize}"
          case _                                  => ""
        }
      )
    ).recover:
      case e: Exception =>
        e.printStackTrace()
        builder.append(", Status: " + transfer.status)

    if (
      state.map(state => CCState.State(state.networkStatus)).getOrElse(CCState.State.Disabled) == CCState.State.Disabled
    )
      builder.append(", ")
      builder.append("disabled".localize)

    builder.toString()
