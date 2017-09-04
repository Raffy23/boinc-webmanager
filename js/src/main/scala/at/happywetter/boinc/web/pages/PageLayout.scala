package at.happywetter.boinc.web.pages

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.language.postfixOps
import scala.scalajs.js
import scalacss.ProdDefaults._
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.07.2017
  */
object PageLayout {

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
      zIndex :=! "2"
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

    val navigation = style(
      position.fixed,
      right(15 px),
      top(30 px),
      display.flex
    )

  }

  val hamburgerMenuAction: js.Function1[Event,Unit] = (event) => {
    val menu = dom.document.getElementById("dashboard-menu").asInstanceOf[HTMLElement]
    val content = dom.document.getElementById("client-container").asInstanceOf[HTMLElement]

    if (menu.style.display == "none") {
      menu.style.display = "block"
      content.style.marginLeft = "218px"
    } else {
      menu.style.display = "none"
      content.style.marginLeft = "20px"
    }

  }

  val heading: JsDom.TypedTag[HTMLElement] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    JsDom.all.header(Style.heading,
      h1(Style.headerText,
        i(`class` := "fa fa-bars", style := "margin-right:13px", onclick := hamburgerMenuAction)
        , "Boinc Webmanager"),
      span(Style.versionField,small(small("(v0.1-dev)"))),
      div(Style.navigation, id:="navigation")
    )
  }

  val footer: JsDom.TypedTag[HTMLElement] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    JsDom.all.footer(Style.footer, p("DEVELOPMENT VERSION!"))
  }

}
