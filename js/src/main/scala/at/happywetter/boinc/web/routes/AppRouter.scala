package at.happywetter.boinc.web.routes

import at.happywetter.boinc.web.pages.Layout
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.Dictionary

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
  case object SettingsLocation extends Page
  case object BoincHomeLocation extends Page


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
        layout.beforeRender(params)
        LayoutManager.render(layout)
      },

      layout.routerHook.getOrElse(defaultPageHook)
    )
  }

  def navigate(page: Page): Unit = router.navigate(routes(page)._1, absolute = true)

  def navigate(e: Event, page: Page): Unit = {
    this.navigate(page)
    e.preventDefault()
  }


  def href(page: Page): String = routes(page)._1

  val openExternal: js.Function1[Event, Unit] = (event: Event) => {
    event.preventDefault()
    dom.window.open(event.target.asInstanceOf[HTMLElement].getAttribute("href"), "_blank")
  }

}
