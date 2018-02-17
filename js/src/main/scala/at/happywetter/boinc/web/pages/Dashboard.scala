package at.happywetter.boinc.web.pages

import at.happywetter.boinc.shared.{Result, Workunit}
import at.happywetter.boinc.web.boincclient._
import at.happywetter.boinc.web.css.{FloatingMenu, TableTheme}
import at.happywetter.boinc.web.helper.AuthClient
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.XMLHelper._
import at.happywetter.boinc.web.pages.component.{DashboardMenu, Tooltip}
import at.happywetter.boinc.web.routes.AppRouter.LoginPageLocation
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.{DashboardMenuBuilder, ErrorDialogUtil}
import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.{Elem, Node, UnprefixedAttribute}
import scalacss.ProdDefaults._
import scalacss.internal.mutable.StyleSheet

/**
  * Created by: 
  *
  * @author Raphael
  * @version 24.07.2017
  */
object Dashboard extends Layout {

  override val path = "dashboard"

  object Style extends StyleSheet.Inline {
    import dsl._

    val centeredText: StyleA = style(
      textAlign.center
    )
  }

  override def before(done: js.Function0[Unit]): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    PageLayout.showMenu()

    AuthClient.tryLogin.foreach {
      case true => done()
      case false => AppRouter.navigate(LoginPageLocation)
    }
  }


  override def already(): Unit = onRender()

  override def onRender(): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    NProgress.start()

    ClientManager.readClients().map(clients => {
      DashboardMenuBuilder.renderClients(clients)
      this.clients := clients

      DashboardMenu.selectByReference("dashboard")
      AppRouter.router.updatePageLinks()

      Future.sequence(
        clients.map(c => ClientManager.clients(c)).map(client => fullyLoadData(client))
      ).foreach(details => {
        clientDetailData = details.map(data => (data._1.client, data._1)).toMap
        clientDetails := details.map(data => (data._1.client, data._2)).toMap

        val projects = details.map(_._1).flatMap(d => d.projects.keySet).toSet

        ProjectNameCache.getAll(projects.toList)
          .map( projectNameData => projectNameData.map{ case (url, maybeUrl) => (url, maybeUrl.getOrElse(url)) })
          .map( projectNameData => projectNameData.toMap)
          .foreach( projectNames => {
            val projects = projectNames.values.toList.sorted
            this.projects := projects

            NProgress.done(true)
          })
      })
    }).recover(ErrorDialogUtil.showDialog)
  }

  import at.happywetter.boinc.shared.App
  private case class DetailData(client: String, projects: Map[String, List[Result]] = Map().empty, workunits: List[Workunit] = List(), apps: Map[String, App] = Map().empty)
  private case class HostData(cpu: String, memory: String, time: String, deadline: String, disk: String, disk_max: String, disk_value: String, var network: String = "")

  private val clients = Var(List.empty[String])
  private val clientDetails = Var(Map.empty[String, Either[HostData, Exception]])
  private var clientDetailData = Map.empty[String, DetailData]
  private val projects = Var(List.empty[String])

  override def render: Elem = {
    import at.happywetter.boinc.web.hacks.NodeListConverter._

    <div>
      <div class={FloatingMenu.root.htmlClass}>
        <a class={FloatingMenu.active.htmlClass} onclick={(event: Event) => {
          event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
            node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
          })
          event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

          dom.window.document.getElementById("dashboard_home_table").asInstanceOf[HTMLElement].style=""
          dom.window.document.getElementById("dashboard_workunits_table").asInstanceOf[HTMLElement].style="display:none"
        }}>
          {"dashboard_home".localize}
        </a>
        <a class={FloatingMenu.active.htmlClass} onclick={(event: Event) => {
          event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
            node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
          })
          event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

          dom.window.document.getElementById("dashboard_home_table").asInstanceOf[HTMLElement].style="display:none"
          dom.window.document.getElementById("dashboard_workunits_table").asInstanceOf[HTMLElement].style=""

          calculateOffsetOfWoruntsTable()
        }}>
          {"dashboard_workunits".localize}
        </a>
      </div>

      <h2 class={BoincClientLayout.Style.pageHeader.htmlClass}>
        <i class="fa fa-tachometer"></i>
        {"dashboard_overview".localize}
      </h2>
      <div>
        <table class={TableTheme.table.htmlClass} id="dashboard_home_table">
          <thead>
            <tr>
              <th style="width:220px">{"table_host".localize}</th>
              <th>{"table_cpu".localize}</th>
              <th>{"table_memory".localize}</th>
              <th>{"table_network".localize.toTags}</th>
              <th>{"table_computinduration".localize.toTags}</th>
              <th>{"table_wudeadline".localize}</th>
              <th>{"table_disk".localize}</th>
            </tr>
          </thead>
          <tbody>
            {
              clients.map(_.map(implicit c => {
                val client = ClientManager.clients(c)

                <tr>
                  <td>{injectErrorTooltip(client.hostname)}</td>
                  <td class={Style.centeredText.htmlClass}>{getData(_.cpu) }</td>
                  <td class={Style.centeredText.htmlClass}>{getData(_.memory)}</td>
                  <td class={Style.centeredText.htmlClass}>{getData(_.network)}</td>
                  <td class={Style.centeredText.htmlClass}>{getData(_.time)}</td>
                  <td class={Style.centeredText.htmlClass}>{getData(_.deadline)}</td>
                  <td style="width:240px" class={BoincClientLayout.Style.progressBar.htmlClass}>
                    <progress style="width:calc(100% - 5em);margin-right:20px" value={getDataAttr(_.disk_value)} max={getDataAttr(_.disk_max)}>
                      <span>{getData(_.disk)}</span>
                    </progress>
                  </td>
                </tr>
              }))
            }
          </tbody>
        </table>
        <div id="workunits_table_container">
          <table class={Seq(TableTheme.table.htmlClass, TableTheme.no_border.htmlClass).mkString(" ")} style="display:none" id="dashboard_workunits_table">
            <thead>
              <tr id="dashbord_project_header">
                <th style="width:220px;text-align:left">{"table_host".localize}</th>
                {
                  projects.map(_.map(project => {
                    <th class={TableTheme.vertical_table_text.htmlClass}>
                      <div>
                        <span>{project}</span>
                      </div>
                    </th>
                  }))
                }
                <th></th>
              </tr>
            </thead>
            <tbody>
              {
                clients.map(_.map(client => {
                  <tr id={s"dashboard_${client}_details"}>
                    <td>{injectErrorTooltip(client)(client)}</td>
                    {
                      projects.map(_.map(project => {
                        val data = clientDetails.now(client)
                        if (data.isRight)
                          return <td></td>

                        val rData = clientDetailData(client).projects.getOrElse(project, List())
                        val active = rData
                          .filter(p => p.activeTask.nonEmpty)
                          .count(t => !t.supsended && Result.ActiveTaskState(t.activeTask.get.activeTaskState) == Result.ActiveTaskState.PROCESS_EXECUTING)

                        <td>
                          <span>{active} / {rData.size}</span>
                          <small>{BoincFormater.convertTime(rData.map(r => r.remainingCPU).sum / active)}</small>
                        </td>
                      }))
                    }
                  </tr>
                }))
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  }

  private def injectErrorTooltip(name: String)(implicit c: String): Rx[Seq[Node]] = {
    def buildTooltip(label: String, `class`: String = "fa fa-exclamation-triangle"): Node = {
      val tooltip = new Tooltip(
        Var(label.localize),
        <i class={`class`}></i>
      ).toXML.asInstanceOf[Elem]

      tooltip.copy(
        attributes1 = UnprefixedAttribute("style", "float:right;color:#FF8181", tooltip.attributes1)
      )
    }

    clientDetails.map(clientMap => {
      clientMap.get(c).map(data => {
        data.fold(
          _ => Seq(name.toXML),
          ex =>
            Seq(
            ex match {
              case _: FetchResponseException => buildTooltip("offline")
              case _ => buildTooltip("error")
            },
            name.toXML
          )
        )
      }).getOrElse(Seq(name))
    })
  }

  private def getDataAttr(f: (HostData) => String)(implicit c: String): Rx[Option[String]] = {
    clientDetails.map(clientMap =>
      clientMap.get(c).flatMap(data => data.swap.map(f).toOption)
    )
  }

  private def getData(f: (HostData) => String, errorTooltip: Boolean = false, default: String = "-- / --")(implicit c: String): Rx[Node] = {
    clientDetails.map(clientMap => {
      clientMap.get(c).map(data => {
        data.fold(
          hostData => f(hostData).toXML,
          ex => if(errorTooltip) "ERROR".toXML else "".toXML
        )
      }).getOrElse(default)
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

  private def fullyLoadData(client: BoincClient): Future[(DetailData, Either[HostData, Exception])] = {
    loadStateData(client).map(data => {
      loadFileTransferData(client, data._2)
      data
    })
  }

  private def loadStateData(client: BoincClient): Future[(DetailData, Either[HostData, Exception])] = {

    client.getState.map(state => {
      val details = DetailData(client.hostname, state.results.groupBy(f => f.project), state.workunits, state.apps)
      val hostData = HostData(
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
        } / ${state.hostInfo.cpus}",
        s"${BoincFormater.convertSize(
          state.results
            .filter(_.activeTask.nonEmpty)
            .map(_.activeTask.get)
            .map(_.workingSet)
            .sum)} / ${BoincFormater.convertSize(state.hostInfo.memory)}",
        BoincFormater.convertTime(state.results.map(r => r.remainingCPU).sum / state.hostInfo.cpus),
        BoincFormater.convertDate(state.results.map(f => f.reportDeadline).min),
        s"%.1f %%".format((state.hostInfo.diskTotal - state.hostInfo.diskFree)/state.hostInfo.diskTotal*100),
        state.hostInfo.diskTotal.toString,
        (state.hostInfo.diskTotal - state.hostInfo.diskFree).toString
      )

      ClientCacheHelper.updateCache(client.hostname, state)
      (details, Left(hostData).asInstanceOf[Either[HostData, Exception]])
    }).recover {
      case e: Exception =>
        e.printStackTrace()
        (DetailData(client.hostname), Right(e))
    }
  }

  private def loadFileTransferData(client: BoincClient, hostData: Either[HostData, Exception]): Unit = {
    if (hostData.isLeft) {
      client.getFileTransfer.foreach(transfers => {
        val upload = transfers.filter(p => p.xfer.isUpload).map(p => p.byte - p.fileXfer.bytesXfered).sum
        val download = transfers.filter(p => !p.xfer.isUpload).map(p => p.byte - p.fileXfer.bytesXfered).sum

        hostData.left.get.network =
          BoincFormater.convertSpeed(upload) + " / " + BoincFormater.convertSpeed(download)
      })
    }
  }

  override def beforeRender(params: Dictionary[String]): Unit = {}
}
