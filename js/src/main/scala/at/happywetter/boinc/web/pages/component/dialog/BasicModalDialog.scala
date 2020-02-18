package at.happywetter.boinc.web.pages.component.dialog
import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle => Style}
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
class BasicModalDialog(dialogID: String,
                       headerElement: List[Node],
                       contentElement: List[Node],
                       footerElement: List[Node]) extends Dialog(dialogID) {

  private val maxHeight = dom.window.innerHeight - 350.0D

  private val jsBackgroundAction: Event => Unit = { event =>
    if (event.target.asInstanceOf[HTMLElement].id == dialogID)
      this.hide()
  }

  override def render(): Elem = {
    <div class={Style.modal.htmlClass} id={dialogID} onclick={jsBackgroundAction}>
      <div class={Style.content.htmlClass}>
        <div class={Style.header.htmlClass}>{headerElement}</div>
        <div class={Style.body.htmlClass} style={s"max-height:${maxHeight}px"}>{contentElement}</div>
        <div class={Style.footer.htmlClass}>{footerElement}</div>
      </div>
    </div>
  }

}
