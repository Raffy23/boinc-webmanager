package at.happywetter.boinc.web.pages
import at.happywetter.boinc.web.pages.boinc.{BoincGlobalPrefsLayout, BoincMainHostLayout, BoincProjectLayout, BoincTaskLayout}
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.routes.{AppRouter, Hook, LayoutManager}
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.{Dictionary, UndefOr}
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.08.2017
  */
object BoincLayout extends Layout {

  private val INITAL_STATE = "boinc"

  var child: BoincPageLayout = _
  private var currentState: String = "NONE"

  override val requestedParent = Some("main #client-container")
  override def requestParentLayout() = { Some(Dashboard) }

  override val component: JsDom.TypedTag[HTMLElement] =  {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._

    div(BoincClientLayout.Style.content, id := "client-data")
  }

  override val routerHook: Option[Hook] = Some(new Hook {
    override def already(): Unit = child.routerHook.foreach(p => p.already())

    override def before(done: js.Function0[Unit]): Unit = {done()}

    override def leave(): Unit = {
      if (child != null) {
        child.routerHook.foreach(p => p.leave())
        dom.document.getElementById("client-data").innerHTML = ""
      }
    }

    override def after(): Unit = {
      if(child!=null) child.routerHook.foreach(p => p.after())
    }

  })

  override def beforeRender(params: Dictionary[String]): Unit = {
    val oldChild = child

    def updateView(implicit view: String): Unit = {
      if (view == currentState) {
        child.routerHook.foreach(p => p.already())
      } else {
        if (child != null)
          child.routerHook.foreach(p => p.leave())

        val nChild = view match {
          case "boinc"    => new BoincMainHostLayout(params)
          case "projects" => new BoincProjectLayout(params)
          case "tasks"    => new BoincTaskLayout(params)
          case "global_prefs" => new BoincGlobalPrefsLayout(params)
        }

        currentState = view
        nChild.routerHook.foreach(p => p.before(() => {}))
        child = nChild
      }
    }

    params.getOrElse("action","_DEFAULT_ACTION_") match {
      case view @ "boinc"      => updateView(view)
      case view @ "messages"   => dom.window.alert("Not implemented")
      case view @ "projects"   => updateView(view)
      case view @ "tasks"      => updateView(view)
      case view @ "transfers"  => dom.window.alert("Not implemented")
      case view @ "statistics" => dom.window.alert("Not implemented")
      case view @ "disk"       => dom.window.alert("Not implemented")
      case view @ "global_prefs" => updateView(view)
      case _ =>
        if (child != null)
          child.routerHook.foreach(p => p.leave())

        child = null
        currentState = "NONE"

        // Delay navigation, maybe we are currently in one ...
        dom.window.setTimeout(() => {
          oldChild match {
            case null => AppRouter.router.navigate("/view/dashboard/" + params.get("client").get + "/" + INITAL_STATE, absolute = true)
            case _: BoincMainHostLayout => AppRouter.router.navigate("/view/dashboard/" + params.get("client").get + "/boinc", absolute = true)
            case _: BoincProjectLayout => AppRouter.router.navigate("/view/dashboard/" + params.get("client").get + "/projects", absolute = true)
            case _: BoincTaskLayout => AppRouter.router.navigate("/view/dashboard/" + params.get("client").get + "/tasks", absolute = true)
            case _: BoincGlobalPrefsLayout => AppRouter.router.navigate("/view/dashboard/" + params.get("client").get + "/global_prefs", absolute = true)
            case _ => AppRouter.router.navigate("/view/dashboard/" + params.get("client").get + "/" + INITAL_STATE, absolute = true)
          }
        }, 100)
    }

  }

  override def onRender(): Unit = {
    println(dom.window.location.pathname)
    if (child != null) LayoutManager.render(child)
  }
}
