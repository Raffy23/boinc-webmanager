package at.happywetter.boinc.web.helper.table

import at.happywetter.boinc.shared.HardwareData.SensorsRow
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.extensions.HardwareStatusClient
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.pages.component.DataTable.{StringColumn, TableColumn}
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.routes.NProgress
import rx.{Ctx, Rx}
import rx.async._

import scala.concurrent.ExecutionContext.Implicits.global
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom.Event


/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
object HardwareTableModel {

  class HardwareTableRow(client: HardwareStatusClient)(implicit ctx: Ctx.Owner) extends DataTable.TableRow {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._

    private val cpuFrequ = client.getCpuFrequency
    private val sensors  = client.getSensorsData

    override val columns: List[DataTable.TableColumn] = List(
      new StringColumn(Rx{client.hostname}),
      new TableColumn(cpuFrequ.map(c => span(c.formatted("%.2f GHz"))).toRx(span("--- GHz")), this) {
        override def compare(that: TableColumn): Int =
          cpuFrequ.value.get.toOption.getOrElse(0D).compare(
            that.datasource.asInstanceOf[HardwareTableRow].cpuFrequ.value.get.toOption.getOrElse(0D)
          )
      },
      newColumn("CPU Temp", "--- GHz"),
      newColumn("CPU Fan", "--- RPM"),
      newColumn("Vcore", "--- V"),
      newColumn("+12.00V", "--- V"),
      new TableColumn(
        Rx {
         div(
           new Tooltip("more_values".localize,
             a(href := "#add-project", i(`class` := "fa fa-info-circle"), style := "color:#333;text-decoration:none;font-size:30px",
               onclick := { (event: Event) => {
                 event.preventDefault()

                 new OkDialog(
                   "hardware_header".localize + " " + client.hostname,
                   List(
                     table(TableTheme.table, TableTheme.table_lastrowsmall,
                        thead(
                          tr(
                            th("table_name"), th("table_value"),
                            th("")
                          )
                        ),
                       tbody(
                          sensors.value.get.get.toList.sortBy(_._1).map { case (name, row) =>
                            tr(
                              td(name),
                              td(row.value + " " + row.unit),
                              td(row.flags)
                            )
                          }.toList
                       )
                     )
                   )
                 ).renderToBody().show()

               }}
             )
           ).render()
         )
        }
        , this) {
        override def compare(that: TableColumn): Int = ???
      }
    )

    private def newColumn(label: String, defaultValue: String): TableColumn = {
      new TableColumn(sensors.map(c => span(c(label).toValueUnitString)).toRx(span(defaultValue)),this) {
        override def compare(that: TableColumn): Int =
          sensors.value.get.toOption.map(_(label).value).getOrElse(0D).compare(
            that.datasource.asInstanceOf[HardwareTableRow].sensors.value.get.toOption.map(_(label).value).getOrElse(0D)
          )
      }
    }
  }

  def convert(data: List[HardwareStatusClient]): List[HardwareTableRow] = Rx.unsafe {
    data.map(new HardwareTableRow(_))
  }.now

}
