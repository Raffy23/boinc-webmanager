package at.happywetter.boinc.web.pages.hardware

import scala.concurrent.Future
import scala.scalajs.js.Dictionary
import scala.xml.Elem

import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.extensions.HardwareStatusClient
import at.happywetter.boinc.web.model.DataModelConverter._
import at.happywetter.boinc.web.model.SensorHardwareTableModel
import at.happywetter.boinc.web.model.SensorHardwareTableModel.SensorHardwareTableRow
import at.happywetter.boinc.web.pages.Layout
import at.happywetter.boinc.web.pages.component.{DashboardMenu, DataTable}
import at.happywetter.boinc.web.pages.swarm.HardwarePageLayout
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N._

import mhtml.Var

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
object SensorsHardwarePage extends HardwarePageLayout:
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  override val header: String = "sensors_header".localize

  override val path: String = "sensors"

  private val tableHeaders: List[(String, Boolean)] = List(
    ("table_host".localize, true),
    ("table_cpu_freq".localize, true),
    ("table_cpu_tmp".localize, true),
    ("table_cpu_rpm".localize, true),
    ("table_cpu_vcore".localize, true),
    ("table_cpu_12v".localize, true),
    ("", false)
  )
  private val dataTable: DataTable[SensorHardwareTableRow] = new DataTable[SensorHardwareTableRow](tableHeaders)

  override def renderChildView: Elem =
    NProgress.start()
    HardwareStatusClient.queryClientsWithData.foreach(clients => {
      dataTable.reactiveData := SensorHardwareTableModel.convert(clients)
      NProgress.done(false)
    })

    <div id="sensors">
      {dataTable.component}
    </div>
