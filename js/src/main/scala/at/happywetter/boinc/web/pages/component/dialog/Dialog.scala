package at.happywetter.boinc.web.pages.component.dialog

import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.08.2017
  */
abstract class Dialog(protected val dialogID: String):

  def render(): Elem

  def renderToBody(): Dialog =
    val existingDialog = dom.document.getElementById(dialogID)
    if (existingDialog != null)
      dom.document.body.removeChild(existingDialog)

    mhtml.mount(dom.document.body, render())
    this

  def hide(): Unit = Dialog.hideByID(dialogID)
  def show(): Unit = Dialog.showByID(dialogID)
  def destroy(): Unit = dom.document.body.removeChild(dom.document.getElementById(dialogID))

object Dialog {

  def exists(dialogID: String): Boolean = dom.document.getElementById(dialogID) != null
  def hideByID(dialogID: String): Unit = dom.document.getElementById(dialogID).asInstanceOf[HTMLElement].style = ""
  def showByID(dialogID: String): Unit = dom.document.getElementById(dialogID).asInstanceOf[HTMLElement].style =
    "display:block;"

}
