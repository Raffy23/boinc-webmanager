package at.happywetter.boinc.web.pages.settings

import at.happywetter.boinc.shared.rpc.HostDetails
import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.model.HostDetailsTableModel
import at.happywetter.boinc.web.model.HostDetailsTableRow
import at.happywetter.boinc.web.pages.Layout
import at.happywetter.boinc.web.pages.component.{DashboardMenu, DataTable}
import at.happywetter.boinc.web.pages.component.topnav.SettingsTopNavigation
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Dictionary
import scala.xml.Elem

/**
 * Created by: 
 *
 * @author Raphael
 * @version 09.07.2020
 */
object HostSettings extends Layout {

  override val path: String = "settings/hosts"

  private val dataTable = DataTable.of(HostDetailsTableModel)

  override def render: Elem =
    <div>
      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-cog" aria-hidden="true"></i>
        {"settings_header".localize} -  <small>{"host_details".localize}</small>
      </h2>

      { dataTable.component }
    </div>

  override def already(): Unit = {
    loadData()
  }

  override def beforeRender(params: Dictionary[String]): Unit = {
    SettingsTopNavigation.render(Some("hosts"))
    loadData()
  }

  override def onRender(): Unit = {
    DashboardMenu.selectByMenuId("settings")
  }

  private def loadData(): Unit = {
    ClientManager.queryClientDetails().foreach { data =>
      dataTable.reactiveData := HostDetailsTableModel.convert(data)
      NProgress.done(true)
    }
  }

}
