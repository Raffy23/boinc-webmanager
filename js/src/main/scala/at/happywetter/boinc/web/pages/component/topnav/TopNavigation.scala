package at.happywetter.boinc.web.pages.component.topnav

import at.happywetter.boinc.web.css.definitions.components.{TopNavigation => Style}
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.pages.PageLayout
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.RichRx._
import mhtml.{Rx, Var}
import org.scalajs.dom.Event

import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.02.2018
  */
trait TopNavigation:

  protected var selected: Var[String]
  val componentId: String

  protected val links: List[(String, String, String)]

  protected def link(nav: String): Rx[String]

  def select(elem: String): Unit = selected := elem

  def clear(): Unit = PageLayout.clearNav()

  def render(select: Option[String] = None): TopNavigation =
    select.foreach(selected := _)
    PageLayout.setNav(this)
    this

  lazy val component: Elem =
    <ul class={Style.nav.htmlClass} id={componentId}>
      {
      selected.map { selected =>
        links.map { case (nav, name, icon) =>
          <li class={BoincClientStyle.inTextIcon.htmlClass}>
            <a href={link(nav)} onclick={jsAction(link(nav))} class={
            if (selected == nav) Some(Style.active.htmlClass) else None
          }>
              <i class={icon} aria-hidden="true"></i>
              <span class={Style.bigScreenOnly.htmlClass}>{name.localize}</span>
            </a>
          </li>
        }
      }
    }
    </ul>

  private def jsAction(link: Rx[String]): (Event) => Unit = event => {
    event.preventDefault()
    AppRouter.router.navigate(link.now)
  }
