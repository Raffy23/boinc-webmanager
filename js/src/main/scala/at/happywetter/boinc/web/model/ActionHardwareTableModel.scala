package at.happywetter.boinc.web.model

import org.scalajs.dom.Event
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Node

import at.happywetter.boinc.shared.extension.HardwareData.Action
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
object ActionHardwareTableModel:

  class ActionHardwareTableRow(client: HardwareStatusClient, actions: List[(String, Action)])
      extends DataTable.TableRow:

    override val columns: List[DataTable.TableColumn] =
      new StringColumn(Var(client.hostname)) :: actions.map(action => {
        new TableColumn(Var(tableCell(action)), this):
          override def compare(that: TableColumn): Int = 0
      })

    private def tableCell(action: (String, Action)): Node =
      <button class={Style.button.htmlClass} onclick={
        (event: Event) => {
          event.preventDefault()

          NProgress.start()
          HardwareStatusClient
            .executeAction(client.hostname, action._1)
            .recover { case e: Throwable => e.printStackTrace() }
            .foreach(_ => NProgress.done(true))
        }
      }>
        <i class={action._2.icon} style="padding-right: 4px;vertical-align: middle;"></i>
        {action._2.name}
      </button>

  def convert(data: List[HardwareStatusClient], actions: List[(String, Action)]): List[ActionHardwareTableRow] =
    data.map(new ActionHardwareTableRow(_, actions))
