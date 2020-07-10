package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.css.definitions.components.{Dialog => DialogStyle}
import at.happywetter.boinc.web.css.definitions.pages.LoginPageStyle
import at.happywetter.boinc.web.model.HostDetailsTableRow
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom

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
              <td><input value={hostDetails.address}></input></td>
            </tr>
            <tr>
              <td><b>{"port".localize}</b></td>
              <td><input type="number" value={hostDetails.port.map(_.toString)}></input></td>
            </tr>
            <tr>
              <td><b>{"password".localize}</b></td>
              <td><input type="password" value={hostDetails.password}></input></td>
            </tr>
          </tbody>
        </table>
      </div>,
      <h3 class={Seq(DialogStyle.header.htmlClass, BoincClientStyle.pageHeaderSmall).mkString(" ")}>
        {"edit_host_details".localize}
      </h3>,
      dialog => {
        dom.window.alert("Action not implemented")
      },
      dialog => {dialog.close()},
      "save".localize
    )


}
