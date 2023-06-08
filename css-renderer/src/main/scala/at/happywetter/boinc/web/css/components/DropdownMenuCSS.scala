package at.happywetter.boinc.web.css.components

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object DropdownMenuCSS extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.components.DropDownMenuStyle._
  import dsl._

  dropdown.cssName - (
    position.relative,
    display.inlineBlock,
    &.hover(
      unsafeChild("div")(
        display.block
      )
    )
  )

  button.cssName - (
    outline.`0`,
    border.`0`,
    padding(12 px),
    color(c"#333"),
    cursor.pointer,
    &.hover(
      backgroundColor(c"#74a9d8")
    )
  )

  dropdownContent.cssName - (
    display.none,
    position.absolute,
    backgroundColor(c"#f9f9f9"),
    // left(-35 px),
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
