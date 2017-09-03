package at.happywetter.boinc.web.css

import scalacss.ProdDefaults._
import scala.language.postfixOps

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.08.2017
  */
object TopNavigation extends StyleSheet.Inline {
  import dsl._

  val nav = style(
    listStyleType := "none",
    margin.`0`,
    padding.`0`,
    overflow.hidden,
    position.fixed,
    top(1 px),
    right(5 px),

    unsafeChild("li")(
      float.left
    ),

    unsafeChild("li a")(
      display.block,
      color.white,
      textAlign.center,
      padding(15 px, 15 px, 16 px, 16 px),
      textDecoration := "none",

      &.hover(
        backgroundColor(c"#666666")
      )
    )
  )

  val active = style(
    backgroundColor(c"#666666")
  )

}
