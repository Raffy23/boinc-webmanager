package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.web.pages.component.dialog.BasicModalDialog.Style
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scalatags.JsDom
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._

/**
  * Created by:
  *
  * @author Raphael
  * @version 08.08.2017
  */
class SimpleModalDialog(bodyElement: JsDom.TypedTag[HTMLElement],
                        headerElement: JsDom.TypedTag[HTMLElement] = JsDom.tags.span,
                        okAction: (SimpleModalDialog) => Unit,
                        abortAction: (SimpleModalDialog) => Unit,
                        okLabel: String = "dialog_ok".localize,
                        abortLabel: String = "dialog_cancel".localize) extends Dialog("modal-dialog") {

  private val dialog = new
      BasicModalDialog("modal-dialog",
        List(headerElement),
        List(bodyElement),
        List(
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
        )
      )

  override def render(): HTMLElement = dialog.render()
}

object SimpleModalDialog {
  def remove(): Unit = dom.document.body.removeChild(dom.document.getElementById("modal-dialog"))
}
