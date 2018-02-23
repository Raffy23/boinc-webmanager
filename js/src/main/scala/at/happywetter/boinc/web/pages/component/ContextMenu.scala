package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.pages.component.ContextMenu.Style
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.{Event, MouseEvent}

import scala.language.postfixOps
import scala.scalajs.js
import scala.xml.{Elem, Node}
import scalacss.ProdDefaults._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 06.08.2017
  */
object ContextMenu {

  object Style extends StyleSheet.Inline {
    import dsl._


    val contextMenu = style(
      display.none,
      position.fixed,
      zIndex(1),
      backgroundColor.white,
      boxShadow := "0 14px 28px rgba(0,0,0,0.25), 0 10px 10px rgba(0,0,0,0.22)",

      unsafeChild("ul")(
        listStyle := "none",
        margin.`0`,
        padding(1 px, 0 px, 0 px, 0 px),
      )
    )

    val elem = style(
      unsafeChild("a")(
        display.block,
        width(125 px),
        textDecoration := "none",
        padding(8 px, 8 px, 8 px, 10 px),
        boxSizing.borderBox,
        color(c"#333"),
        fontSize(13 px),

        &.hover(
          backgroundColor(c"#d7e6f4")
        )
      )
    )

  }

}

class ContextMenu(contextMenuId: String) {

  private val elements = Var(List.empty[Node])

  lazy val component: Elem = {
    <div class={Style.contextMenu.htmlClass} id={contextMenuId}>
      <ul>{elements}</ul>
    </div>
  }

  def display(event: MouseEvent): Unit = display(event.clientX.toInt, event.clientY.toInt)

  def display(x: Int,y: Int): Unit = {
    val me = dom.document.getElementById(contextMenuId).asInstanceOf[HTMLElement]

    me.style =
      "display:block;" +
      "top:"+y+"px;" +
      "left:"+x+"px;"

    dom.window.setTimeout(() => { dom.document.body.addEventListener("click", hideListener) }, 500)
    dom.window.setTimeout(() => { dom.document.body.addEventListener("contextmenu", hideListener) }, 500)
  }

  private val hideListener: js.Function1[Event, Unit] = (_) => {
    dom.document.body.removeEventListener("click", hideListener)
    dom.document.body.removeEventListener("contextmenu", hideListener)

    hide()
  }

  def hide(): Unit = {
    val me = dom.document.getElementById(contextMenuId).asInstanceOf[HTMLElement]
    me.style = ""

    dom.document.body.removeEventListener("click", hideListener)
  }

  def removeMenuReferences(reference: String): Unit = {
    val contextMenu = dom.document.getElementById(contextMenuId).firstChild
    val elements = dom.document.querySelectorAll(s"div[id='$contextMenuId'] a[data-menu-id='$reference']")

    if(elements != null) {
      println(elements)

      import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList
      elements.forEach((node, _, _) => contextMenu.removeChild(node.parentNode))
    }
  }

  def addMenu(linkUrl: String, elementName: String, menuAction: (Event) => Unit, reference: Option[String] = None): Unit = {
    val newElement: Elem = {
      <li class={Style.elem.htmlClass}>
        <a href={linkUrl} data-menu-id={reference} data-navigo={linkUrl.startsWith("/")}
           data-external-url={if (!linkUrl.startsWith("/")) Some(linkUrl) else None}
           onclick={(event: Event) => {menuAction(event); event.preventDefault()}}>
          {elementName}
        </a>
      </li>
    }

    elements.update( _ :+ newElement)
  }

  def renderToBody(): ContextMenu = {
    val existingElement = dom.document.getElementById(contextMenuId)
    if (existingElement != null)
      dom.document.body.removeChild(existingElement)

    mhtml.mount(dom.document.body, component)
    this
  }

}
