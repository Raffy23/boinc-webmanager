package at.happywetter.boinc.web.css.components

import scala.language.postfixOps

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object ContextMenuCSS extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.components.ContextMenuStyle._

  import dsl._

  contextMenu.cssName - (
    display.none,
    position.fixed,
    zIndex(1),
    backgroundColor.white,
    boxShadow := "0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22)",
    unsafeChild("ul")(
      listStyle := "none",
      margin.`0`,
      padding(1 px, 0 px, 0 px, 0 px)
    )
  )

  elem.cssName - (
    unsafeChild("a")(
      display.block,
      width(125 px),
      textDecoration := "none",
      padding(8 px, 8 px, 8 px, 10 px),
      boxSizing.borderBox,
      color(c"#333"),
      fontSize(13 px),
      &.hover(
        backgroundColor(c"#d7e6f4")
      )
    )
  )
