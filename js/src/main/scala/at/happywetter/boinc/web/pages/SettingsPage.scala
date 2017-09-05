package at.happywetter.boinc.web.pages
import at.happywetter.boinc.web.boincclient.{ClientManager, FetchResponseException}
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.routes.AppRouter.DashboardLocation
import at.happywetter.boinc.web.routes.{AppRouter, Hook, LayoutManager, NProgress}
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.09.2017
  */
object SettingsPage extends Layout {
  override val path: String = "settings"

  override val staticComponent: Option[JsDom.TypedTag[HTMLElement]] =  {
    import scalatags.JsDom.all._

    Some(
      div(
        DashboardMenu.component.render,
        div(id := "client-container", style := "margin-left:218px", "Settings ...."
        )
      )
    )
  }

  override val routerHook: Option[Hook] = Some(new Hook {
    override def already(): Unit = {
      LayoutManager.render(SettingsPage.this)
    }

    override def before(done: js.Function0[Unit]): Unit = {
      NProgress.done(true)
      done()
    }

    override def leave(): Unit = {}

    override def after(): Unit = {}
  })

  override def beforeRender(params: Dictionary[String]): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    ClientManager.readClients().map(clients => {
      DashboardMenu.removeMenuReferences("boinc-client-entry")
      clients.foreach(client =>
        DashboardMenu.addMenu(s"${AppRouter.href(DashboardLocation)}/$client",client, Some("boinc-client-entry"))
      )


      AppRouter.router.updatePageLinks()
    }).recover {
      case _: FetchResponseException =>
        import scalatags.JsDom.all._
        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }
  }
}
