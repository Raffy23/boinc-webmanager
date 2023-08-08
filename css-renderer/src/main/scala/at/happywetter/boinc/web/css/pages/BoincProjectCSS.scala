package at.happywetter.boinc.web.css.pages

import scala.language.postfixOps

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincProjectCSS extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.pages.BoincProjectStyle._

  import dsl._

  link.cssName - (
    cursor.pointer,
    textDecoration := none,
    color(c"#333"),
    &.hover(
      textDecoration := "underline #333"
    )
  )

  firstRowFixedWith.cssName - (
    unsafeChild("tbody > tr > td:first-child")(
      maxWidth(100 px)
    )
  )

  floatingHeadbar.cssName - (
    position.absolute,
    top(80 px),
    right(20 px)
  )

  floatingHeadbarButton.cssName - (
    color(c"#333"),
    textDecoration := none,
    fontSize(30 px),
    cursor.pointer
  )
