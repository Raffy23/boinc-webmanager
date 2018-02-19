package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.shared.BoincProjectMetaData
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.LoginPage
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLInputElement, HTMLSelectElement}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.xml.{Elem, Node}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 06.09.2017
  */
class ProjectAddDialog(projectData: Map[String, BoincProjectMetaData], submitAction: (String, String, String, String) => Future[Boolean]) extends Dialog("modal-dialog") {

  //TODO: convert to mthml.Rx stuff:
  private lazy val jsOnChangeListener: (Event) => Unit = (event) => {
    val element = projectData(event.target.asInstanceOf[HTMLSelectElement].value)
    dom.document.getElementById("pad-url").textContent = element.url
    dom.document.getElementById("pad-url").setAttribute("href", element.url)
    dom.document.getElementById("pad-general_area").textContent = element.general_area
    dom.document.getElementById("pad-description").textContent = element.description
    dom.document.getElementById("pad-home").textContent = element.home
  }

  val dialog = new SimpleModalDialog(
    <div>
      <table class={TableTheme.table.htmlClass}>
        <tbody>
          <tr>
            <td style="width:125px">{"table_project".localize}</td>
            <td>
              <select class={LoginPage.Style.input.htmlClass} style="margin:0" id="pad-project" onchange={jsOnChangeListener}>
                <option disabled={true} selected="selected">{"project_new_default_select".localize}</option>
                {
                  projectData.map(project =>
                    <option value={project._1}>{project._1}</option>
                  ).toList
                }
              </select>
            </td>
          </tr>
          <tr>
            <td>{"project_new_url".localize}</td>
            <td><a id="pad-url" onclick={AppRouter.openExternal}></a></td>
          </tr>
          <tr><td>{"project_new_general_area".localize}</td><td id="pad-general_area"></td></tr>
          <tr><td>{"project_new_desc".localize}</td><td id="pad-description"></td></tr>
          <tr><td>{"project_new_home".localize}</td><td id="pad-home"></td></tr>
        </tbody>
      </table>
      <br/>
      <h4>{"project_new_userdata".localize}</h4>
      <table style="width:calc(100% - 20px)">
        <tbody>
          <tr>
            <td>{"login_username".localize}</td>
            <td>
              <input class={LoginPage.Style.input.htmlClass} placeholder="example@boinc-user.com" style="margin:0" id="pad-username"></input>
            </td>
          </tr>
          <tr>
            <td>{"login_password".localize}</td>
            <td>
              <input class={LoginPage.Style.input.htmlClass} placeholder={"login_password".localize} style="margin:0" id="pad-password" type="password"></input>
            </td>
          </tr>
        </tbody>
      </table>
      <br/>
      <br/>
    </div>,
    <h2 class={Dialog.Style.header.htmlClass}>{"project_new_addbtn".localize}</h2>,
    (dialog: SimpleModalDialog) => {
      NProgress.start()

      val select = dom.document.getElementById("pad-project").asInstanceOf[HTMLSelectElement]
      val element = projectData(select.value)
      val username = dom.document.getElementById("pad-username").asInstanceOf[HTMLInputElement].value
      val password = dom.document.getElementById("pad-password").asInstanceOf[HTMLInputElement].value

      submitAction(element.url, username, password, element.name).foreach( state => {
        if (state)
          dialog.hide()
      })
    },
    (dialog: SimpleModalDialog) => {dialog.hide()}
  )

  override def render(): Elem = dialog.render()

  def toXML: Node = dialog.render()

}
