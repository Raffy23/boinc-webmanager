package at.happywetter.boinc.web.routes

import at.happywetter.boinc.web.css.definitions.components.PageLayoutStyle
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.pages.{Layout, PageLayout}
import mhtml.Var
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.07.2017
  */
object LayoutManager {

  private val rootElement = dom.document.getElementById("app-container")
  private val childLayout = Var[Layout](new BootstrapLayout) //TODO
  private val mainContent = Var[Elem](<div>{childLayout.map(_.render)}</div>)

  def init(): Unit = {
    dom.console.log("LayoutManager.init()")
    rootElement.innerHTML = ""
    mhtml.mount(rootElement, PageLayout.heading)
    dom.console.log("LayoutManager.intt(): after mhtml.mount rootelement")
    mhtml.mount(rootElement,
      <main>
        {DashboardMenu.component}
        <div id="client-container" class={PageLayoutStyle.clientContainer.htmlClass}>
          {mainContent}
        </div>
      </main>
    )
    dom.console.log("LayoutManager.init(): finished")
  }

  def renderLayout(params: js.Dictionary[String], page: Layout): Unit = {
    page.beforeRender(params)
    render(page)
  }

  def render(page: Layout): Unit = {
    childLayout := page
    page.onRender()
  }

  class BootstrapLayout extends Layout {
    override val path: String = "/"
    override def render: Elem = {<div>__BOOTSTRAP_LAYAOUT__</div>}

    override def beforeRender(params: Dictionary[String]): Unit = {}
  }

}
