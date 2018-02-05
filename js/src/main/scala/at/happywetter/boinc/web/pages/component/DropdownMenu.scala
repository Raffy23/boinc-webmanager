package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.pages.component.DropdownMenu.Style
import mhtml.{Rx, Var}

import scala.xml.Elem
import scalacss.ProdDefaults._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
object DropdownMenu {

  object Style extends StyleSheet.Inline {
    import dsl._
    import scala.language.postfixOps

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
      //left(-35 px),
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

class DropdownMenu(text: Elem, elements: Rx[List[Elem]], dropdownStyle: String = "") {

  val component: Elem = {
    <div class={Style.dropdown.htmlClass}>
      <a class={Style.button.htmlClass}>{text}</a>
      <div class={Style.dropdownContent.htmlClass} style={dropdownStyle}>{elements}</div>
    </div>
  }

}
