package at.happywetter.boinc.web.pages
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.pages.boinc._
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.routes.AppRouter.{DashboardLocation, LoginPageLocation}
import mhtml.Var
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.08.2017
  */
object BoincLayout extends Layout {

  private val INITAL_STATE = "boinc"

  val child: Var[BoincPageLayout] = Var(null)
  private var currentState: String = "NONE"

  override val path: String = "/view/dashboard"

  private val component =
    <div class={BoincClientLayout.Style.content.htmlClass} id="client-data">
      {child.map( child => {
          if(child != null) child.render
          else {<div>Loading ...</div>}
        })
      }
    </div>

  override def already(): Unit = childRouteAction(_.already())
  override def before(done: js.Function0[Unit]): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    AuthClient.tryLogin.foreach {
      case true => done()
      case false => AppRouter.navigate(LoginPageLocation)
    }
  }
  override def leave(): Unit = childRouteAction(_.leave())
  override def after(): Unit = childRouteAction(_.after())

  override def beforeRender(params: Dictionary[String]): Unit = {
    val oldChild = getLayout

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
        childRouteAction(_.leave())

        child.update(_ => null)
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
      child.impure.run(_.already())
    } else {
      childRouteAction(_.leave())

      currentState = view
      boincPageLayout.before(() => {})
      child := boincPageLayout
    }
  }

  private[this] def getChildPath: String = getLayout.path
  private[this] def getLayout: BoincPageLayout = {
    var value: BoincPageLayout = null
    child.impure.run(layout => value = layout)

    value
  }

  private[this] def childRouteAction(f: (BoincPageLayout) => Unit): Unit = {
    var value: BoincPageLayout = null
    child.impure.run(layout => value = layout)

    if (value != null) f(value)
  }

  override def render: Elem = component
}
