package at.happywetter.boinc.web.css.components

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scala.language.postfixOps
/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object Button extends StyleSheet.Inline {
  import dsl._

  val headerButton: StyleA = style(
    textDecoration := "none",
    width(100 %%),
    padding(14 px),
    color(c"#333"),
    cursor.pointer,

    borderTop :=! "1px #AAA solid",
    borderRight :=! "1px #AAA solid",

    &.hover(
      backgroundColor(c"#c3daee")
    )
  )

  val normal: StyleA = style(
    textDecoration := "none",
    outline.`0`,

    padding(10 px),
    color(c"#ffffff").important,
    cursor.pointer,

    margin(6 px),
    padding(10 px),

    backgroundColor(c"#428bca"),

    &.hover(
      backgroundColor(c"#74a9d8")
    )
  )

  val btnActive: StyleA = style(
    backgroundColor(c"#c3daee")
  )

}
