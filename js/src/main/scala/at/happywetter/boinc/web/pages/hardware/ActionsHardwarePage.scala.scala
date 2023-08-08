package at.happywetter.boinc.web.pages.hardware

import org.scalajs.dom.Event
import scala.concurrent.Future
import scala.scalajs.js.Dictionary
import scala.xml.Elem

import at.happywetter.boinc.shared.extension.HardwareData.Action
import at.happywetter.boinc.shared.extension.HardwareData.Actions
import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle => Style}
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.extensions.HardwareStatusClient
import at.happywetter.boinc.web.model.ActionHardwareTableModel
import at.happywetter.boinc.web.model.ActionHardwareTableModel.ActionHardwareTableRow
import at.happywetter.boinc.web.model.DataModelConverter._
import at.happywetter.boinc.web.pages.Layout
import at.happywetter.boinc.web.pages.component.{DashboardMenu, DataTable}
import at.happywetter.boinc.web.pages.swarm.HardwarePageLayout
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N._

import mhtml.Var

object ActionsHardwarePage extends HardwarePageLayout:
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  override val header: String = "actions_header".localize

  override val path: String = "actions"

  private val actions: Var[Actions] = Var(Actions(Map.empty, Map.empty))
  private val clients: Var[List[HardwareStatusClient]] = Var(List.empty)
  private val actionData = clients.zip(actions)

  override def renderChildView: Elem =
    NProgress.start()

    Future
      .sequence(
        List(
          HardwareStatusClient.queryActions
            .map(actions => {
              this.actions := actions
            })
            .recover { case e: Throwable => e.printStackTrace() },
          HardwareStatusClient.queryClients.map(clients => {
            this.clients := clients
          })
        )
      )
      .recover { case e: Throwable => e.printStackTrace() }
      .foreach(_ => NProgress.done(true))

    <div id="actions">
      <h3>Global</h3>
      <div> 
      {
      actionData.map {
        case (clients, Actions(actions, _)) => {
          println(("VAR", clients, actions))

          if (actions.isEmpty) {
            <span>No actions are defined</span>
          } else {
            val hostActionList = actions.toList.sortBy(_._1)

            <ul>
                {
              hostActionList.map { case (id, Action(icon, name, _, _)) =>
                <li>
                     <button class={Style.button.htmlClass} onclick={
                  (event: Event) => {
                    event.preventDefault()

                    NProgress.start()
                    HardwareStatusClient
                      .executeGlobalAction(id)
                      .recover { case e: Throwable => e.printStackTrace() }
                      .foreach(_ => NProgress.done(true))

                  }
                }>
        <i class={icon} style="padding-right: 4px;vertical-align: middle;"></i>
        {name}
      </button>
                    </li>
              }
            }
              </ul>
          }
        }
      }
    }
      </div>

      <h3>Host</h3>
      <div>
      {
      actionData.map {
        case (clients, Actions(_, actions)) => {
          if (actions.isEmpty) {
            <span>No actions are defined</span>
          } else {
            val hostActionList = actions.toList.sortBy(_._1)
            val tableHeaders: List[(String, Boolean)] =
              ("table_host".localize, true) :: hostActionList.map((_, action) => (action.name, false))

            val dataTable = new DataTable[ActionHardwareTableRow](tableHeaders,
                                                                  clients.map(client => {
                                                                    new ActionHardwareTableRow(client, hostActionList)
                                                                  })
            )

            dataTable.component
          }
        }
      }
    }
      </div>
    </div>
