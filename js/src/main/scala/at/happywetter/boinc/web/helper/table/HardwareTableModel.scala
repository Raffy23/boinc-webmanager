package at.happywetter.boinc.web.helper.table

import at.happywetter.boinc.shared.extension.HardwareData.SensorsRow
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.extensions.HardwareStatusClient
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.XMLHelper._
import at.happywetter.boinc.web.pages.component.DataTable.{StringColumn, TableColumn}
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.util.I18N._
import mhtml.Var
import org.scalajs.dom.Event

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Node

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
object HardwareTableModel {

  class HardwareTableRow(client: HardwareStatusClient) extends DataTable.TableRow {
    private val cpuFrequ = client.getCpuFrequency
    private val sensors  = client.getSensorsData

    override val columns: List[DataTable.TableColumn] = List(
      new StringColumn(Var(client.hostname)),
      new TableColumn(cpuFrequ.map(_.formatted("%.2f GHz").toXML).toRx("--- GHz".toXML), this) {
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
        Var (
           new Tooltip(
             Var("more_values".localize),
             <a href="add-project" style="color:#333;text-decoration:none;font-size:30px" onclick={jsAddProjectAction}>
               <i class="fa fa-info-circle"></i>
             </a>
           ).toXML
        )
        , this) {
        override def compare(that: TableColumn): Int = ???
      }
    )

    private def renderValue(row: SensorsRow): Node = {
      if (row.flags.contains("ALARM")) {
        <span>
          {
           Seq(
             row.toValueUnitString.toXML,
             Tooltip.warningTriangle("alarm".localize).toXML
           )
          }
        </span>
      } else {
        row.toValueUnitString.toXML
      }
    }

    private def newColumn(label: String, defaultValue: Node): TableColumn = {
      new TableColumn(sensors.map(c => renderValue(c(label))).toRx(defaultValue),this) {
        override def compare(that: TableColumn): Int =
          sensors.value.get.toOption.map(_(label).value).getOrElse(0D).compare(
            that.datasource.asInstanceOf[HardwareTableRow].sensors.value.get.toOption.map(_(label).value).getOrElse(0D)
          )
      }
    }

    private lazy val jsAddProjectAction: (Event) => Unit = (event) => {
      event.preventDefault()

      new OkDialog(
        "hardware_header".localize + " " + client.hostname,
        List(
          <table class={Seq(TableTheme.table.htmlClass, TableTheme.lastRowSmall.htmlClass).mkString(" ")}>
            <thead>
              <tr>
                <th>{"table_sensor".localize}</th><th>{"table_value".localize}</th><th></th>
              </tr>
            </thead>
            <tbody>
              {
                sensors.value.get.get.toList.sortBy(_._1).map { case (name, row) =>
                  <tr>
                    <td>{name}</td>
                    <td style={if(row.flags.contains("ALARM")) Some("color:red") else None}>
                      {row.toValueUnitString}
                    </td>
                    <td>{row.flags}</td>
                  </tr>
                }
              }
            </tbody>
          </table>
        )
      ).renderToBody().show()
    }
  }

  def convert(data: List[HardwareStatusClient]): List[HardwareTableRow] =
    data.map(new HardwareTableRow(_))

}
