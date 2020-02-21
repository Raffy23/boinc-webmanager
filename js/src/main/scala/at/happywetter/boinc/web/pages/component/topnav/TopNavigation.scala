package at.happywetter.boinc.web.pages.component.topnav

import at.happywetter.boinc.web.css.definitions.pages.{BoincClientStyle => BoincClientStyle}
import at.happywetter.boinc.web.css.definitions.components.{TopNavigation => Style}
import at.happywetter.boinc.web.pages.PageLayout
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
    cur.classList.remove(Style.nav.htmlClass)

    val next = dom.document.querySelector(s"#$componentId a[data-nav=$elem]")
    next.classList.add(Style.nav.htmlClass)

    selected = elem
  }

  def clear(): Unit = PageLayout.clearNav()

  def render(select: String = selected): TopNavigation = {
    selected = select

    PageLayout.nav :=
      <ul class={Style.nav.htmlClass} id={componentId} mhtml-onmount={jsUpdatePageLinksAction}>
        {
        links.map { case (nav, name, icon) =>
          <li class={BoincClientStyle.inTextIcon.htmlClass}>
            <a href={link(nav)} data-navigo={true} data-nav={nav}
               class={if(selected == nav) Some(Style.active.htmlClass) else None}>
              <i class={icon} aria-hidden="true"></i>
              <span class={Style.bigScreenOnly.htmlClass}>{name.localize}</span>
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
