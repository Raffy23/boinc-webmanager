package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.pages.component.DropdownMenu.Style
import org.scalajs.dom.raw.HTMLElement

import scala.language.postfixOps
import scalacss.ProdDefaults._
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
object DropdownMenu {

  object Style extends StyleSheet.Inline {
    import dsl._

    val dropdown = style(
      position.relative,
      display.inlineBlock,

      &.hover(
        unsafeChild("div")(
          display.block
        )
      )
    )

    val button = style(
      outline.`0`,
      border.`0`,
      padding(12 px),
      color(c"#333"),
      cursor.pointer,

      &.hover(
        backgroundColor(c"#74a9d8")
      )
    )

    val dropdownContent = style(
      display.none,
      position.absolute,
      backgroundColor(c"#f9f9f9"),
      left(-35 px),
      minWidth(160 px),
      boxShadow := "0px 8px 16px 0px rgba(0,0,0,0.32)",
      zIndex(1),

      unsafeChild("a")(
        color.black,
        padding(12 px, 16 px),
        textDecoration := "none",
        display.block,

        &.hover(
          backgroundColor(c"#74a9d8"),
          color.white
        )
      )
    )

  }
}

class DropdownMenu(text: List[scalatags.JsDom.Modifier], elements: List[JsDom.TypedTag[HTMLElement]]) {

  val component: JsDom.TypedTag[HTMLElement] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    div(Style.dropdown,
      a(Style.button, text),
      div(Style.dropdownContent, elements)
    )
  }

  def render(): HTMLElement = component.render
}
