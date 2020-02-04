package at.happywetter.boinc.web.pages.component.dialog
import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle => Style}

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

  override def render(): Elem = {
    <div class={Style.modal.htmlClass} id={dialogID}>
      <div class={Style.content.htmlClass}>
        <div class={Style.header.htmlClass}>{headerElement}</div>
        <div class={Style.body.htmlClass}>{contentElement}</div>
        <div class={Style.footer.htmlClass}>{footerElement}</div>
      </div>
    </div>
  }

}
