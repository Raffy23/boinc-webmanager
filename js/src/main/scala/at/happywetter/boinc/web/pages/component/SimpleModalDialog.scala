package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.pages.component.SimpleModalDialog.Style
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.language.postfixOps
import scalacss.DevDefaults._
import scalatags.JsDom
import at.happywetter.boinc.web.util.I18N._

/**
  * Created by:
  *
  * @author Raphael
  * @version 08.08.2017
  */
class SimpleModalDialog(
                         bodyElement: JsDom.TypedTag[HTMLElement],
                         headerElement: JsDom.TypedTag[HTMLElement] = JsDom.tags.span,
                         okAction: (SimpleModalDialog) => Unit,
                         abortAction: (SimpleModalDialog) => Unit,
                         okLabel: String = "dialog_ok".localize,
                         abortLabel: String = "dialog_cancel".localize) {

  val component: JsDom.TypedTag[HTMLElement] = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    div(Style.modal, id := "modal-dialog",
      div(Style.content,
        div(Style.modalHeader, headerElement),
        div(Style.modalBody, bodyElement),
        div(Style.modalFooter,
          button(Style.button, okLabel, onclick := { (event: Event) => {
            event.preventDefault()
            okAction(this)
          }
          }),
          button(Style.button, abortLabel, onclick := { (event: Event) => {
            event.preventDefault()
            abortAction(this)
          }
          })
        ))
    )
  }

  def render(): HTMLElement = component.render

  def renderToBody(): SimpleModalDialog = {
    val existingDialog = dom.document.getElementById("modal-dialog")
    if (existingDialog != null)
      dom.document.body.removeChild(existingDialog)

    dom.document.body.appendChild(render())
    this
  }

  def hide(): Unit = dom.document.getElementById("modal-dialog").asInstanceOf[HTMLElement].style = ""
  def show(): Unit = dom.document.getElementById("modal-dialog").asInstanceOf[HTMLElement].style = "display:block;"
}

object SimpleModalDialog {

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
      minWidth(200 px),
      maxWidth(40 %%),
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
      padding(2 px, 16 px),
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

  def remove(): Unit = dom.document.body.removeChild(dom.document.getElementById("modal-dialog"))
}
