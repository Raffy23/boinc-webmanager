package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.boincrpc.FileTransfer
import at.happywetter.boinc.web.boincclient.BoincFormater
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var

import scala.util.Try
import scala.xml.Elem
import BoincFormater.Implicits._
import at.happywetter.boinc.web.css.definitions.components.TableTheme

/**
  * Created by: 
  *
  * @author Raphael
  * @version 30.08.2017
  */
class BoincFileTransferLayout extends BoincClientLayout {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  override val path = "transfers"

  private val data = Var(List.empty[FileTransfer])

  override def render: Elem = {
    boinc.getFileTransfer.foreach(fileTransfer => data := fileTransfer)

    <div id="file_transfer">
      <h3 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-exchange-alt" aria-hidden="true"></i>
        {"file_transfer_header".localize}
      </h3>
      <table class={TableTheme.table.htmlClass}>
        <thead>
          <tr>
            <th>{"table_project".localize}</th><th>{"table_task".localize}</th>
            <th>{"table_transfer_size".localize}</th>
            <th>{"table_transfer".localize}</th> <th>{"table_speed".localize}</th>
            <th>{"table_transfer_time".localize}</th> <th>{"table_status".localize}</th>
          </tr>
        </thead>
        <tbody>
          {
            data.map(_.map(transfer => {
              <tr>
                <td>{transfer.projectName}</td>
                <td>{transfer.name}</td>
                <td>{transfer.byte.toSize}</td>
                <td>{transfer.fileXfer.bytesXfered.toSize}</td>
                <td>{transfer.fileXfer.xferSpeed.toSpeed}</td>
                <td>{transfer.xfer.timeSoFar.toTime}</td>
                <td>{buildStatusField(transfer)}</td>
              </tr>
            }))
          }
        </tbody>
      </table>
    </div>
  }

  def buildStatusField(transfer: FileTransfer): String = {
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

    builder.toString()
  }

}
