package at.happywetter.boinc.web.css.pages

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincStatisticsCSS extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.pages.BoincStatisticsStyle._
  import dsl._

  button.cssName - (
    textDecoration := "none",
    outline.`0`,
    width(100 %%),
    border.`0`,
    padding(14 px),
    color(c"#333"),
    cursor.pointer,
    borderTop :=! "1px #AAA solid",
    borderRight :=! "1px #AAA solid",
    &.hover(
      backgroundColor(c"#c3daee")
    )
  )

  active.cssName - (
    backgroundColor(c"#c3daee")
  )
