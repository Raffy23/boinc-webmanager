package at.happywetter.boinc.web.css.pages

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scalacss.internal.mutable.StyleSheet

import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincGlobalPrefsCSS extends StyleSheet.Standalone {
  import at.happywetter.boinc.web.css.definitions.pages.BoincGlobalPrefsStyle._
  import dsl._

  rootPane.cssName - (
    unsafeChild("label")(
      marginLeft(15 px)
    )
  )

  input.cssName - (
    outline.`0`,
    backgroundColor(c"#FFF"),
    width(4 em),
    border :=! "1px #AAA solid",
    margin(3 px, 6 px),
    padding(6 px, 8 px),
    boxSizing.borderBox,
    fontSize(14 px)
  )

  h4Padding.cssName - (
    marginTop(30 px)
  )

}
