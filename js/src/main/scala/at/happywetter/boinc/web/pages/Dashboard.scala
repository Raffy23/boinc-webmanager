package at.happywetter.boinc.web.pages

import at.happywetter.boinc.shared.Result
import at.happywetter.boinc.web.boincclient.{BoincFormater, ClientCacheHelper, ClientManager, FetchResponseException}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.pages.component.DashboardMenu
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.routes.AppRouter.{DashboardLocation, LoginPageLocation}
import at.happywetter.boinc.web.routes.{AppRouter, Hook, LayoutManager, NProgress}
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scalatags.JsDom
import at.happywetter.boinc.web.util.I18N._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 24.07.2017
  */
object Dashboard extends Layout {

  import scalacss.DevDefaults._
  object Style extends StyleSheet.Inline {
  }

  lazy val staticComponent: Option[JsDom.TypedTag[HTMLElement]] = {
    import scalatags.JsDom.all._

    Some(
    div(
      DashboardMenu.component.render,
      div(id := "client-container", style := "margin-left:218px"
      )
    )
    )
  }

  override val routerHook: Option[Hook] = Some(new Hook {
    override def already(): Unit = {
      LayoutManager.render(Dashboard.this)
    }

    override def before(done: js.Function0[Unit]): Unit = {
      import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

      AuthClient.tryLogin.foreach {
        case true => done()
        case false => AppRouter.navigate(LoginPageLocation)
      }
    }

    override def leave(): Unit = {}

    override def after(): Unit = {}
  })

  override def onRender(): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    ClientManager.readClients().map(clients => {

      DashboardMenu.removeMenuReferences("boinc-client-entry")
      clients.foreach(client =>
        DashboardMenu.addMenu(s"${AppRouter.href(DashboardLocation)}/$client",client, Some("boinc-client-entry"))
      )

      if (dom.window.location.pathname == "/view/dashboard")
        renderDashboardContent(clients)

      AppRouter.router.updatePageLinks()
      NProgress.done(true)
    }).recover {
      case _: FetchResponseException =>
        import scalatags.JsDom.all._
        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }
  }

  private def renderDashboardContent(clients: List[String]): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    val container = dom.document.getElementById("client-container")
    container.appendChild(
      div(
        h2(BoincClientLayout.Style.pageHeader, "dashboard_overview".localize),
        div(
          table(TableTheme.table,
            thead(tr(
              th("table_host".localize), th("table_cpu".localize), th("table_network".localize.toTags),
              th("table_computinduration".localize.toTags), th("table_wudeadline".localize), th("table_disk".localize)
            )),
            tbody(
              clients.map(c => ClientManager.clients(c)).map(client => {

                client.getState.map(state => {
                  dom.document.getElementById(s"dashboard-${client.hostname}-cpu").textContent =
                    s"${
                      state.results
                        .filter(p => p.activeTask.nonEmpty)
                        .filter(t => !t.supsended && Result.ActiveTaskState(t.activeTask.get.activeTaskState) == Result.ActiveTaskState.PROCESS_EXECUTING)
                        .map(p =>
                          state.workunits
                            .find(wu => wu.name == p.wuName)
                            .map(wu => state.apps(wu.appName))
                            .map(app => if (app.nonCpuIntensive) 0 else app.version.maxCpus.ceil.toInt)
                            .getOrElse(0)
                        ).sum
                    } / ${state.hostInfo.cpus}"

                  dom.document.getElementById(s"dashboard-${client.hostname}-time").textContent =
                    BoincFormater.convertTime(state.results.map(r => r.remainingCPU).sum / state.hostInfo.cpus)

                  dom.document.getElementById(s"dashboard-${client.hostname}-deadline").textContent =
                    BoincFormater.convertDate(state.results.map(f => f.reportDeadline).min)

                  val progressBar = dom.document.getElementById(s"dashboard-${client.hostname}-disk")
                  progressBar.setAttribute("value", state.hostInfo.diskFree.toString)
                  progressBar.setAttribute("max", state.hostInfo.diskTotal.toString)

                  progressBar.parentNode.appendChild(s"%.1f %%".format(state.hostInfo.diskFree/state.hostInfo.diskTotal*100).render)

                  ClientCacheHelper.updateCache(client.hostname, state)
                }).recover {
                  case _: FetchResponseException =>
                    val hField = dom.document.getElementById(s"dashboard-${client.hostname}-hostname").asInstanceOf[HTMLElement]
                    hField.textContent = client.hostname + " [Offline]"
                }

                client.getFileTransfer.foreach(transfers => {
                  val upload = transfers.filter(p => p.xfer.isUpload).map(p => p.byte - p.fileXfer.bytesXfered).sum
                  val download = transfers.filter(p => !p.xfer.isUpload).map(p => p.byte - p.fileXfer.bytesXfered).sum

                  dom.document.getElementById(s"dashboard-${client.hostname}-network").textContent =
                    BoincFormater.convertSize(upload) + " / " + BoincFormater.convertSize(download)
                }) // Fail silently ...

                tr(
                  td(id := s"dashboard-${client.hostname}-hostname", client.hostname),
                  td(style := "text-align:center;", id := s"dashboard-${client.hostname}-cpu", "-- / --"),
                  td(style := "text-align:center;", id := s"dashboard-${client.hostname}-network", "-- / --"),
                  td(style := "text-align:center;", id := s"dashboard-${client.hostname}-time", "--"),
                  td(style := "text-align:center;", id := s"dashboard-${client.hostname}-deadline", "--"),
                  td(style := "width: 240px", BoincClientLayout.Style.progressBar, JsDom.tags2.progress(style := "width:calc(100% - 5em);margin-right:20px", id := s"dashboard-${client.hostname}-disk")),
                )
              })
            )
          )
        )
      ).render
    )
  }

  override def beforeRender(params: Dictionary[String]): Unit = {}
}
