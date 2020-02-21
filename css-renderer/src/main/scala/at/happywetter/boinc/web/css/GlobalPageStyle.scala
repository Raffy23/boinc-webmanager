package at.happywetter.boinc.web.css

import AppCSS.CSSDefaults._
import at.happywetter.boinc.web.css.components.Button

import scala.language.postfixOps

/**
 * Created by:
 *
 * @author Raphael
 * @version 23.07.2017
 */
object GlobalPageStyle extends StyleSheet.Standalone {
  import dsl._

  "html" - (
    height(100 %%),
    width(100 %%),
    position.absolute
  )

  "body" - (
    position.absolute,
    margin.`0`,
    padding.`0`,
    fontSize(14 px),
    fontFamily :=! "Roboto, sans-serif",
    height(100 %%),
    width(100 %%),
    backgroundColor(c"#f7f7f7"),
    backgroundSize := "cover"
  )

  "main" - (
    marginTop(80 px),
    height(100 %%),
    marginBottom(30 px),

    media.maxWidth(690 px)(
      marginTop(130 px)
    )
  )

  "button" - (
    Button.normal
  )

}
