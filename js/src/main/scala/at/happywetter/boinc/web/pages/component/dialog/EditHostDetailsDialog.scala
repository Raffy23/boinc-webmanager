package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.css.definitions.components.{Dialog => DialogStyle}
import at.happywetter.boinc.web.model.HostDetailsTableRow
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.RichRx._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLInputElement
import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Created by: 
 *
 * @author Raphael
 * @version 10.07.2020
 */
object EditHostDetailsDialog {

  def apply(hostDetails: HostDetailsTableRow): Dialog =
    new SimpleModalDialog(
      <div>
        <table>
          <tbody>
            <tr>
              <td><b>{"hostname".localize}</b></td>
              <td>{hostDetails.name}</td>
            </tr>
            <tr>
              <td><b>{"address".localize}</b></td>
              <td><input id="host_details_address" value={hostDetails.address}></input></td>
            </tr>
            <tr>
              <td><b>{"port".localize}</b></td>
              <td><input id="host_details_port" type="number" value={hostDetails.port.map(_.toString)}></input></td>
            </tr>
            <tr>
              <td><b>{"password".localize}</b></td>
              <td><input id="host_details_password" type="password" value={hostDetails.password}></input></td>
            </tr>
          </tbody>
        </table>
      </div>,
      <h3 class={Seq(DialogStyle.header.htmlClass, BoincClientStyle.pageHeaderSmall).mkString(" ")}>
        {"edit_host_details".localize}
      </h3>,
      dialog => {
        val address = dom.document.querySelector("#host_details_address").asInstanceOf[HTMLInputElement].value
        val port = dom.document.querySelector("#host_details_port").asInstanceOf[HTMLInputElement].value
        val password = dom.document.querySelector("#host_details_password").asInstanceOf[HTMLInputElement].value

        NProgress.start()
        ClientManager.updateClient(hostDetails.name.now, address, port.toInt, password).foreach { succ =>
          NProgress.done(true)

          if (succ) {
            hostDetails.address := address
            hostDetails.port := port.toInt
            hostDetails.password := password

            dialog.close()
          } else {
            dom.window.alert("Error: Could not update host details!")
          }
        }
      },
      dialog => {dialog.close()},
      "save".localize
    )


}
