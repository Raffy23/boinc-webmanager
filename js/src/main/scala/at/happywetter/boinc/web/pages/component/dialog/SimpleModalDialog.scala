package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.web.util.XMLHelper.toXMLTextNode
import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle => Style}
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.xml.{Elem, Node}

/**
  * Created by:
  *
  * @author Raphael
  * @version 08.08.2017
  */
class SimpleModalDialog(bodyElement: Node,
                        headerElement: Node = "",
                        okAction: (SimpleModalDialog) => Unit,
                        abortAction: (SimpleModalDialog) => Unit,
                        okLabel: String = "dialog_ok".localize,
                        abortLabel: String = "dialog_cancel".localize) extends Dialog("modal-dialog") {

  private val dialog = new
      BasicModalDialog("modal-dialog",
        List(headerElement),
        List(bodyElement),
        List(
          <button class={Style.button.htmlClass} onclick={(event: Event) => {
            event.preventDefault()
            okAction(this)
          }}>
            {okLabel}
          </button>,
          <button class={Style.button.htmlClass} onclick={(event: Event) => {
            event.preventDefault()
            abortAction(this)
          }}>
            {abortLabel}
          </button>
        )
      )

  override def render(): Elem = dialog.render()

  def close(): Unit = SimpleModalDialog.remove()

}

object SimpleModalDialog {
  def remove(): Unit = dom.document.body.removeChild(dom.document.getElementById("modal-dialog"))
}
