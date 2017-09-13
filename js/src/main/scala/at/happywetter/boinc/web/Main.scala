package at.happywetter.boinc.web

import at.happywetter.boinc.BuildInfo
import at.happywetter.boinc.web.css.AppCSS
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.pages._
import at.happywetter.boinc.web.routes.AppRouter._
import at.happywetter.boinc.web.routes.{AppRouter, LayoutManager, NProgress}
import at.happywetter.boinc.web.util.I18N.Locale
import at.happywetter.boinc.web.util.LanguageDataProvider
import org.scalajs.dom

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import at.happywetter.boinc.web.util.I18N._

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
    dom.console.log("Current Version: " + BuildInfo.version)

    NProgress.start()
    AppCSS.load()
    AuthClient.loadFromLocalStorage()
    dom.console.log("Early load Locale from SessionStorage: " + Locale.load)

    // Load Languages before jumping to UI
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    LanguageDataProvider.bootstrap.foreach(_ => {
      initRouter()
      LayoutManager.init()

      dom.console.log("Finished, navigating to Path")
      dom.console.log("Setting current language to: " + Locale.current)
      dom.console.log("Language Name: " + "language_name".localize)

      AppRouter.router.navigate(dom.window.location.pathname, absolute = true)
      NProgress.done(true)
    })
  }

  def initRouter(): Unit = {
    AppRouter.addRoute(LoginPageLocation, "/view/login", new LoginPage(AuthClient.validate))
    AppRouter.addRoute(DashboardLocation, "/view/dashboard", Dashboard)
    AppRouter.addRoute(SettingsLocation, "/view/settings", SettingsPage)
    AppRouter.addRoute(BoincHomeLocation, "/view/dashboard/:client", BoincLayout)
    AppRouter.addRoute(BoincHomeLocation, "/view/dashboard/:client/:action", BoincLayout)
    AppRouter.addRoute(SwarmControlLocation, "/view/swarm", SwarmControlPage)
    AppRouter.addRoute(SwarmControlLocation, "/view/swarm/:action", SwarmControlPage)

    AppRouter.router.on(() => AppRouter.navigate(DashboardLocation))
    AppRouter.router.notFound((param) => {
      dom.console.log(param)
      dom.window.alert("page_not_found".localize)
      AppRouter.navigate(DashboardLocation)
    })

    AppRouter.router.updatePageLinks()
  }

}
