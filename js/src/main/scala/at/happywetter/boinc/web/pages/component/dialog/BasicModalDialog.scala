package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.web.pages.component.dialog.BasicModalDialog.Style
import org.scalajs.dom.raw.HTMLElement

import scalacss.ProdDefaults._
import scalacss.internal.mutable.StyleSheet
import scalatags.JsDom


/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.08.2017
  */
object BasicModalDialog {
  import scala.language.postfixOps

  object Style extends StyleSheet.Inline {
    import dsl._

    val modal = style(
      display.none,
      position.fixed,
      zIndex(5),
      paddingTop(100 px),
      left.`0`,
      top.`0`,
      width(100 %%),
      height(100 %%),
      overflow.hidden,
      backgroundColor :=! "rgba(0,0,0,0.5)"
    )

    val content = style(
      position.relative,
      backgroundColor(c"#FFF"),
      margin.auto,
      padding.`0`,
      minWidth(300 px),
      maxWidth(60 %%),
    )

    val modalBody = style(
      padding(2 px, 16 px)
    )

    val modalHeader = style(
      padding(2 px, 16 px),

      unsafeChild("h3")(
        borderBottom :=! "1px solid #DDD",
        fontSize(20 px),
        fontWeight._400
      )
    )

    val modalFooter = style(
      paddingBottom(4 px),
      paddingRight(10 px),
      textAlign.right
    )

    val button = style(
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
}

class BasicModalDialog(dialogID: String,
                       headerElement: List[scalatags.JsDom.Modifier],
                       contentElement: List[scalatags.JsDom.Modifier],
                       footerElement: List[scalatags.JsDom.Modifier]) extends Dialog(dialogID) {

  val component: JsDom.TypedTag[HTMLElement] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    div(Style.modal, id := dialogID,
      div(Style.content,
        div(Style.modalHeader, headerElement),
        div(Style.modalBody, contentElement),
        div(Style.modalFooter, footerElement)
      )
    )
  }

  def render(): HTMLElement = component.render

}
