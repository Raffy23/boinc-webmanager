package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.css.TopNavigation
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.routes.AppRouter.BoincClientLocation
import mhtml.Var

import scala.xml.Elem
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 06.02.2018
  */
object BoincTopNavigation {

  private var selected = "boinc"
  private val links = List(
    ("boinc", "head_menu_boinc", "fa fa-id-card-o"),
    ("messages", "head_menu_messages", "fa fa-envelope-o"),
    ("projects", "head_menu_projects", "fa fa-tag"),
    ("tasks", "head_menu_tasks", "fa fa-tasks"),
    ("transfers", "head_menu_transfers", "fa fa-exchange"),
    ("statistics", "head_menu_statistics", "fa fa-area-chart"),
    ("disk", "head_menu_disk", "fa fa-pie-chart"),
    ("global_prefs", "head_menu_prefs", "fa fa-cogs")
  )

  val clientName: Var[String] = Var("none")
  val component: Var[Elem] = Var(<span></span>)

  def select(elem: String): Unit = {
    val cur = dom.document.querySelector(s"#boinc_top_navbar a[data-nav=$selected]")
    cur.classList.remove(TopNavigation.nav.htmlClass)

    val next = dom.document.querySelector(s"#boinc_top_navbar a[data-nav=$elem]")
    next.classList.add(TopNavigation.nav.htmlClass)

    selected = elem
  }

  def clear(): Unit = component := {<span></span>}
  def render(select: String = selected): Unit = {
    selected = select

    component :=
      <ul class={TopNavigation.nav.htmlClass} id="boinc_top_navbar">
        {
        links.map { case (nav, name, icon) =>
          <li class={BoincClientLayout.Style.in_text_icon.htmlClass}>
            <a href={s"${BoincClientLocation.link}/$clientName/$nav"} data-navigo="true" data-nav={nav}
               class={if(selected == nav) Some(TopNavigation.active.htmlClass) else None}>
              <i class={icon}></i>
              <span class={TopNavigation.invisible_on_small_screen.htmlClass}>{name.localize}</span>
            </a>
          </li>
        }
        }
      </ul>

    AppRouter.router.updatePageLinks()
  }
}
