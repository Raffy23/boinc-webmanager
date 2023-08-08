package at.happywetter.boinc.web.css.components

import scala.language.postfixOps

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object FloatingMenu extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.components.FloatingMenu._

  import dsl._

  root.cssName - (
    float.right,
    marginTop(12 px),
    marginRight(20 px),
    unsafeChild("a")(
      Button.headerButton,
      &.firstChild(
        borderLeft :=! "1px #AAA solid"
      )
    )
  )

  active.cssName - (
    backgroundColor(c"#c3daee")
  )
