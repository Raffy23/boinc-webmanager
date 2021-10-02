package at.happywetter.boinc.web

import at.happywetter.boinc.BuildInfo
import at.happywetter.boinc.shared.boincrpc.ServerSharedConfig
import at.happywetter.boinc.web.boincclient.{ClientCacheHelper, ClientManager}
import at.happywetter.boinc.web.css.AppCSSRegistry
import at.happywetter.boinc.web.pages._
import at.happywetter.boinc.web.pages.boinc._
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.pages.settings.HostSettings
import at.happywetter.boinc.web.pages.swarm.{BoincSwarmPage, ProjectSwarmPage}
import at.happywetter.boinc.web.routes.{AppRouter, LayoutManager, NProgress}
import at.happywetter.boinc.web.util.I18N.{Locale, _}
import at.happywetter.boinc.web.util.{AuthClient, DashboardMenuBuilder, LanguageDataProvider, ServerConfig}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.07.2017
  */
@JSExportTopLevel("Main")
object Main {

  @JSExport
  @Deprecated
  def launch(): Unit = main(Array.empty)

  @JSExport
  def launch(config: js.Dynamic): Unit = {
    ServerConfig.config := ServerSharedConfig(
      config.selectDynamic("hostNameCacheTimeout").asInstanceOf[Int],
      config.selectDynamic("hardware").asInstanceOf[Boolean]
    )

    main(Array.empty)
  }

  @JSExport
  def main(args: Array[String]): Unit = {
    dom.console.log("Booting Application ...")
    dom.console.log("Current Version: " + BuildInfo.version)

    NProgress.start()
    val registeredStyles = AppCSSRegistry.registerCSSNames()
    dom.console.log(s"Registered $registeredStyles css classes")

    if (!AuthClient.isSecureEndpoint)
      dom.console.warn("Server endpoint is not secure, crypto API will be disabled ...")

    val haveToken = AuthClient.loadFromLocalStorage()
    dom.console.log("Is old token available: " + haveToken)
    dom.console.log("Early load Locale from SessionStorage: " + Locale.load)

    if (haveToken)
      ServerConfig.query

    ClientCacheHelper.init()
    ClientManager.cacheInvalidationCallback += (_ => {
      DashboardMenu.clearSubmenus()
      DashboardMenuBuilder.invalidateCache()
    })

    // Load Languages before jumping to UI
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    LanguageDataProvider.bootstrap.foreach(_ => {
      dom.console.log("Setting current language to: " + Locale.current)
      dom.console.log("Language Name: " + "language_name".localize)

      Try{
        LayoutManager.init()
        initRouter()
        navigate()
      }.recover {
        case ex: Exception => ex.printStackTrace()
      }
    })
  }

  def navigate(): Unit = {
    AppRouter.router.resolve()
    NProgress.done(true)
  }

  def initRouter(): Unit = {
    // Login Page:
    AppRouter += new LoginPage(AuthClient.validate)

    // Static pages for all views:
    AppRouter += Dashboard
    AppRouter += SettingsPage
    AppRouter += WebRPCProjectPage
    AppRouter += HardwarePage
    AppRouter += JobManagerPage

    // Settings pages:
    AppRouter += HostSettings

    // Swarm pages:
    AppRouter += new BoincSwarmPage
    AppRouter += new ProjectSwarmPage

    // All Boinc pages:
    (AppRouter += BoincRootLayout).currentController = AppRouter += new BoincMainHostLayout
    AppRouter += new BoincTaskLayout
    AppRouter += new BoincProjectLayout
    AppRouter += new BoincFileTransferLayout
    AppRouter += new BoincDiskLayout
    AppRouter += new BoincMessageLayout
    AppRouter += new BoincGlobalPrefsLayout
    AppRouter += new BoincStatisticsLayout

    AppRouter.router.on(() => AppRouter.navigate(Dashboard))
    AppRouter.router.notFound((_) => {
      dom.window.alert("page_not_found".localize)
      dom.console.error(s"Error: The page ('${AppRouter.current}') was not found!")

      AppRouter.navigate(Dashboard)
    })

    AppRouter.router.updatePageLinks()
  }

}
