package at.happywetter.boinc.web.pages

import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager}
import at.happywetter.boinc.web.css.TopNavigation
import at.happywetter.boinc.web.pages.boinc.BoincComponent
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.routes.AppRouter.DashboardLocation
import at.happywetter.boinc.web.routes.{AppRouter, Hook, Navigo}
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.07.2017
  */

abstract class BoincClientLayout(clientName: String) extends Layout  with BoincComponent {

  protected implicit val boincClientName: String = clientName
  protected lazy val boinc: BoincClient = ClientManager.clients(clientName)

  override val component: JsDom.TypedTag[HTMLElement] = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._

    div(BoincClientLayout.Style.content, id := "client-data")
  }

  override val routerHook: Option[Hook] = None

  override val requestedParent = Some("main #client-container")
  override def requestParentLayout() = { Some(Dashboard) }

  def root: Element = dom.document.getElementById("client-data")

  override def onRender(): Unit = {
    DashboardMenu.selectMenuItemByContent(clientName)

    //Render Top-Navbar for Boinc
    val nav = dom.document.getElementById("navigation")
    if (nav.childNodes.length > 0) nav.removeChild(nav.firstChild)
    nav.appendChild({
      import scalatags.JsDom.all._
      import scalacss.ScalatagsCss._

      ul(TopNavigation.nav,
        li(a(href := s"${AppRouter.href(DashboardLocation)}/$clientName/boinc", "Boinc", data("navigo") := "")),
        li(a(href := s"${AppRouter.href(DashboardLocation)}/$clientName/messages", "Nachrichten", data("navigo") := "")),
        li(a(href := s"${AppRouter.href(DashboardLocation)}/$clientName/projects", "Projekte", data("navigo") := "")),
        li(a(href := s"${AppRouter.href(DashboardLocation)}/$clientName/tasks", "Aufgaben", data("navigo") := "" )),
        li(a(href := s"${AppRouter.href(DashboardLocation)}/$clientName/transfers", "Übertragungen", data("navigo") := "")),
        li(a(href := s"${AppRouter.href(DashboardLocation)}/$clientName/statistics", "Statistiken", data("navigo") := "")),
        li(a(href := s"${AppRouter.href(DashboardLocation)}/$clientName/disk", "Festplatte", data("navigo") := ""))
      ).render
    })

    AppRouter.router.updatePageLinks()
    onRender(boinc)
  }

  override def beforeRender(params: Dictionary[String]): Unit = {}
}

object BoincClientLayout {
  import scalacss.internal.mutable.StyleSheet
  import scalacss.DevDefaults._
  import scala.language.postfixOps

  object Style extends StyleSheet.Inline {
    import dsl._

    val pageHeader = style(
      paddingBottom(9 px),
      margin(40 px, 20 px, 20 px, auto),
      borderBottom :=! "1px solid #DDD",
      fontSize(28 px),
      fontWeight._300
    )

    val content = style(
      paddingLeft(8 px)
    )

    val progressBar = style(

      unsafeChild("progress")(
        backgroundColor(c"#EEE"),
        border.`0`,
        height(18 px),
        width :=! "calc(100% - 3em)"
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

}
