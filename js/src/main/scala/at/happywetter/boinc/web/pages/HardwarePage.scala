package at.happywetter.boinc.web.pages
import at.happywetter.boinc.web.boincclient.{ClientManager, FetchResponseException}
import at.happywetter.boinc.web.extensions.HardwareStatusClient
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.helper.table.HardwareTableModel
import at.happywetter.boinc.web.helper.table.HardwareTableModel.HardwareTableRow
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.{DashboardMenu, DataTable}
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.routes.AppRouter.LoginPageLocation
import at.happywetter.boinc.web.util.DashboardMenuBuilder
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
object HardwarePage extends Layout {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  override val path: String = "hardware"

  override def before(done: js.Function0[Unit]): Unit = {
    AuthClient.tryLogin.foreach {
      case true => done()
      case false => AppRouter.navigate(LoginPageLocation)
    }

  }

  private var clients: Future[List[HardwareStatusClient]] = _
  private var dataTable: DataTable[HardwareTableRow] = _
  private val tableHeaders: List[(String, Boolean)] = List(
    ("table_host".localize, true),
    ("table_cpu_freq".localize, true),
    ("table_cpu_tmp".localize, true),
    ("table_cpu_rpm".localize, true),
    ("table_cpu_vcore".localize, true),
    ("table_cpu_12v".localize, true),
    ("", false)
  )

  override def beforeRender(params: Dictionary[String]): Unit = {
    clients = HardwareStatusClient.queryClients.map(_.sortBy(_.hostname))
  }


  override def onRender(): Unit = {
    ClientManager.readClients().map(clients => {
      DashboardMenuBuilder.renderClients(clients)

      DashboardMenu.selectByReference("hardware")
      AppRouter.router.updatePageLinks()
    }).recover {
      case _: FetchResponseException =>
        import scalatags.JsDom.all._
        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }

    clients.foreach(clients => {
      val body = dom.document.getElementById("hardware")

      dataTable = new DataTable(
        tableHeaders, HardwareTableModel.convert(clients)
      )

      body.appendChild(dataTable.component)
    })
  }

  override def render: Elem = {
    <div id="hardware">
      <h2 class={BoincClientLayout.Style.pageHeader.htmlClass}>
        <i class="fa fa-microchip"></i>
        {"hardware_header".localize}
      </h2>
    </div>
  }
}
