package at.happywetter.boinc.web.pages
import at.happywetter.boinc.web.pages.boinc.{BoincMainHostLayout, BoincProjectLayout, BoincTaskLayout}
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

  var child: BoincPageLayout = _

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
      child.routerHook.foreach(p => p.leave())
      child = null
      dom.document.getElementById("client-data").innerHTML = ""
    }

    override def after(): Unit = if(child!=null) child.routerHook.foreach(p => p.after())

  })

  override def beforeRender(params: Dictionary[String]): Unit = {
    val oldChild = child

    params.getOrElse("action","_DEFAULT_ACTION_") match {
      case "boinc" => child = new BoincMainHostLayout(params)
      case "messages" => dom.window.alert("Not implemented")
      case "projects" => child = new BoincProjectLayout(params)
      case "tasks" => child = new BoincTaskLayout(params)
      case "tranfsers" => dom.window.alert("Not implemented")
      case "statistics" => dom.window.alert("Not implemented")
      case "disk" =>  dom.window.alert("Not implemented")
      case _ => {
        child = null

        if (oldChild == null)
          AppRouter.router.navigate(
            "/view/dashboard/" + params.get("client").get + "/boinc",
            absolute = true
          )
        else {

          oldChild match {
            case _: BoincMainHostLayout => AppRouter.router.navigate("/view/dashboard/" + params.get("client").get + "/boinc", absolute = true)
            case _: BoincProjectLayout => AppRouter.router.navigate("/view/dashboard/" + params.get("client").get + "/projects", absolute = true)
            case _: BoincTaskLayout => AppRouter.router.navigate("/view/dashboard/" + params.get("client").get + "/tasks", absolute = true)
            case _ => AppRouter.router.navigate("/view/dashboard/" + params.get("client").get + "/boinc", absolute = true)
          }

        }
      }
    }


    println("Child: " + child)
    if (child!=null) {
      if (oldChild != null && (child.getClass == oldChild.getClass)) {
        child.routerHook.foreach(p => p.already())
      } else {
        if (oldChild != null) oldChild.routerHook.foreach(p => p.leave())
        child.routerHook.foreach(p => p.before(() => {}))
      }
    }
  }

  override def onRender(): Unit = {
    LayoutManager.render(child)
  }
}
