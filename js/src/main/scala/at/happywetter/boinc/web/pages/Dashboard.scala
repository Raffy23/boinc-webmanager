package at.happywetter.boinc.web.pages

import at.happywetter.boinc.shared.{Result, Workunit}
import at.happywetter.boinc.web.boincclient._
import at.happywetter.boinc.web.css.{FloatingMenu, TableTheme}
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.pages.boinc.BoincStatisticsLayout
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.component.{DashboardMenu, Tooltip}
import at.happywetter.boinc.web.routes.AppRouter.{DashboardLocation, LoginPageLocation}
import at.happywetter.boinc.web.routes.{AppRouter, Hook, LayoutManager, NProgress}
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scalatags.JsDom
/**
  * Created by: 
  *
  * @author Raphael
  * @version 24.07.2017
  */
object Dashboard extends Layout {

  lazy val staticComponent: Option[JsDom.TypedTag[HTMLElement]] = {
    import scalatags.JsDom.all._
    import scalacss.ScalatagsCss._

    Some(
    div(
      DashboardMenu.component.render,
      div(id := "client-container", PageLayout.Style.clientContainer)
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

      if (dom.window.location.pathname == DashboardLocation.link) {
        renderDashboardContent(clients)
        DashboardMenu.selectByReference("dashboard")
      }

      AppRouter.router.updatePageLinks()
    }).recover {
      case _: FetchResponseException =>
        import scalatags.JsDom.all._
        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }
  }

  private def renderDashboardContent(clients: List[String]): Unit = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._
    import at.happywetter.boinc.web.hacks.NodeListConverter._

