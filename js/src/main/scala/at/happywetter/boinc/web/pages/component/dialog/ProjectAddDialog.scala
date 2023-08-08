package at.happywetter.boinc.web.pages.component.dialog

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.HTMLInputElement
import org.scalajs.dom.HTMLSelectElement
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Random
import scala.xml.Elem
import scala.xml.Node

import at.happywetter.boinc.shared.boincrpc.BoincProjectMetaData
import at.happywetter.boinc.shared.util.StringLengthAlphaOrdering
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.css.definitions.components.{Dialog => DialogStyle}
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.css.definitions.pages.LoginPageStyle
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.RichRx._

import mhtml.Var

/**
  * Created by: 
  *
  * @author Raphael
  * @version 06.09.2017
  */
class ProjectAddDialog(projectData: Map[String, BoincProjectMetaData],
                       submitAction: (String, String, String, String) => Future[Boolean]
) extends Dialog("modal-dialog"):

  private val selected: Var[Option[BoincProjectMetaData]] = Var(None)
  private val projectDialogID = Random.alphanumeric.take(5).mkString

  // TODO: convert to mthml.Rx stuff:
  private lazy val jsOnChangeListener: (Event) => Unit = event => {
    selected := Some(projectData(event.target.asInstanceOf[HTMLSelectElement].value))
  }

  val dialog = new SimpleModalDialog(
    <div>
      <table class={TableTheme.table.htmlClass}>
        <tbody>
          <tr>
            <td style="width:125px">{"table_project".localize}</td>
            <td>
              <select class={LoginPageStyle.input.htmlClass} style="margin:0" id="pad-project" onchange={
      jsOnChangeListener
    }>
                <option disabled={true} selected="selected">{"project_new_default_select".localize}</option>
                {
      projectData.keys.toSeq.sorted.map(projectName => <option value={projectName}>{projectName}</option>).toList
    }
              </select>
            </td>
          </tr>
          <tr>
            <td>{"project_new_url".localize}</td>
            <td>
              <a onclick={AppRouter.openExternal} href={selected.map(_.map(_.url))}>
                {selected.map(_.map(_.url))}
              </a>
            </td>
          </tr>
          <tr><td>{"project_new_general_area".localize}</td><td>{selected.map(_.map(_.general_area))}</td></tr>
          <tr><td>{"project_new_desc".localize}</td><td>{selected.map(_.map(_.description))}</td></tr>
          <tr><td>{"project_new_home".localize}</td><td>{selected.map(_.map(_.home))}</td></tr>
        </tbody>
      </table>
      <br/>
      <h4>{"project_new_userdata".localize}</h4>
      <table style="width:calc(100% - 20px)">
        <tbody>
          <tr>
            <td>{"login_username".localize}</td>
            <td>
              <input class={LoginPageStyle.input.htmlClass}
                     placeholder="example@boinc-user.com"
                     style="margin:0"
                     id={s"$projectDialogID-username"}>
              </input>
            </td>
          </tr>
          <tr>
            <td>{"login_password".localize}</td>
            <td>
              <input class={LoginPageStyle.input.htmlClass}
                     placeholder={"login_password".localize}
                     style="margin:0"
                     id={s"$projectDialogID-password"}
                     type="password">
              </input>
            </td>
          </tr>
        </tbody>
      </table>
      <br/>
      <br/>
    </div>,
    <h2 class={Seq(DialogStyle.header.htmlClass, BoincClientStyle.pageHeaderSmall).mkString(" ")}>
      {"project_new_addbtn".localize}
    </h2>,
    (dialog: SimpleModalDialog) => {

      selected.now.foreach { element =>
        NProgress.start()

        val username = dom.document.getElementById(s"$projectDialogID-username").asInstanceOf[HTMLInputElement].value
        val password = dom.document.getElementById(s"$projectDialogID-password").asInstanceOf[HTMLInputElement].value

        submitAction(element.url, username, password, element.name).foreach(state => {
          if (state)
            dialog.hide()
        })

      }

    },
    (dialog: SimpleModalDialog) => { dialog.hide() }
  )

  def focusUsernameFiled(): Unit =
    dom.document.getElementById(s"$projectDialogID-username").asInstanceOf[HTMLInputElement].focus()

  override def render(): Elem = dialog.render()

  def toXML: Node = dialog.render()
