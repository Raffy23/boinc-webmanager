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
  //rootElement.appendChild(PageLayout.footer.render)

  def render(page: Layout): Unit = {
    dom.window.console.log(s"LayoutManager: Rendering ${page.getClass.getSimpleName}")
    val mainElement = loadMainElement(page)

    mainElement.innerHTML = ""

    if (page.staticComponent.isDefined)
      mainElement.appendChild(page.staticComponent.get.render)

    val pageContent = page.render
    if (pageContent.isDefined)
      mainElement.appendChild(pageContent.get.render)

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
