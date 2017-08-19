package at.happywetter.boinc.web

import at.happywetter.boinc.web.css.AppCSS
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.pages.{BoincLayout, Dashboard, LoginPage}
import at.happywetter.boinc.web.routes.AppRouter.{BoincHomeLocation, DashboardLocation, LoginPageLocation}
import at.happywetter.boinc.web.routes.{AppRouter, LayoutManager, NProgress}
import org.scalajs.dom

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.07.2017
  */
@JSExportTopLevel("Main")
object Main {

  @JSExport
  def launch(): Unit = main()

  @JSExport
  def main(): Unit = {
    dom.console.log("Booting Application ...")

    NProgress.start()
    AppCSS.load()
    initRouter()
    LayoutManager.init()

    dom.console.log("Finished, navigating to Path")
    AppRouter.router.navigate(dom.window.location.pathname, absolute = true)
    NProgress.done(true)
  }

  def initRouter(): Unit = {
    AppRouter.addRoute(LoginPageLocation, "/view/login", new LoginPage(AuthClient.validate))
    AppRouter.addRoute(DashboardLocation, "/view/dashboard", Dashboard)
    AppRouter.addRoute(BoincHomeLocation, "/view/dashboard/:client", BoincLayout)
    AppRouter.addRoute(BoincHomeLocation, "/view/dashboard/:client/:action", BoincLayout)

    AppRouter.router.on(() => AppRouter.navigate(DashboardLocation))
    AppRouter.router.notFound((param) => {
      dom.console.log(param)
      dom.window.alert("Page was not found!")
      AppRouter.navigate(DashboardLocation)
    })

    AppRouter.router.updatePageLinks()
  }

}
