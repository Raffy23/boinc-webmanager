package at.happywetter.boinc.web.routes

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
    rootElement.innerHTML = ""
    mhtml.mount(rootElement, PageLayout.heading)
    mhtml.mount(rootElement,
      <main>
        {DashboardMenu.component}
        <div id="client-container" class={PageLayout.Style.clientContainer.htmlClass}>
          {mainContent}
        </div>
      </main>
    )
  }

  def renderLayout(params: js.Dictionary[String], page: Layout): Unit = {
    beforeRender(params)
    render(page)
  }

  def beforeRender(params: js.Dictionary[String]): Unit =
    childLayout.impure.run(_.beforeRender(params))

  def render(page: Layout): Unit = {
    dom.window.console.log(s"LayoutManager: Rendering ${page.getClass.getSimpleName}")

    childLayout := page
    page.onRender()
  }

  class BootstrapLayout extends Layout {
    override val path: String = "/"
    override def render: Elem = {<div>__BOOTSTRAP_LAYAOUT__</div>}

    override def beforeRender(params: Dictionary[String]): Unit = {}
  }

}
