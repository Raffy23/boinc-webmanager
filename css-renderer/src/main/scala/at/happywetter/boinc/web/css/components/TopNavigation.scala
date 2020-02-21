package at.happywetter.boinc.web.css.components

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
  * Created by:
  *
  * @author Raphael
  * @version 02.08.2017
  */
object TopNavigation extends StyleSheet.Standalone {
  import at.happywetter.boinc.web.css.definitions.components.TopNavigation._
  import dsl._

  nav.cssName - (
    listStyleType := "none",
    margin.`0`,
    padding.`0`,
    overflow.hidden,
    position.absolute,
    top(1 px),
    right(15 px),

    media.maxWidth(690 px)(
      top(51 px)
    ),

    unsafeChild("li")(
      float.left
    ),

    unsafeChild("li a")(
      display.block,
      color.white,
      textAlign.center,
      padding(15 px, 15 px, 17 px, 15 px),
      textDecoration := "none",

      &.hover(
        backgroundColor(c"#666666")
      )
    ),
  )

  bigScreenOnly.cssName - (
    media.maxWidth(1220 px)(
      display.none
    )
  )

  active.cssName - (
    backgroundColor(c"#666666")
  )

}
