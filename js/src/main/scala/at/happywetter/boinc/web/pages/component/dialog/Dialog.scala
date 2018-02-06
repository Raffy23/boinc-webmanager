package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.web.pages.BoincClientLayout
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.xml.Elem
import scalacss.StyleSheet
import scalacss.ProdDefaults._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.08.2017
  */
object Dialog {
  object Style  extends StyleSheet.Inline {
    import dsl._
    import scala.language.postfixOps

    val header = style(
      BoincClientLayout.Style.pageHeader_small,
      marginTop(15 px)
    )
  }
}

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
