package at.happywetter.boinc.web.pages

import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.extensions.HardwareStatusClient
import at.happywetter.boinc.web.model.DataModelConverter._
import at.happywetter.boinc.web.model.HardwareTableModel
import at.happywetter.boinc.web.model.HardwareTableModel.HardwareTableRow
import at.happywetter.boinc.web.pages.component.{DashboardMenu, DataTable}
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var

import scala.concurrent.Future
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

  private val actions: Var[List[String]] = Var(List.empty)
  private val tableHeaders: List[(String, Boolean)] = List(
    ("table_host".localize, true),
    ("table_cpu_freq".localize, true),
    ("table_cpu_tmp".localize, true),
    ("table_cpu_rpm".localize, true),
    ("table_cpu_vcore".localize, true),
    ("table_cpu_12v".localize, true),
    ("", false)
  )
  private val dataTable: DataTable[HardwareTableRow] = new DataTable[HardwareTableRow](tableHeaders)

  override def beforeRender(params: Dictionary[String]): Unit = {
    HardwareStatusClient.queryClients.foreach(clients =>
      dataTable.reactiveData := HardwareTableModel.convert(clients, actions)
    )
  }

  override def already(): Unit = {
    beforeRender(Dictionary.empty)
  }

  override def onRender(): Unit = {
    DashboardMenu.selectByMenuId("dashboard_hardware")
    HardwareStatusClient.queryActions.foreach(actions =>
      this.actions := actions
    )
  }

  override def render: Elem = {
    <div id="hardware">
      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-microchip" aria-hidden="true"></i>
        {"hardware_header".localize}
      </h2>

      {dataTable.component}
    </div>
  }
}
