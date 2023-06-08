package at.happywetter.boinc.web.css.pages

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object LoginPageCSS extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.pages.LoginPageStyle._
  import at.happywetter.boinc.web.css.definitions.pages.LoginPageStyle.{content => style_content}
  import dsl._

  style_content.cssName - (
    position.relative,
    zIndex(1),
    backgroundColor(c"#FFFFFF"),
    maxWidth(360 px),
    margin(250 px, auto),
    padding(10 px, 45 px, 45 px, 45 px),
    textAlign.center,
    boxShadow := " 0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22)"
  )

  input.cssName - (
    width(100 %%),
    padding(15 px),
    margin(0 px, 0 px, 15 px, 0 px)
  )

  headerBar.cssName - (
    position.fixed,
    top.`0`,
    left.`0`,
    width(100 %%),
    height(75 px),
    backgroundColor(c"#424242"),
    color(c"#F2F2F2"),
    boxShadow := " 0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22)",
    textAlign.center
  )

  button.cssName - (
    textTransform.uppercase,
    outline.`0`,
    backgroundColor(c"#428bca"),
    width(100 %%),
    border.`0`,
    padding(15 px),
    color(c"#FFFFFF"),
    cursor.pointer,
    margin.`0`,
    &.hover(
      backgroundColor(c"#74a9d8")
    )
  )
