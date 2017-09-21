package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.shared.BoincProjectMetaData
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.LoginPage
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement, HTMLSelectElement}
import at.happywetter.boinc.web.util.I18N._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 06.09.2017
  */
class ProjectAddDialog(projectData: Map[String, BoincProjectMetaData], submitAction: (String, String, String, String) => Future[Boolean]) extends Dialog("modal-dialog") {

  val dialogContent = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._

    div(
      table(TableTheme.table,
        tbody(
          tr(td("table_project".localize, style := "width:125px"),
            td(
              select(LoginPage.Style.input, style := "margin:0", id := "pad-project",
                option(disabled, selected := "selected", "project_new_default_select".localize),
                projectData.map(project => option(value := project._1, project._1)).toList,
                onchange := { (event: Event) => {
                  val element = projectData(event.target.asInstanceOf[HTMLSelectElement].value)
                  dom.document.getElementById("pad-url").textContent = element.url
                  dom.document.getElementById("pad-url").setAttribute("href", element.url)
                  dom.document.getElementById("pad-general_area").textContent = element.general_area
                  dom.document.getElementById("pad-description").textContent = element.description
                  dom.document.getElementById("pad-home").textContent = element.home
                }
                }
              )
            )
          ),
          tr(td("project_new_url".localize), td(a(id := "pad-url", onclick := AppRouter.openExternal))
          ),
          tr(td("project_new_general_area".localize), td(id := "pad-general_area")),
          tr(td("project_new_desc".localize), td(id := "pad-description")),
          tr(td("project_new_home".localize), td(id := "pad-home"))
        )
      ),
      br(),
      h4("project_new_userdata".localize),
      table(style := "width: calc(100% - 20px)",
        tbody(
          tr(td("login_username".localize), td(input(LoginPage.Style.input, placeholder := "example@boinc-user.com", style := "margin:0", id := "pad-username"))),
          tr(td("login_password".localize), td(input(LoginPage.Style.input, placeholder := "login_password".localize, `type` := "password", style := "margin:0", id := "pad-password"))),
        )
      ), br(), br()
    )
  }

  val dialog = new SimpleModalDialog(
    dialogContent, {
      import scalatags.JsDom.all._
      import scalacss.ScalatagsCss._
      h2("project_new_addbtn".localize, Dialog.Style.header)
    },
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

  override def render(): HTMLElement = dialog.render()

}
