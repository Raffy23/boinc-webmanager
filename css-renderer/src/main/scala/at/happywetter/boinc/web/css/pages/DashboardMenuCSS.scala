package at.happywetter.boinc.web.css.pages

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object DashboardMenuCSS extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.pages.DashboardMenuStyle._
  import dsl._

  private val menuMargin = 10

  menu.cssName - (
    position.fixed,
    overflowX.auto,
    bottom.`0`,
    top(50 px),
    listStyleType := "none",
    margin.`0`,
    padding(15 px, 0 px, 0 px, 0 px),
    backgroundColor(c"#e6e6e6"),
    width(207 px),
    border :=! "1px solid #EEE",
    boxShadow := " 0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22)",
    zIndex :=! "98",
    media.maxWidth(690 px)(
      top(100 px)
    )
  )

  elem.cssName - (
    unsafeChild("a")(
      display.block,
      width(207 px),
      textDecoration := "none",
      padding(menuMargin px, 15 px),
      boxSizing.borderBox,
      color(c"#333"),
      &.hover(
        backgroundColor(c"#74a9d8"),
        color.white
      ),
      unsafeChild("i")(
        marginRight(menuMargin px),
        width(1 em)
      )
    )
  )

  active.cssName - (
    backgroundColor(c"#428bca"),
    color :=! "white !important"
  )

  clickable.cssName - (
    cursor.pointer
  )

  subMenuHosts.cssName - (
    float.right,
    margin(0 px, (-1 * menuMargin + 5) px, 0 px, 0 px),
    fontSize.smaller,
    backgroundColor(c"#757575"),
    color.white,
    minWidth(20 px),
    border(1 px, solid, c"#757575"),
    borderRadius(6 px),
    textAlign.center
  )
