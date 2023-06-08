package at.happywetter.boinc.web.css.components

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object Tooltip extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.components.Tooltip._
  import dsl._

  errorIcon.cssName - (
    float.right,
    color(c"#FF8181")
  )

  loadingIcon.cssName - (
    float.right,
    color(c"#428bca")
  )

  tooltipText.cssName - (
    position.absolute,
    visibility.hidden,
    width(100 px),
    backgroundColor :=! "rgba(3,3,3,0.80)",
    color.white,
    textAlign.center,
    padding(5 px, 5 px, 5 px, 5 px),
    zIndex(101),
    boxShadow := " 0 12px 18px rgba(0,0,0,0.25), 0 5px 5px rgba(0,0,0,0.22)"
  )

  topText.cssName - (
    bottom(100 %%),
    left(50 %%),
    marginLeft(-50 px),
    marginBottom(4 px)
  )

  leftText.cssName - (
    bottom.auto,
    right(128 %%),
    top(-5 px)
  )

  tooltip.cssName - (
    position.relative,
    display.inlineBlock,
    &.hover(
      unsafeChild(s".${tooltipText.htmlClass}")(
        visibility.visible
      )
    )
  )
