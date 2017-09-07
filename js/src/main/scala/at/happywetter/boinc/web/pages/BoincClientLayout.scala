package at.happywetter.boinc.web.pages

import at.happywetter.boinc.web.boincclient.{BoincClient, ClientManager}
import at.happywetter.boinc.web.css.TopNavigation
import at.happywetter.boinc.web.pages.boinc.BoincComponent
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.routes.AppRouter.DashboardLocation
import at.happywetter.boinc.web.routes.{AppRouter, Hook}
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js.Dictionary
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.07.2017
  */

abstract class BoincClientLayout(clientName: String) extends Layout  with BoincComponent {

  implicit val boincClientName: String = clientName
  protected lazy val boinc: BoincClient = ClientManager.clients(clientName)

  override val staticComponent: Option[JsDom.TypedTag[HTMLElement]] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    Some(div(BoincClientLayout.Style.content, id := "client-data"))
  }

  override val routerHook: Option[Hook] = None

  override val requestedParent = Some("main #client-container")
  override def requestParentLayout() = { Some(Dashboard) }

  def root: Element = dom.document.getElementById("client-data")

  private val links = List(
    ("boinc", "head_menu_boinc".localize),
    ("messages", "head_menu_messages".localize),
    ("projects", "head_menu_projects".localize),
    ("tasks", "head_menu_tasks".localize),
    ("transfers", "head_menu_transfers".localize),
    ("statistics", "head_menu_statistics".localize),
    ("disk", "head_menu_disk".localize),
    ("global_prefs", "head_menu_prefs".localize)
  )

  override def onRender(): Unit = {
    DashboardMenu.selectMenuItemByContent(clientName)

    //Render Top-Navbar for Boinc
    val nav = dom.document.getElementById("navigation")
    if (nav.childNodes.length > 0) nav.removeChild(nav.firstChild)
    nav.appendChild({
      import scalacss.ScalatagsCss._
      import scalatags.JsDom.all._

      ul(TopNavigation.nav, id := "boinc_top_navbar",
        links.map { case (nav, name) =>
          li(
            a(href := s"${DashboardLocation.link}/$clientName/$nav", name, data("navigo") := "",
              `class` := (if (path == nav) TopNavigation.active.htmlClass else "")
            )
          )
        }
      ).render
    })

    AppRouter.router.updatePageLinks()
    onRender(boinc)
  }

  override def beforeRender(params: Dictionary[String]): Unit = {}
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
      fontWeight._300
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
