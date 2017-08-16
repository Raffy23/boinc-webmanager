package at.happywetter.boinc.web.routes

import at.happywetter.boinc.web.pages.{Dashboard, Layout, PageLayout}
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLCollection, HTMLElement}

import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.07.2017
  */
object LayoutManager {

  private val rootElement = dom.document.getElementById("app-container")

  rootElement.innerHTML = ""
  rootElement.appendChild(PageLayout.heading.render)
  rootElement.appendChild(JsDom.tags2.main().render)
  rootElement.appendChild(PageLayout.footer.render)

  def render(page: Layout): Unit = {
    val mainElement = loadMainElement(page)
    dom.window.console.log("Rendering View: " + page.getClass.getSimpleName)

    mainElement.innerHTML = ""
    mainElement.appendChild(page.component.render)
    page.onRender()
  }


  private def loadMainElement(page: Layout): HTMLElement = {
    val firstTry = dom.document.querySelector(page.requestedParent.getOrElse("main"))

    if (firstTry == null) {
      render(page.requestParentLayout().get)
      dom.document.querySelector(page.requestedParent.getOrElse("main")).asInstanceOf[HTMLElement]
    } else {
      firstTry.asInstanceOf[HTMLElement]
    }

  }

  def init(): Unit = {}
}