    val container = dom.document.getElementById("client-container")
    container.appendChild(
      div(
        div(FloatingMenu.root,
          a("dashboard_home".localize, FloatingMenu.active,
            onclick := { (event: Event) => {
              event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
                node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
              })
              event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

              dom.window.document.getElementById("dashboard_home_table").asInstanceOf[HTMLElement].style=""
              dom.window.document.getElementById("dashboard_workunits_table").asInstanceOf[HTMLElement].style="display:none"
            }}),
          a("dashboard_workunits".localize, onclick := { (event: Event) => {
            event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
              node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
            })
            event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

            dom.window.document.getElementById("dashboard_home_table").asInstanceOf[HTMLElement].style="display:none"
            dom.window.document.getElementById("dashboard_workunits_table").asInstanceOf[HTMLElement].style=""

            calculateOffsetOfWoruntsTable()
          }})
        ),
        h2(BoincClientLayout.Style.pageHeader, i(`class` := "fa fa-tachometer"), "dashboard_overview".localize),
        div(
          table(TableTheme.table, id := "dashboard_home_table",
            thead(tr(
              th("table_host".localize, style := "width:220px;"), th("table_cpu".localize), th("table_memory".localize),
              th("table_network".localize.toTags), th("table_computinduration".localize.toTags),
              th("table_wudeadline".localize), th("table_disk".localize)
            )),
            tbody(
              clients.map(c => ClientManager.clients(c)).map(client => {
                tr(
                  td(id := s"dashboard-${client.hostname}-hostname", client.hostname),
                  td(style := "text-align:center;", id := s"dashboard-${client.hostname}-cpu", "-- / --"),
                  td(style := "text-align:center;", id := s"dashboard-${client.hostname}-memory", "--"),
                  td(style := "text-align:center;", id := s"dashboard-${client.hostname}-network", "-- / --"),
                  td(style := "text-align:center;", id := s"dashboard-${client.hostname}-time", "--"),
                  td(style := "text-align:center;", id := s"dashboard-${client.hostname}-deadline", "--"),
                  td(style := "width: 240px", BoincClientLayout.Style.progressBar, JsDom.tags2.progress(style := "width:calc(100% - 5em);margin-right:20px", id := s"dashboard-${client.hostname}-disk")),
                )
              })
            )
          ),
          div( id := "workunits_table_container",
            table(TableTheme.table, TableTheme.no_border, style := "display:none", id := "dashboard_workunits_table",
              thead(
                tr( id := "dashbord_project_header",
                  th("table_host".localize, style := "width:220px;text-align:left")
                )
              ),
              tbody(
                clients.map(client => {
                  tr( id := s"dashboard_${client}_details",
                    td(client, id := s"dashboard_${client}_details_hostname")
                  )
                })
              )
            )
          )
        )
      ).render
    )

    Future.sequence(
      clients.map(c => ClientManager.clients(c)).map(client => {
        NProgress.start()
        loadFileTransferData(client)
        loadStateData(client)
      })
    ).foreach(details => {
      val tableHeader = dom.document.getElementById("dashbord_project_header")
      val projects = details.flatMap(d => d.projects.keySet).toSet
      val clients = details.map(d => d.client)

      ProjectNameCache.getAll(projects.toList)
        .map( projectNameData => projectNameData.map{ case (url, maybeUrl) => (url, maybeUrl.getOrElse(url)) })
        .map( projectNameData => projectNameData.toMap)
        .foreach( projectNames => {

          projects.foreach(project => {
            tableHeader.appendChild(
              th(TableTheme.vertical_table_text,
                div(
                  span(
                    projectNames(project)
                  )
                )
              ).render
            )

            clients.foreach(client => {
              val data = details.find(_.client == client).get
              val rData = data.projects.getOrElse(project, List())

              val active = rData
                  .filter(p => p.activeTask.nonEmpty)
                  .count(t => !t.supsended && Result.ActiveTaskState(t.activeTask.get.activeTaskState) == Result.ActiveTaskState.PROCESS_EXECUTING)

              dom.document.getElementById(s"dashboard_${client}_details").appendChild(
                if (rData.nonEmpty)
                  td(
                    span(s"$active / ${rData.size}"), br(),
                    small(BoincFormater.convertTime(rData.map(r => r.remainingCPU).sum / active))
                  ).render
                else
                  td().render
              )
            })
          })

          tableHeader.appendChild(th().render)
          NProgress.done(true)
        })
    })

  }

  private def calculateOffsetOfWoruntsTable(): Unit = {
    import at.happywetter.boinc.web.hacks.NodeListConverter._

    var max = 0D
    dom.document.querySelectorAll("#workunits_table_container > table > thead > tr span").forEach((node, _, _) => {
      val result = node.asInstanceOf[HTMLElement].offsetWidth * Math.cos(4.10152) * -1
      if (max < result)
        max = result
    })

    dom.document.getElementById("workunits_table_container").asInstanceOf[HTMLElement].style="margin-top:"+max+"px"
  }

  private def loadStateData(client: BoincClient): Future[DetailData] = {
    import scalatags.JsDom.all._

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

      dom.document.getElementById(s"dashboard-${client.hostname}-memory").textContent = s"" +
        s"${BoincFormater.convertSize(
          state.results
            .filter(_.activeTask.nonEmpty)
            .map(_.activeTask.get)
            .map(_.workingSet)
            .sum)} / ${BoincFormater.convertSize(state.hostInfo.memory)}"


      dom.document.getElementById(s"dashboard-${client.hostname}-time").textContent =
        BoincFormater.convertTime(state.results.map(r => r.remainingCPU).sum / state.hostInfo.cpus)

      dom.document.getElementById(s"dashboard-${client.hostname}-deadline").textContent =
        BoincFormater.convertDate(state.results.map(f => f.reportDeadline).min)

      val progressBar = dom.document.getElementById(s"dashboard-${client.hostname}-disk")
      progressBar.setAttribute("value", state.hostInfo.diskFree.toString)
      progressBar.setAttribute("max", state.hostInfo.diskTotal.toString)

      progressBar.parentNode.appendChild(s"%.1f %%".format(state.hostInfo.diskFree/state.hostInfo.diskTotal*100).render)

      ClientCacheHelper.updateCache(client.hostname, state)
      DetailData(client.hostname, state.results.groupBy(f => f.project), state.workunits, state.apps)
    }).recover {
      case _: FetchResponseException =>
        val hField = dom.document.getElementById(s"dashboard-${client.hostname}-hostname").asInstanceOf[HTMLElement]
        val dField = dom.document.getElementById(s"dashboard_${client.hostname}_details_hostname").asInstanceOf[HTMLElement]

        val tooltip = new Tooltip("Offline", i(`class` := "fa fa-exclamation-triangle")).render()
        tooltip.style = "float:right;color:#FF8181"

        val tooltip2 = new Tooltip("Offline", i(`class` := "fa fa-exclamation-triangle")).render()
        tooltip2.style = "float:right;color:#FF8181"

        hField.appendChild(tooltip)
        dField.appendChild(tooltip2)

        DetailData(client.hostname)
      case _ =>  DetailData(client.hostname)
    }
  }

  private def loadFileTransferData(client: BoincClient): Unit = {
    client.getFileTransfer.foreach(transfers => {
      val upload = transfers.filter(p => p.xfer.isUpload).map(p => p.byte - p.fileXfer.bytesXfered).sum
      val download = transfers.filter(p => !p.xfer.isUpload).map(p => p.byte - p.fileXfer.bytesXfered).sum

      dom.document.getElementById(s"dashboard-${client.hostname}-network").textContent =
        BoincFormater.convertSize(upload) + " / " + BoincFormater.convertSize(download)
    }) // Fail silently ...
  }


  override def beforeRender(params: Dictionary[String]): Unit = {}

  override val path = "dashboard"

  import at.happywetter.boinc.shared.App
  case class DetailData(client: String, projects: Map[String, List[Result]] = Map().empty, workunits: List[Workunit] = List(), apps: Map[String, App] = Map().empty)
}
