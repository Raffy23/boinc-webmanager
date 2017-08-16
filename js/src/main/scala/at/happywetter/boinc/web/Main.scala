package at.happywetter.boinc.web

import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.css.AppCSS
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
    println("Booting Application ...")

    NProgress.start()
    AppCSS.load()
    initRouter()

    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    ClientManager.bootstrapClients().foreach(_ => LayoutManager.init())

    AppRouter.router.navigate(dom.window.location.pathname, absolute = true)
    NProgress.done(true)
  }

  def initRouter(): Unit = {
    AppRouter.addRoute(
      LoginPageLocation,
      "/view/login",
      new LoginPage((username, password) => {
        dom.window.sessionStorage.setItem("username", username)
        dom.window.sessionStorage.setItem("password", password)

        //TODO: Auth with Server!
        if(username == "admin" && password == "password") true
        else false
      })
    )

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
