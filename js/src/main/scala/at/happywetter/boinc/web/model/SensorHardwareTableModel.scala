package at.happywetter.boinc.web.model

import org.scalajs.dom.Event
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Node

import at.happywetter.boinc.shared.extension.HardwareData.SensorsRow
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle => Style}
import at.happywetter.boinc.web.extensions.HardwareStatusClient
import at.happywetter.boinc.web.pages.component.DataTable.{StringColumn, TableColumn}
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.{DataTable, Tooltip}
import at.happywetter.boinc.web.routes.{NProgress, Navigo}
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.RichRx._
import at.happywetter.boinc.web.util.XMLHelper._

import mhtml.{Rx, Var}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
object SensorHardwareTableModel:

  class SensorHardwareTableRow(client: HardwareStatusClient) extends DataTable.TableRow:
    private val cpuFrequ = client.getCpuFrequency
    private val sensors = client.getSensorsData

    override val columns: List[DataTable.TableColumn] = List(
      new StringColumn(Var(client.hostname)),
      new TableColumn(cpuFrequ.map("%.2f GHz".format(_).toXML).toRx("--- GHz".toXML), this) {
        override def compare(that: TableColumn): Int =
          cpuFrequ.value.get.toOption
            .getOrElse(0d)
            .compare(
              that.datasource.asInstanceOf[SensorHardwareTableRow].cpuFrequ.value.get.toOption.getOrElse(0d)
            )
      },
      newColumn("CPU Temp", "--- GHz"),
      newColumn("CPU Fan", "--- RPM"),
      newColumn("Vcore", "--- V"),
      newColumn("+12.00V", "--- V"),
      new TableColumn(
        Var(
          <div>
            {
            new Tooltip(
              Var("more_values".localize),
              <a href="add-project" style="color:#333;text-decoration:none;font-size:30px" onclick={jsAddProjectAction}>
                  <i class="fa fa-info-circle"></i>
                </a>
            ).toXML
          }{
            <span></span>
            /*new Tooltip(
              Var("show_functions".localize),
              <a href="execute-functions" style="color:#333;text-decoration:none;font-size:30px" onclick={
                jsExecuteActions
              }>
                  <i class="fa-solid fa-book"></i>
                </a>
            ).toXML*/
          }
          </div>
        ),
        this
      ) {
        override def compare(that: TableColumn): Int = ???
      }
    )

    private def renderValue(row: SensorsRow): Node =
      if (row.flags.contains("ALARM"))
        <span>
          {
          Seq(
            row.toValueUnitString.toXML,
            Tooltip.warningTriangle("alarm".localize).toXML
          )
        }
        </span>
      else
        row.toValueUnitString.toXML

    private def newColumn(label: String, defaultValue: Node): TableColumn =
      new TableColumn(sensors.map(c => renderValue(c(label))).toRx(defaultValue), this):
        override def compare(that: TableColumn): Int =
          sensors.value.get.toOption
            .map(_(label).value)
            .getOrElse(0d)
            .compare(
              that.datasource
                .asInstanceOf[SensorHardwareTableRow]
                .sensors
                .value
                .get
                .toOption
                .map(_(label).value)
                .getOrElse(0d)
            )

    private lazy val jsAddProjectAction: (Event) => Unit = event => {
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
                    <td style={if (row.flags.contains("ALARM")) Some("color:red") else None}>
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

    /*
    private lazy val jsExecuteActions: (Event) => Unit = event => {
      event.preventDefault()

      new OkDialog(
        "execute_function".localize + " " + client.hostname,
        List(
          <ul style="list-style-type: none;">
            {
            actions.map(actions =>
              actions.map(action =>
                <button class={Style.button.htmlClass} onclick={
                  (event: Event) => {
                    event.preventDefault()

                    NProgress.start()
                    HardwareStatusClient.executeAction(client.hostname, action).foreach(_ => NProgress.done(true))

                  }
                }>{action}
                  </button>
              )
            )
          }
          </ul>
        )
      ).renderToBody().show()
    }
     */

  def convert(data: List[HardwareStatusClient]): List[SensorHardwareTableRow] =
    data.map(new SensorHardwareTableRow(_))
