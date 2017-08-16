package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.pages.component.Tooltip.Style
import org.scalajs.dom.raw.HTMLElement

import scalacss.DevDefaults._
import scala.language.postfixOps
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
object Tooltip {

  object Style extends StyleSheet.Inline {
    import dsl._

    val tooltipText = style(
      position.absolute,
      visibility.hidden,
      width(100 px),
      backgroundColor :=! "rgba(3,3,3,0.80)",
      color.white,
      textAlign.center,
      padding(5 px, 5 px, 5 px, 5 px),
      zIndex(1),
      boxShadow := " 0 12px 18px rgba(0,0,0,0.25), 0 5px 5px rgba(0,0,0,0.22)",
    )

    val topText = style(
      bottom(100 %%),
      left(50 %%),
      marginLeft(-50 px),
      marginBottom(4 px)
    )

    val leftText = style(
      bottom.auto,
      right(128 %%),
      top(-5 px)
    )

    val tooltip = style(
      position.relative,
      display.inlineBlock,

      &.hover(
        unsafeChild(s".${tooltipText.htmlClass}")(
          visibility.visible
        )
      )
    )
  }

  def unapply(arg: Tooltip): HTMLElement = arg.render()
  def apply(text: String, parent: JsDom.TypedTag[HTMLElement]): Tooltip = new Tooltip(text, parent)

  //implicit def toHTMLElement(arg: Tooltip): HTMLElement = arg.unapply()
}

class Tooltip(text: String, parent: JsDom.TypedTag[HTMLElement], textOrientation: StyleA = Style.topText) {

  val component: JsDom.TypedTag[HTMLElement] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    div(Style.tooltip, parent, span(Style.tooltipText,textOrientation, text))
  }

  def render(): HTMLElement = component.render
}
