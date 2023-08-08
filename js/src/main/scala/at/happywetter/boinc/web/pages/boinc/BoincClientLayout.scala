package at.happywetter.boinc.web.pages.boinc

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.util.Try

import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager}
import at.happywetter.boinc.web.facade.Implicits._
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.pages.component.topnav.BoincTopNavigation
import at.happywetter.boinc.web.pages.{Dashboard, Layout}
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.util.{AuthClient, DashboardMenuBuilder, ErrorDialogUtil}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.07.2017
  */
object BoincClientLayout:

  def link(client: String, path: String): String = s"/view/boinc-client/${dom.window.encodeURIComponent(client)}/$path"

  def link(client: String): String = s"/view/boinc-client/${dom.window.encodeURIComponent(client)}"

abstract class BoincClientLayout extends Layout:

  implicit var boincClientName: String = _

  def linkForHost(host: String) =
    s"/view/boinc-client/${dom.window.encodeURIComponent(host)}/$path"

  override def link: String =
    if (boincClientName != null)
      linkForHost(boincClientName)
    else
      s"/view/boinc-client/:client/$path"

  implicit protected var boinc: BoincClient = _

  override def beforeRender(params: Dictionary[String]): Unit =
    if (params == null || js.undefined == params.asInstanceOf[js.UndefOr[Dictionary[String]]])
      dom.console.error("Unable to instantiate Boinc Client Layout without params!")
      if (boinc == null)
        dom.console.error("No Fallback Client from prev. view, falling back to Dashboard!")
        AppRouter.navigate(Dashboard)
    else
      parse(params)
      BoincRootLayout.currentController = this

    BoincTopNavigation.clientName := boincClientName
    BoincTopNavigation.render(Some(path))

  override def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit =
    DashboardMenuBuilder.afterRenderHooks += (() => DashboardMenu.selectMenuItemByContent(boincClientName))
    super.before(() => { parse(params); done() }, params)

  private def parse(params: js.Dictionary[String]): Unit =
    if (params == null)
      return

    boincClientName = params.get("client").get

    Try(
      this.boinc = ClientManager.clients(boincClientName)
    ).recover:
      case e: Exception =>
        ErrorDialogUtil.showDialog(e)
        AppRouter.navigate(Dashboard)
