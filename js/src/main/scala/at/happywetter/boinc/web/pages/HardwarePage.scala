package at.happywetter.boinc.web.pages
import at.happywetter.boinc.shared.HardwareData
import at.happywetter.boinc.web.boincclient.{ClientCacheHelper, ClientManager, FetchResponseException}
import at.happywetter.boinc.web.extensions.HardwareStatusClient
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.helper.table.HardwareTableModel
import at.happywetter.boinc.web.helper.table.HardwareTableModel.HardwareTableRow
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.{DashboardMenu, DataTable}
import at.happywetter.boinc.web.routes.AppRouter.LoginPageLocation
import at.happywetter.boinc.web.routes.{AppRouter, Hook, LayoutManager}
import at.happywetter.boinc.web.util.DashboardMenuBuilder
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scalatags.JsDom
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
object HardwarePage extends Layout {
  override val path: String = "hardware"
  override val staticComponent: Option[JsDom.TypedTag[HTMLElement]] = None
  override val routerHook: Option[Hook] =  Some(new Hook {
    override def already(): Unit = {
      LayoutManager.render(HardwarePage.this)
    }

    override def before(done: js.Function0[Unit]): Unit = {
      import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

      AuthClient.tryLogin.foreach {
        case true => done()
        case false => AppRouter.navigate(LoginPageLocation)
      }

    }

    override def leave(): Unit = {
      dataTable.dispose()
    }
    override def after(): Unit = {}
  })

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

  override def render: Option[JsDom.TypedTag[HTMLElement]] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    Some(
      div(
        DashboardMenu.component.render,
        div(id := "client-container", PageLayout.Style.clientContainer,
          div(id := "hardware",
            h2(BoincClientLayout.Style.pageHeader,
              i(`class` := "fa fa-microchip"), "hardware_header".localize)
          )
        )
      )
    )
  }
}
