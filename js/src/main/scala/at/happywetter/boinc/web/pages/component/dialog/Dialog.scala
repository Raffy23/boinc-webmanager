package at.happywetter.boinc.web.pages.component.dialog

import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.08.2017
  */
abstract class Dialog(dialogID: String) {

  def render(): Elem

  def renderToBody(): Dialog = {
    val existingDialog = dom.document.getElementById(dialogID)
    if (existingDialog != null)
      dom.document.body.removeChild(existingDialog)

    mhtml.mount(dom.document.body, render())
    this
  }

  def hide(): Unit = dom.document.getElementById(dialogID).asInstanceOf[HTMLElement].style = ""
  def show(): Unit = dom.document.getElementById(dialogID).asInstanceOf[HTMLElement].style = "display:block;"
}
