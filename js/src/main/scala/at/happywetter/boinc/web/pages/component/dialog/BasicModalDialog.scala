package at.happywetter.boinc.web.pages.component.dialog

import at.happywetter.boinc.web.pages.component.dialog.BasicModalDialog.Style

import scala.xml.{Elem, Node}
import scalacss.ProdDefaults._
import scalacss.internal.mutable.StyleSheet


/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.08.2017
  */
object BasicModalDialog {
  import scala.language.postfixOps

  object Style extends StyleSheet.Inline {
    import dsl._

    val modal = style(
      display.none,
      position.fixed,
      zIndex(101),
      paddingTop(100 px),
      left.`0`,
      top.`0`,
      width(100 %%),
      height(100 %%),
      overflow.hidden,
      backgroundColor :=! "rgba(0,0,0,0.5)"
    )

    val content = style(
      position.relative,
      backgroundColor(c"#FFF"),
      margin.auto,
      padding.`0`,
      minWidth(300 px),
      maxWidth(60 %%),
      maxHeight :=! "calc(100% - 200px)",
      overflow.auto
    )

    val modalBody = style(
      padding(2 px, 16 px)
    )

    val modalHeader = style(
      padding(2 px, 16 px),

      unsafeChild("h3")(
        borderBottom :=! "1px solid #DDD",
        fontSize(20 px),
        fontWeight._400
      )
    )

    val modalFooter = style(
      paddingBottom(4 px),
      paddingRight(10 px),
      textAlign.right
    )

    val button = style(
      outline.`0`,
      backgroundColor(c"#428bca"),
      border.`0`,
      padding(10 px),
      color(c"#FFFFFF"),
      cursor.pointer,
      margin(6 px, 6 px),

      &.hover(
        backgroundColor(c"#74a9d8")
      )
    )
  }
}

class BasicModalDialog(dialogID: String,
                       headerElement: List[Node],
                       contentElement: List[Node],
                       footerElement: List[Node]) extends Dialog(dialogID) {

  override def render(): Elem = {
    <div class={Style.modal.htmlClass} id={dialogID}>
      <div class={Style.content.htmlClass}>
        <div class={Style.modalHeader.htmlClass}>{headerElement}</div>
        <div class={Style.modalBody.htmlClass}>{contentElement}</div>
        <div class={Style.modalFooter.htmlClass}>{footerElement}</div>
      </div>
    </div>
  }
}
