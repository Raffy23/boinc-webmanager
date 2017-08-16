package at.happywetter.boinc.web.pages

import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.routes.AppRouter.{DashboardLocation, LoginPageLocation}
import at.happywetter.boinc.web.routes.{AppRouter, Hook, NProgress}
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.{Dictionary, UndefOr}
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 24.07.2017
  */
object Dashboard extends Layout {

  import scalacss.DevDefaults._
  object Style extends StyleSheet.Inline {
  }

  lazy val component: JsDom.TypedTag[HTMLElement] = {
    import scalatags.JsDom.all._

    div(
      DashboardMenu.component.render,
      div(id := "client-container", style := "margin-left:218px"
      )
    )
  }

  override val routerHook: Option[Hook] = Some(new Hook {
    override def already(): Unit = {
      onRender()
    }

    override def before(done: js.Function0[Unit]): Unit = {
      val usr = dom.window.sessionStorage.getItem("username")
      val pwd = dom.window.sessionStorage.getItem("password")

      if (usr == null || pwd == null) {
        dom.console.error("Username or password was not defined!")
        AppRouter.navigate(LoginPageLocation)
      } else {
        done()
      }
    }

    override def leave(): Unit = {}

    override def after(): Unit = {}
  })

  override def onRender(): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    ClientManager.readClients().foreach(clients => {

      DashboardMenu.removeMenuReferences("boinc-client-entry")
      clients.foreach(client =>
        DashboardMenu.addMenu(s"${AppRouter.href(DashboardLocation)}/$client",client, Some("boinc-client-entry"))
      )

      AppRouter.router.updatePageLinks()
      NProgress.done(true)
    })
  }

  override def beforeRender(params: Dictionary[String]): Unit = {}
}
