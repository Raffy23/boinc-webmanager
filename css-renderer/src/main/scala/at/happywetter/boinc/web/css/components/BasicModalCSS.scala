package at.happywetter.boinc.web.css.components

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BasicModalCSS extends StyleSheet.Standalone {
  import at.happywetter.boinc.web.css.definitions.components.BasicModalStyle._
  import at.happywetter.boinc.web.css.definitions.components.BasicModalStyle.{content => style_content}
  import dsl._

  modal.cssName - (
    display.none,
    position.fixed,
    zIndex(101),
    paddingTop(100 px),
    left.`0`,
    top.`0`,
    width(100 %%),
    height(100 %%),
    overflow.hidden,
    backgroundColor :=! "rgba(0,0,0,0.5)"
  )

  style_content.cssName - (
    position.relative,
    backgroundColor(c"#FFF"),
    margin.auto,
    padding.`0`,
    minWidth(300 px),
    maxWidth(60 %%),
    maxHeight :=! "calc(100% - 200px)",
    overflow.auto
  )

  body.cssName - (
    padding(2 px, 16 px)
    )

  header.cssName - (
    padding(2 px, 16 px),

    unsafeChild("h3")(
      borderBottom :=! "1px solid #DDD",
      fontSize(20 px),
      fontWeight._400
    )
  )

  footer.cssName - (
    paddingBottom(4 px),
    paddingRight(10 px),
    textAlign.right
  )

  button.cssName - (
    outline.`0`,
    backgroundColor(c"#428bca"),
    border.`0`,
    padding(10 px),
    color(c"#FFFFFF"),
    cursor.pointer,
    margin(6 px, 6 px),

    &.hover(
      backgroundColor(c"#74a9d8")
    )
  )

}
