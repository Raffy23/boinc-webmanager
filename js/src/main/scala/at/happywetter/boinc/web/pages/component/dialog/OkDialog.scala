package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.web.pages.component.dialog.BasicModalDialog.Style
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scalacss.ScalatagsCss._
import scalatags.JsDom.all._
/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.08.2017
  */
class OkDialog(title: String, content: List[Modifier], action: (OkDialog) => Unit = (_) => {}) extends Dialog("modal-dialog-type1") {

  private val dialog = new BasicModalDialog("modal-dialog-type1",
    List(h3(title)),
    List(content),
    List(
      button(name := "dialog_ok_btn",
        Style.button,
        "dialog_ok".localize,
        onclick := { (event: Event) => {
          event.preventDefault()
          action(this)
          this.hide()
        }},
        autofocus)
    )
  )

  override def render(): HTMLElement = dialog.render()

  override def show(): Unit = {
    super.show()
    dom.document.querySelector("#modal-dialog button[name='dialog_ok_btn']").asInstanceOf[HTMLElement].focus()
  }
}
