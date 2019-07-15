package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager}
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.pages.component.topnav.BoincTopNavigation
import at.happywetter.boinc.web.pages.{Dashboard, Layout, LoginPage, PageLayout}
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.util.{DashboardMenuBuilder, ErrorDialogUtil}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.07.2017
  */
abstract class BoincClientLayout extends Layout {

  implicit var boincClientName: String = _

  override def link: String = {
    val name =
      if(boincClientName != null) boincClientName
      else ":client"

    s"/view/boinc-client/$name/$path"
  }

  protected implicit var boinc: BoincClient = _

  override def beforeRender(params: Dictionary[String]): Unit = {
    println(s"BoincClientLayout.beforeRender(${params.toList})")

    if (params == null || js.undefined == params.asInstanceOf[js.UndefOr[Dictionary[String]]]) {
      dom.console.error("Unable to instantiate Boinc Client Layout without params!")
      if (boinc == null) {
        dom.console.error("No Fallback Client from prev. view, falling back to Dashboard!")
        AppRouter.navigate(Dashboard)
      }

    } else {
      parse(params)
      BoincRootLayout.currentController = this
    }

    //PageLayout.showMenu()

    BoincTopNavigation.clientName := boincClientName
    BoincTopNavigation.render(path)

    DashboardMenuBuilder.renderClients()
    DashboardMenu.selectMenuItemByContent(boincClientName)
  }

  override def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit = {
    AuthClient.validateAction(done)
    parse(params)
  }

  private def parse(params: js.Dictionary[String]): Unit = {
    if(params == null)
      return

    boincClientName = params.get("client").get

    Try(
      boinc = ClientManager.clients(boincClientName)
    ).recover{
      case e: Exception =>
        ErrorDialogUtil.showDialog(e)
        AppRouter.navigate(Dashboard)
    }
  }

}

object BoincClientLayout {
  import scala.language.postfixOps
  import scalacss.ProdDefaults._
  import scalacss.internal.mutable.StyleSheet

  object Style extends StyleSheet.Inline {
    import dsl._

    val pageHeader = style(
      paddingBottom(9 px),
      margin(40 px, 20 px, 20 px, auto),
      borderBottom :=! "1px solid #DDD",
      fontSize(28 px),
      fontWeight._300,

      unsafeChild("i")(
        marginRight(10 px)
      )
    )

    val pageHeader_small = style(
      paddingBottom(9 px),
      margin(40 px, 20 px, 20 px, auto),
      borderBottom :=! "1px solid #DDD",
      fontSize(25 px),
      fontWeight._300
    )

    val h4 = style(
      paddingBottom(9 px),
      margin(10 px, 20 px, 20 px, auto),
      borderBottom :=! "1px solid #DDD",
      fontSize(19 px),
      fontWeight._300
    )

    val h4_without_line = style(
      paddingBottom(9 px),
      margin(10 px, 20 px, 5 px, auto),
      fontSize(19 px),
      fontWeight._300
    )

    val content = style(
      paddingLeft(8 px)
    )

    val in_text_icon = style(
      unsafeChild("i")(
        marginRight(10 px)
      )
    )

    val progressBar = style(

      unsafeChild("progress")(
        backgroundColor(c"#EEE"),
        border.`0`,
        height(18 px),
        width :=! "calc(100% - 3em)"
      ),

      unsafeChild("span")(
        float.right
      ),

      unsafeChild("progress::-webkit-progress-bar")(
        backgroundColor.transparent,
        borderRadius(1 px),
        boxShadow := "0 2px 2px rgba(0, 0, 0, 0.25) inset"
      ),

      unsafeChild("progress::-moz-progress-bar")(
        backgroundColor(c"#428bca")
      ),

      unsafeChild("progress::-webkit-progress-value")(
        backgroundColor(c"#428bca")
      ),

    )
  }

  def link(client: String, path: String): String = s"/view/boinc-client/$client/$path"

  def link(client: String): String = s"/view/boinc-client/$client"

}
