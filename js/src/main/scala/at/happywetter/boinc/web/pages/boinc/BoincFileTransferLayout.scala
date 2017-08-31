package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater, FetchResponseException}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.pages.component.dialog.OkDialog

import scala.scalajs.js
import at.happywetter.boinc.web.util.I18N._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 30.08.2017
  */
class BoincFileTransferLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {

  override def onRender(client: BoincClient): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getFileTransfer.map(transfers => {
      root.appendChild(
        div(id := "file_transfer",
          h3(BoincClientLayout.Style.pageHeader, "file_transfer_header".localize),
          table(TableTheme.table,
            thead(
              tr(
                th("table_project".localize), th("table_task".localize), th("table_transfer".localize),
                th("table_speed".localize), th("table_transfer_time".localize), th("table_status".localize)
              )
            ),
            tbody(
              transfers.map(transfer => {
                tr(
                  td(transfer.projectName),
                  td(transfer.name),
                  td(BoincFormater.convertSize(transfer.fileXfer.bytesXfered)),
                  td(BoincFormater.convertSize(transfer.fileXfer.xferSpeed) + " /s"),
                  td(BoincFormater.convertTime(transfer.xfer.timeSoFar)),
                  td(transfer.status)
                )
              })
            )
          )
        ).render
      )
    }).recover {
      case _: FetchResponseException =>
        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }
  }

}
