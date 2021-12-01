package at.happywetter.boinc.web.routes

import at.happywetter.boinc.web.pages.Layout
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.collection.mutable
import scala.scalajs.js

/**
  * Created by: 
  *
  * @author Raphael
  * @version 23.07.2017
  */
object AppRouter {

  val routes: mutable.Set[Layout] = new mutable.HashSet[Layout]()
  val router = new Navigo(root = s"${dom.window.location.protocol}//${dom.window.location.host}")

  def +=[T <: Layout](layout: T): T = {
    addLayout(layout)
    layout
  }

  def addLayout(layout: Layout): Unit = {
    //println(s"addLayout($layout) => ${layout.link}")

    routes.add(layout)
    router.on(
      layout.link,

      (params: js.Dictionary[String]) => {
        LayoutManager.renderLayout(params, layout)
      },

      new Hook {
        override def already(params: js.Dictionary[String]): Unit = layout.already()
        override def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit = layout.before(done, params)
        override def leave(params: js.Dictionary[String]): Unit = layout.leave()
        override def after(params: js.Dictionary[String]): Unit = layout.after()
      }
    )
  }

  def navigate(layout: Layout): Unit = {
    this.navigate(layout.link)
  }

  def navigate(page: String): Unit = {
    router.navigate(page, absolute = true)
  }

  def navigate(e: Event, layout: Layout): Unit = {
    this.navigate(layout)
    e.preventDefault()
  }

  def href(page: Layout): String = page.link.replaceAll("\\/:\\w*[$]?","")

  def openExternalLink(link: String): Unit = {
    dom.window.open(link, "_blank")
  }

  val openExternal: (Event) => Unit = (event) => {
    event.preventDefault()
    dom.window.open(event.target.asInstanceOf[HTMLElement].getAttribute("href"), "_blank")
  }

  val onClick: (Event) => Unit = (event) => {
    event.preventDefault()
    navigate(event.target.asInstanceOf[HTMLElement].getAttribute("href"))
  }

  def current: String = dom.window.location.pathname

}
