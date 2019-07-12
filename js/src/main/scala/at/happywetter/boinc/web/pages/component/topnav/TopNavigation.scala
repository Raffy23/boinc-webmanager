package at.happywetter.boinc.web.pages.component.topnav

import at.happywetter.boinc.web.css.TopNavigation
import at.happywetter.boinc.web.pages.PageLayout
import at.happywetter.boinc.web.pages.boinc.BoincClientLayout
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.util.I18N._
import mhtml.Rx
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.02.2018
  */
trait TopNavigation {

  protected var selected: String
  protected val componentId: String

  protected val links: List[(String, String, String)]

  protected def link(nav: String): Rx[String]

  def select(elem: String): Unit = {
    val cur = dom.document.querySelector(s"#$componentId a[data-nav=$selected]")
    cur.classList.remove(TopNavigation.nav.htmlClass)

    val next = dom.document.querySelector(s"#$componentId a[data-nav=$elem]")
    next.classList.add(TopNavigation.nav.htmlClass)

    selected = elem
  }

  def clear(): Unit = PageLayout.clearNav()

  def render(select: String = selected): TopNavigation = {
    selected = select

    PageLayout.nav :=
      <ul class={TopNavigation.nav.htmlClass} id={componentId} mhtml-onmount={jsUpdatePageLinksAction}>
        {
        links.map { case (nav, name, icon) =>
          <li class={BoincClientLayout.Style.in_text_icon.htmlClass}>
            <a href={link(nav)} data-navigo={true} data-nav={nav}
               class={if(selected == nav) Some(TopNavigation.active.htmlClass) else None}>
              <i class={icon} aria-hidden="true"></i>
              <span class={TopNavigation.invisible_on_small_screen.htmlClass}>{name.localize}</span>
            </a>
          </li>
        }
        }
      </ul>

    this
  }

  private lazy val jsUpdatePageLinksAction: (dom.html.UList) => Unit = (_) => {
    Future {
      AppRouter.router.updatePageLinks()
    }
  }
}
