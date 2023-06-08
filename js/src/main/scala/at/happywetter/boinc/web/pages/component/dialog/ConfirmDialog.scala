package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle => Style}
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.HTMLElement

import scala.xml.Elem
import scala.xml.Node

/**
 * Created by: 
 *
 * @author Raphael
 * @version 11.07.2020
 */
class ConfirmDialog(title: String,
                    content: List[Node],
                    okAction: ConfirmDialog => Unit,
                    abortAction: ConfirmDialog => Unit
) extends Dialog("confirm-dialog"):

  private val dialog = new BasicModalDialog(
    "confirm-dialog",
    List(<h3>{title}</h3>),
    content,
    List(
      <button name="dialog_ok_btn" class={Style.button.htmlClass} style="background-color:#42734B" onclick={
        (event: Event) => {
          event.preventDefault()
          okAction(this)
          this.hide()
        }
      } autofocus="autofocus">
        {"dialog_ok".localize}
      </button>,
      <button name="dialog_cancel_btn" class={Style.button.htmlClass} style="background-color:#940008" onclick={
        (event: Event) => {
          event.preventDefault()
          abortAction(this)
          this.hide()
        }
      } autofocus="autofocus">
        {"dialog_cancel".localize}
      </button>
    )
  )

  override def render(): Elem = dialog.render()

  override def show(): Unit =
    super.show()
    dom.document.querySelector("#mconfirm-dialog button[name='dialog_ok_btn']").asInstanceOf[HTMLElement].focus()
