package at.happywetter.boinc.web.pages

import at.happywetter.boinc.BuildInfo
import at.happywetter.boinc.web.helper.XMLHelper._
import at.happywetter.boinc.web.pages.LoginPage.Style
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.{DashboardMenu, LanguageChooser}
import at.happywetter.boinc.web.routes.AppRouter.DashboardLocation
import at.happywetter.boinc.web.routes.{AppRouter, LayoutManager, NProgress}
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.LanguageDataProvider
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem
import scalacss.ProdDefaults._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 23.07.2017
  */
object LoginPage {

  object Style extends StyleSheet.Inline {
    import dsl._

    val content: StyleA = style(
      position.relative,
      zIndex(1),
      backgroundColor(c"#FFFFFF"),
      maxWidth(360 px),
      margin(250 px, auto),
      padding(10 px, 45 px, 45 px, 45 px),
      textAlign.center,
      boxShadow := " 0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22)",
    )

    val input = style(
      outline.`0`,
      backgroundColor(c"#F2F2F2"),
      width(100%%),
      border.`0`,
      margin(0 px, 0 px, 15 px, 0 px),
      padding(15 px),
      boxSizing.borderBox,
      fontSize(14 px)
    )

    val headerBar = style(
      position.fixed,
      top.`0`,
      left.`0`,
      width(100 %%),
      height(75 px),
      backgroundColor(c"#424242"),
      color(c"#F2F2F2"),
      boxShadow := " 0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22)",
      textAlign.center
    )

    val button = style(
      textTransform.uppercase,
      outline.`0`,
      backgroundColor(c"#428bca"),
      width(100 %%),
      border.`0`,
      padding(15 px),
      color(c"#FFFFFF"),
      cursor.pointer,

      &.hover(
        backgroundColor(c"#74a9d8")
      )
    )
  }
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

        AppRouter.navigate(event, DashboardLocation)

        PageLayout.showMenu()
        DashboardMenu.processSeverConfig()

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

  override def before(done: js.Function0[Unit]): Unit = {
    val usr = dom.window.sessionStorage.getItem("username")
    val pwd = dom.window.sessionStorage.getItem("password")

    DashboardMenu.hide()
    PageLayout.hideMenu()
    done()

    if (usr != null && pwd != null) {
      loginValidator(usr, pwd).foreach {
        case true => AppRouter.navigate(DashboardLocation)
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
