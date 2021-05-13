package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle => Style}
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.xml.{Elem, Node}
/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.08.2017
  */
class OkDialog(title: String, content: List[Node], action: OkDialog => Unit = _ => {}) extends Dialog("modal-dialog-type1") {

  private val dialog = new BasicModalDialog("modal-dialog-type1",
    List(<h3>{title}</h3>),
    content,
    List(
      <button name="dialog_ok_btn" class={Style.button.htmlClass} onclick={(event: Event) => {
        event.preventDefault()
        action(this)
        this.hide()
        this.destroy()
      }} autofocus="autofocus">
        {"dialog_ok".localize}
      </button>
    )
  )

  override def render(): Elem = dialog.render()

  override def show(): Unit = {
    super.show()
    dom.document.querySelector(s"#$dialogID button[name='dialog_ok_btn']").asInstanceOf[HTMLElement].focus()
  }

}
