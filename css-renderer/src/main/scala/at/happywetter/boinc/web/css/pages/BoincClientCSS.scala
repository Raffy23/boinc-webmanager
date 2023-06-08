package at.happywetter.boinc.web.css.pages

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincClientCSS extends StyleSheet.Standalone:

  import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle._
  import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle.{content => style_content}
  import dsl._

  pageHeader.cssName - (
    paddingBottom(9 px),
    margin(40 px, 20 px, 20 px, auto),
    borderBottom :=! "1px solid #DDD",
    fontSize(28 px),
    fontWeight._300,
    unsafeChild("i")(
      marginRight(10 px)
    )
  )

  pageHeaderSmall.cssName - (
    paddingBottom(9 px),
    margin(40 px, 20 px, 20 px, auto),
    borderBottom :=! "1px solid #DDD",
    fontSize(25 px),
    fontWeight._300
  )

  h4.cssName - (
    paddingBottom(9 px),
    margin(10 px, 20 px, 20 px, auto),
    borderBottom :=! "1px solid #DDD",
    fontSize(19 px),
    fontWeight._300
  )

  h4WithoutLine.cssName - (
    paddingBottom(9 px),
    margin(10 px, 20 px, 5 px, auto),
    fontSize(19 px),
    fontWeight._300
  )

  style_content.cssName - (
    paddingLeft(8 px)
  )

  inTextIcon.cssName - (
    unsafeChild("i")(
      marginRight(10 px)
    )
  )

  progressBar.cssName - (
    unsafeChild("progress")(
      backgroundColor(c"#EEE"),
      border.`0`,
      height(18 px),
      width :=! "calc(100% - 3em)"
    ),
    unsafeChild("progress[value]::progress-value")(
      backgroundColor(c"#428bca"),
      borderRadius(1 px),
      boxShadow := "0 2px 2px rgba(0, 0, 0, 0.25) inset"
    ),
    unsafeChild("progress[value]::-webkit-progress-value")(
      backgroundColor(c"#428bca"),
      borderRadius(1 px),
      boxShadow := "0 2px 2px rgba(0, 0, 0, 0.25) inset"
    ),
    unsafeChild("progress[value]::-webkit-progress-ue")(
      backgroundColor(c"#428bca")
    ),
    unsafeChild("progress[value]::-webkit-progress-bar")(
      backgroundColor(c"#eee"),
      borderRadius(1 px),
      boxShadow := "0 2px 2px rgba(0, 0, 0, 0.25) inset"
    ),
    unsafeChild("progress::-moz-progress-bar")(
      backgroundColor(c"#428bca"),
      borderRadius(1 px),
      boxShadow := "0 2px 2px rgba(0, 0, 0, 0.25) inset"
    ),
    unsafeChild("span")(
      float.right
    )
  )
