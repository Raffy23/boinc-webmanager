package at.happywetter.boinc.web.util

import at.happywetter.boinc.web.boincclient.FetchResponseException
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.routes.NProgress

import I18N._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 21.09.2017
  */
object ErrorDialogUtil {
  import XMLHelper._

  val showDialog: PartialFunction[Throwable, Unit] = {
    case ex: FetchResponseException if ex.statusCode == 500 =>
      printStackTraceAndStopProgress(ex)

      new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
        .renderToBody().show()

    case ex: FetchResponseException =>
      printStackTraceAndStopProgress(ex)

      new OkDialog("dialog_error_header".localize, List(ex.reason.localize))
        .renderToBody().show()

    case ex: Exception =>
      printStackTraceAndStopProgress(ex)

      new OkDialog("dialog_error_header".localize, List(
        "ups_something_went_wrong".localize, <br/>, ex.getLocalizedMessage)
      ).renderToBody().show()

  }

  def apply(): PartialFunction[Throwable, Unit] = ErrorDialogUtil.showDialog

  private def printStackTraceAndStopProgress(ex: Throwable): Unit = {
    NProgress.done(true)
    ex.printStackTrace()
  }

}
