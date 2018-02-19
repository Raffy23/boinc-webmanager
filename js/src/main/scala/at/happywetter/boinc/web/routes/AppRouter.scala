package at.happywetter.boinc.web.routes

import at.happywetter.boinc.web.pages.Layout
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.collection.mutable
import scala.scalajs.js
import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 23.07.2017
  */
object AppRouter {

  trait Page {
    def link: String = AppRouter.href(this)
  }
  case object LoginPageLocation extends Page
  case object DashboardLocation extends Page
  case object BoincHomeLocation extends Page

  case object BoincClientLocation extends Page {
    override def link: String = "/view/boinc-client"
  }
  case object SettingsLocation extends Page {
    override def link: String = "/view/settings"
  }
  case class SwarmControlLocation(path: String = "") extends Page {
    override def link: String = "/view/swarm"
  }
  case object HardwareLocation extends Page {
    override def link: String = "/view/hardware"
  }


  val defaultPageHook = new Hook {
    override def already(): Unit = {}

    override def before(done: js.Function0[Unit]): Unit = {
      NProgress.start()
      done()
    }

    override def leave(): Unit = {}

    override def after(): Unit = {}
  }


  val routes: mutable.Map[Page, (String,Layout)] = new mutable.HashMap[Page, (String,Layout)]()
  val router = new Navigo()

  def addRoute(page: Page, path: String, layout: Layout): Unit = {
    routes.put(page, (path,layout))
    router.on(
      path,

      (params: js.Dictionary[String]) => {
        LayoutManager.renderLayout(params, layout)
      },

      new Hook {
        override def already(): Unit = layout.already()
        override def before(done: js.Function0[Unit]): Unit = layout.before(done)
        override def leave(): Unit = layout.leave()
        override def after(): Unit = layout.after()
      }
    )
  }

  def navigate(page: Page): Unit = router.navigate(routes(page)._1, absolute = true)
  def navigate(page: String): Unit = router.navigate(page, absolute = true)

  def navigate(e: Event, page: Page): Unit = {
    this.navigate(page)
    e.preventDefault()
  }


  def href(page: Page): String = routes(page)._1.replaceAll("\\/:\\w*[$]?","")

  def openExternalLink(link: String): Unit = {
    dom.window.open(link, "_blank")
  }

  val openExternal: (Event) => Unit = (event) => {
    event.preventDefault()
    dom.window.open(event.target.asInstanceOf[HTMLElement].getAttribute("href"), "_blank")
  }

  def current: String = dom.window.location.pathname

  def isOn(page: Page): Boolean = current == page.link

}
