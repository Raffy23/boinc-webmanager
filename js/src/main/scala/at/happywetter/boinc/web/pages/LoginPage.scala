package at.happywetter.boinc.web.pages

import at.happywetter.boinc.web.util.XMLHelper._
import at.happywetter.boinc.web.css.definitions.pages.{LoginPageStyle => Style}
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.{DashboardMenu, LanguageChooser}
import at.happywetter.boinc.web.routes.{AppRouter, LayoutManager, NProgress}
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.{LanguageDataProvider, ServerConfig}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem
import at.happywetter.boinc.BuildInfo

/**
  * Created by: 
  *
  * @author Raphael
  * @version 23.07.2017
  */
object LoginPage {

  def link: String = "/view/login"

}

class LoginPage(loginValidator: (String,String) => Future[Boolean]) extends Layout {
  override val path = "login"

  override def render: Elem = {
    <div>
      <div id="language-selector-area" style="position:fixed;top:74px;right:22px">
      {
        new LanguageChooser((event, lang_code) => {
          event.preventDefault()

          NProgress.start()
          LanguageDataProvider
            .loadLanguage(lang_code)
            .foreach(_ => {
              Locale.save(lang_code)

              LayoutManager.render(this)
              NProgress.done(true)
            })
        }, -35).component
      }
      </div>

      <div>
        <form class={Style.content.htmlClass} id="login-form">
          <h2 style="margin-bottom:25px">{"login_title".localize}</h2>
          <input class={Style.input.htmlClass} type="text"
                 placeholder={"login_username".localize} id="login-username"></input>
          <input class={Style.input.htmlClass} type="password"
                 placeholder={"login_password".localize} id="login-password"></input>
          <button class={Style.button.htmlClass} onclick={loginAction}>
            {"login_btn".localize}
          </button>
        </form>
      </div>

      <span style="position:fixed;bottom:0">
        <small><b>Version: </b>{BuildInfo.version}</small>
      </span>
    </div>
  }

  private val loginAction: (Event) => Unit = (event) => {

    NProgress.start()
    val username = dom.document.getElementById("login-username").asInstanceOf[HTMLInputElement].value
    val password = dom.document.getElementById("login-password").asInstanceOf[HTMLInputElement].value

    loginValidator(username, password).foreach {
      case true =>
        dom.window.sessionStorage.setItem("username", username)
        dom.window.sessionStorage.setItem("password", password)

        AppRouter.navigate(event, Dashboard)

        PageLayout.showMenu()
        ServerConfig.query

      case _ =>
        new OkDialog(
          "dialog_error_header".localize,
          List("login_wrong_password_msg".localize),
          (_) => {dom.document.getElementById("login-username").asInstanceOf[HTMLElement].focus()}
        ).renderToBody().show()

        NProgress.done(true)
    }

    event.preventDefault()
  }

  override def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit = {
    val usr = dom.window.sessionStorage.getItem("username")
    val pwd = dom.window.sessionStorage.getItem("password")

    DashboardMenu.hide()
    PageLayout.hideMenu()
    done()

    if (usr != null && pwd != null) {
      loginValidator(usr, pwd).foreach {
        case true => AppRouter.navigate(Dashboard.link)
        case _ =>
          dom.window.sessionStorage.removeItem("username")
          dom.window.sessionStorage.removeItem("password")
          NProgress.done(true)
      }
    }
  }

  override def already(): Unit = {
    dom.document.getElementById("login-username").asInstanceOf[HTMLInputElement].value = ""
    dom.document.getElementById("login-password").asInstanceOf[HTMLInputElement].value = ""
  }

  override def beforeRender(params: Dictionary[String]): Unit = {}
}
