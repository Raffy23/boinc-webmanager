package at.happywetter.boinc.web.pages

import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.xml.{Elem, Node}
import scalacss.ProdDefaults._
import at.happywetter.boinc.web.helper.XMLHelper._
import at.happywetter.boinc.web.pages.component.topnav.TopNavigation

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.07.2017
  */
object PageLayout {
  import scala.language.postfixOps

  object Style extends StyleSheet.Inline {
    import dsl._

    val heading = style(
      position.fixed,
      top.`0`,
      left.`0`,
      width(100 %%),
      height(50 px),
      paddingLeft(15 px),
      backgroundColor(c"#222"),
      color(c"#F2F2F2"),
      boxShadow := "0 3px 6px rgba(0,0,0,0.16), 0 3px 6px rgba(0,0,0,0.23)",
      zIndex :=! "2",

      media.maxWidth(690 px)(
        height(100 px)
      )
    )

    val headerText = style(
      display.inlineBlock,
      fontWeight._300,
      marginTop(10 px),
      marginBottom(10 px),
      fontSize(22 px)
    )

    val versionField = style(
      marginLeft(20 px)
    )

    val footer = style(
      position.fixed,
      bottom.`0`,
      left.`0`,
      width(100 %%),
      height(35 px),
      backgroundColor(c"#222"),
      color(c"#F2F2F2"),
      textAlign.center,
      fontWeight.lighter,
      fontSize.smaller
    )

    /*
    val navigation = style(
      position.fixed,
      right(15 px),
      top(30 px),
      //display.flex

      media.maxWidth(690 px)(
        top(60 px)
      )
    )
    */

    val clientContainer = style(
      marginLeft(218 px),

      media.maxWidth(690 px)(
        marginLeft(5 px)
      )
    )
  }

  private var curNav: TopNavigation = _
  val nav: Var[Node] = Var("")

  val hamburgerMenuAction: (Event) => Unit = (event) => {
    val menu = dom.document.getElementById("dashboard-menu").asInstanceOf[HTMLElement]

    if (menu.style.display == "none") showMenu()
    else hideMenu()
  }

  val heading: Elem = {
    <header class={Style.heading.htmlClass}>
      <h1 class={Style.headerText.htmlClass}>
        <i class="fa fa-bars" stlye="margin-right:13px;cursor:pointer" onclick={hamburgerMenuAction}></i>
        Boinc Webmanager
      </h1>
      <div id="navigation">{nav}</div>
    </header>
  }

  def showMenu(): Unit = {
    val menu = dom.document.getElementById("dashboard-menu").asInstanceOf[HTMLElement]
    val content = dom.document.getElementById("client-container").asInstanceOf[HTMLElement]

    menu.style.display = "block"

    if (dom.window.outerWidth > 690)
      content.style.marginLeft = "218px"
  }

  def hideMenu(): Unit = {
    val menu = dom.document.getElementById("dashboard-menu").asInstanceOf[HTMLElement]
    val content = dom.document.getElementById("client-container").asInstanceOf[HTMLElement]

    menu.style.display = "none"

    if (dom.window.outerWidth > 690) content.style.marginLeft = "20px"
    else content.style.marginLeft = "5px"
  }

  def clearNav(): Unit = nav := ""

}
