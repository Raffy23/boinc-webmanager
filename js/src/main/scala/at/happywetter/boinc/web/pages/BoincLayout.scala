package at.happywetter.boinc.web.pages
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.pages.boinc._
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.routes.AppRouter.{DashboardLocation, LoginPageLocation}
import at.happywetter.boinc.web.routes.{AppRouter, Hook, LayoutManager}
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.Dictionary
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

  override val path: String = "/view/dashboard"
  override val requestedParent = Some("main #client-container")
  override def requestParentLayout(): Some[Dashboard.type] = { Some(Dashboard) }

  override val staticComponent: Option[JsDom.TypedTag[HTMLElement]] =  {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    Some(div(BoincClientLayout.Style.content, id := "client-data"))
  }

  override val routerHook: Option[Hook] = Some(new Hook {
    override def already(): Unit = {
      child.routerHook.foreach(p => p.already())

      // Refresh Boinc page even it router hook is not set in client
      if (child.routerHook.isEmpty)
        LayoutManager.render(child)
    }

    override def before(done: js.Function0[Unit]): Unit = {
      import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

      AuthClient.tryLogin.foreach {
        case true => done()
        case false => AppRouter.navigate(LoginPageLocation)
      }
    }

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

    params.getOrElse("action","_DEFAULT_ACTION_") match {
      case view @ "boinc"      => generateChild(view, new BoincMainHostLayout(params))
      case view @ "messages"   => generateChild(view, new BoincMessageLayout(params))
      case view @ "projects"   => generateChild(view, new BoincProjectLayout(params))
      case view @ "tasks"      => generateChild(view, new BoincTaskLayout(params))
      case view @ "transfers"  => generateChild(view, new BoincFileTransferLayout(params))
      case view @ "statistics" => generateChild(view, new BoincStatisticsLayout(params))
      case view @ "disk"       => generateChild(view, new BoincDiskLayout(params))
      case view @ "global_prefs" => generateChild(view, new BoincGlobalPrefsLayout(params))

      case _ =>
        if (child != null)
          child.routerHook.foreach(p => p.leave())

        child = null
        currentState = "NONE"

        // Delay navigation, maybe we are currently in one ...
        dom.window.setTimeout(() => {
          oldChild match {
            case null => AppRouter.router.navigate(DashboardLocation.link + "/" + params.get("client").get + "/" + INITAL_STATE, absolute = true)
            case _ => AppRouter.router.navigate(DashboardLocation.link + "/" + params.get("client").get + "/" + oldChild.path, absolute = true)
          }
        }, 100)
    }

  }

  private[this] def generateChild(view: String, boincPageLayout: BoincPageLayout): Unit = {
    if (view == currentState) {
      child.routerHook.foreach(p => p.already())
    } else {
      if (child != null)
        child.routerHook.foreach(p => p.leave())

      currentState = view
      boincPageLayout.routerHook.foreach(p => p.before(() => {}))
      child = boincPageLayout
    }
  }

  override def onRender(): Unit = {
    if (child != null) LayoutManager.render(child)
  }

}
