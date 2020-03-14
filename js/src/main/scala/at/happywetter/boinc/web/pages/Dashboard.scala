package at.happywetter.boinc.web.pages

import at.happywetter.boinc.shared.boincrpc.{Result, Workunit}
import at.happywetter.boinc.web.boincclient._
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.helper.{AuthClient, WebSocketClient}
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.XMLHelper._
import at.happywetter.boinc.web.pages.boinc.BoincClientLayout
import at.happywetter.boinc.web.pages.component.{DashboardMenu, DataTable, Tooltip}
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
import scala.xml.{Elem, Node}
import at.happywetter.boinc.web.css.definitions.{Misc => Style}

import scala.util.Try
import BoincFormater.Implicits._
import at.happywetter.boinc.shared.util.StringLengthAlphaOrdering
import at.happywetter.boinc.web.css.definitions.components.{FloatingMenu, TableTheme}
import at.happywetter.boinc.web.helper.table.StringTableRow

import Ordering.Double.TotalOrdering
import scala.collection.mutable

/**
  * Created by: 
  *
  * @author Raphael
  * @version 24.07.2017
  */
object Dashboard extends Layout {

  override val path = "dashboard"

  import at.happywetter.boinc.shared.boincrpc.App
  private case class DetailData(client: String, projects: Map[String, List[Result]] = Map().empty,
                                workunits: List[Workunit] = List(), apps: Map[String, App] = Map().empty)

  private case class HostData(cpu: String, memory: String, time: String, deadline: String,
                              disk: String, disk_max: String, disk_value: String,
                              network: Future[String])

  private case class HostSumData(currentCPUs: Var[Int], sumCPUs: Var[Int], runtime: Var[Double],
                                 networkUpload: Var[Double], networkDownload: Var[Double])

  private case class ClientData(name: String, data: Var[Option[Either[HostData, Exception]]], details: Var[Option[DetailData]])
  private object ClientData {

    @inline
    def apply(name: String): ClientData = new ClientData(name, Var(None), Var(None))

    val Empty = new ClientData("", Var(None), Var(None))
  }

  private val clients = Var(Map.empty[String, ClientData])
  private val projects = Var(Map.empty[String, String])
  private val clientsDataSum = HostSumData(Var(0),Var(0),Var(0D),Var(0D),Var(0D))

  override def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit = {
    PageLayout.clearNav()
    PageLayout.showMenu()
    PageLayout.clearNav()

    AuthClient.validateAction(done)
  }

  override def already(): Unit = onRender()

  override def onRender(): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    NProgress.start()

    DashboardMenu.selectByMenuId("dashboard")
    if (!WebSocketClient.isOpend) {
      WebSocketClient.start()
    }

    ClientManager.readClients().map(clients => {
      DashboardMenuBuilder.renderClients(clients)
      this.clients := clients.map(name => (name, ClientData(name))).toMap

      // Sequence futures for the project names, data is partially updated with Rx[...] and Var[...]
      Future.sequence(
        clients
          .map(name => (name, ClientManager.clients(name)))
          .map{ case (name, client) =>
            val future = fullyLoadData(name, client)

            future
          }
      ).foreach(details => {
        val projects = details.map(_._1).flatMap(d => d.projects.keySet).toSet

        ProjectNameCache.getAll(projects.toList)
          .map( projectNameData => projectNameData.map{ case (url, maybeUrl) => (url, maybeUrl.getOrElse(url)) })
          .map( projectNameData => projectNameData.toMap)
          .map( projectNames => {
            this.projects := projectNames
            NProgress.done(true)
          })
      })
    }).recover(ErrorDialogUtil.showDialog)
  }



  override def render: Elem = {
    import at.happywetter.boinc.web.hacks.NodeListConverter._

    <div>
      <div class={FloatingMenu.root.htmlClass}>
        <a class={FloatingMenu.active.htmlClass} onclick={(event: Event) => {
          event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
            if (!js.isUndefined(node.asInstanceOf[HTMLElement].classList))
              node.asInstanceOf[HTMLElement].classList.remove(FloatingMenu.active.htmlClass)
          })
          event.target.asInstanceOf[HTMLElement].classList.add(FloatingMenu.active.htmlClass)

          dom.window.document.getElementById("dashboard_home_table").asInstanceOf[HTMLElement].style=""
          dom.window.document.getElementById("dashboard_workunits_table").asInstanceOf[HTMLElement].style="display:none"
        }}>
          {"dashboard_home".localize}
        </a>
        <a onclick={(event: Event) => {
          event.target.asInstanceOf[HTMLElement].parentNode.childNodes.forEach((node,_,_) => {
            if (!js.isUndefined(node.asInstanceOf[HTMLElement].classList))
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

      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-tachometer-alt" aria-hidden="true"></i>
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
              clients.map(_.toList.sortBy(_._1)(ord = StringLengthAlphaOrdering).map(c => {
                implicit val data: Rx[Option[Either[HostData, Exception]]] = c._2.data
                val client = ClientManager.clients(c._1)

                <tr>
                  <td>{injectErrorTooltip(client.hostname)}</td>
                  <td class={Style.centeredText.htmlClass}>{getData(_.cpu) }</td>
                  <td class={Style.centeredText.htmlClass}>{getData(_.memory)}</td>
                  <td class={Style.centeredText.htmlClass}>{getNetwork()}</td>
                  <td class={Style.centeredText.htmlClass}>{getData(_.time)}</td>
                  <td class={Style.centeredText.htmlClass}>{getData(_.deadline)}</td>
                  <td style="width:240px" class={BoincClientStyle.progressBar.htmlClass}>
                    <progress style="width:calc(100% - 5em);margin-right:20px" value={getDataAttr(_.disk_value)} max={getDataAttr(_.disk_max)}/>
                    <span>{getData(_.disk)}</span>
                  </td>
                </tr>
              }).toSeq)
            }
            <tr>
              <td><b>{"sum".localize}</b></td>
              <td class={Style.centeredText.htmlClass}>{clientsDataSum.currentCPUs.map(c => s"$c / ${clientsDataSum.sumCPUs.now}")}</td>
              <td class={Style.centeredText.htmlClass}></td>
              <td class={Style.centeredText.htmlClass}>
                {clientsDataSum.networkUpload.map(u => BoincFormater.convertSize(u))} /
                {clientsDataSum.networkDownload.map(d => BoincFormater.convertSize(d))}
              </td>
              <td class={Style.centeredText.htmlClass}>{clientsDataSum.runtime.map(r => BoincFormater.convertTime(r))}</td>
              <td class={Style.centeredText.htmlClass}></td>
              <td class={Style.centeredText.htmlClass}></td>
            </tr>
          </tbody>
        </table>
        <div id="workunits_table_container">
          <table class={Seq(TableTheme.table.htmlClass, TableTheme.noBorder.htmlClass).mkString(" ")} style="display:none" id="dashboard_workunits_table">
            <thead>
              <tr id="dashbord_project_header">
                <th style="width:220px;text-align:left">{"table_host".localize}</th>
                {
                  projects.map(_.map(project => {
                    <th class={TableTheme.verticalText.htmlClass}>
                      <div>
                        <span>{project._2}</span>
                      </div>
                    </th>
                  }).toList)
                }
                <th></th>
              </tr>
            </thead>
            <tbody>
              {
                clients.map(_.toList.sortBy(_._1)(ord = StringLengthAlphaOrdering).map(client => {
                  <tr id={s"dashboard_${client._1}_details"}>
                    <td>{injectErrorTooltip(client._1)(client._2.data)}</td>
                    {
                      projects.map(_.map(project => {
                        val rData = client._2.details.map(_.map(_.projects.getOrElse(project._1, List.empty)).getOrElse(List.empty)).now

                        val active = rData
                          .filter(p => p.activeTask.nonEmpty)
                          .count(t => !t.supsended && Result.ActiveTaskState(t.activeTask.get.activeTaskState) == Result.ActiveTaskState.PROCESS_EXECUTING)

                        val time = rData
                          .filter(r => !r.plan.contains("nci"))
                          .map(r => r.remainingCPU).sum / (if(active == 0) 1 else active)

                        <td>
                          {
                            if (rData.nonEmpty) {
                              Seq(
                                <span>{active} / {rData.size}</span>,
                                <br/>,
                                <small>{if (time > 0) Some(BoincFormater.convertTime(time)) else None}</small>
                              )
                            } else {
                              Seq("".toXML)
                            }
                         }
                        </td>
                      }).toList)
                    }
                  </tr>
                }).toSeq)
              }
              <tr>
                <td><b>{"sum".localize}</b></td>
                {
                  projects.zip(clients).map { case (projects, clients) =>
                    projects.map { case (projectUrl, _) =>
                      val data = clients.foldLeft((0, 0, 0D)) { case (x@(active, size, time), data) =>
                        data._2.details.now.map { details =>
                          val rData  = details.projects.getOrElse(projectUrl, List.empty)

                          val activeClient = rData
                            .filter(p => p.activeTask.nonEmpty)
                            .count(t => !t.supsended && Result.ActiveTaskState(t.activeTask.get.activeTaskState) == Result.ActiveTaskState.PROCESS_EXECUTING)

                          val timeClient = rData
                            .filter(r => !r.plan.contains("nci"))
                            .map(r => r.remainingCPU).sum / (if(active == 0) 1 else active)

                          (active + activeClient, size + rData.length, time + timeClient)
                        }.getOrElse(x)
                      }

                      <td>
                        {
                          println(projectUrl, data)
                          if (data._2 > 0) {
                            Seq(
                              <span>{data._1} / {data._2}</span>,
                              <br/>,
                              <small>{if (data._3 > 0) Some(BoincFormater.convertTime(data._3)) else None}</small>
                            )
                          } else {
                            Seq("".toXML)
                          }
                        }
                      </td>
                    }.toSeq
                  }
                }
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  }

  private def injectErrorTooltip(name: String)(implicit data: Rx[Option[Either[HostData, Exception]]]): Rx[Seq[Node]] = {
    data.map { dataOption =>

      dataOption.map { data =>
        data.fold(
          _ => Seq(name.toXML),
          ex =>
            Seq(
              ex match {
                case _: FetchResponseException => Tooltip.warningTriangle("offline").toXML
                case _ => Tooltip.warningTriangle("error".localize).toXML
              },
              name.toXML
            )
        )
      }.getOrElse(
        Seq(
          Tooltip.loadingSpinner("loading").toXML,
          name.toXML
        )
      )
    }
  }

  @inline
  private def getDataAttr(f: HostData => String)(implicit data: Rx[Option[Either[HostData, Exception]]]): Rx[Option[String]] = {
    data.map(_.flatMap(_.swap.map(f).toOption))
  }

  private def getData(f: HostData => String, errorTooltip: Boolean = false, default: String = "-- / --")(implicit data: Rx[Option[Either[HostData, Exception]]]): Rx[Node] = {
    data.map { dataOption =>
      dataOption.map(_.fold(
        hostData => f(hostData).toXML,
        ex => { ex.printStackTrace(); if(errorTooltip) "ERROR".toXML else "".toXML }
      )).getOrElse(default)
    }
  }

  private def getNetwork(errorTooltip: Boolean = false, default: String = "-- / --")(implicit data: Rx[Option[Either[HostData, Exception]]]): Rx[Node] = {
    data.flatMap { dataOption =>
      dataOption.map(_.fold(
        hostData => hostData.network.map(_.toXML).toRx(default),
        ex => { ex.printStackTrace(); Var(if(errorTooltip) "ERROR".toXML else "".toXML) }
      )).getOrElse(Var(default))
    }
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

  private def fullyLoadData(name: String, client: BoincClient): Future[(DetailData, Either[HostData, Exception])] = {
    loadStateData(name, client)
  }

  private def loadStateData(name: String, client: BoincClient): Future[(DetailData, Either[HostData, Exception])] = {
    clientsDataSum.currentCPUs := 0
    clientsDataSum.networkDownload := 0D
    clientsDataSum.networkUpload := 0D
    clientsDataSum.runtime := 0D
    clientsDataSum.sumCPUs := 0

    client.getState.map(state => {
      val details = DetailData(client.hostname, state.results.groupBy(f => f.project), state.workunits, state.apps)
      var cpus = 0
      val taskRuntime = state.results.map(r => r.remainingCPU).sum
      val hostData = HostData(
        s"${
          cpus = state.results
            .filter(p => p.activeTask.nonEmpty)
            .filter(t => !t.supsended && Result.ActiveTaskState(t.activeTask.get.activeTaskState) == Result.ActiveTaskState.PROCESS_EXECUTING)
            .map(p =>
              state.workunits
                .find(wu => wu.name == p.wuName)
                .flatMap(wu => state.apps.get(wu.appName))
                .map(app => if (app.nonCpuIntensive) 0 else app.version.maxCpus.ceil.toInt)
                .getOrElse(0)
            ).sum

          cpus
        } / ${state.hostInfo.cpus}",
        s"${
          state.results
            .filter(_.activeTask.nonEmpty)
            .map(_.activeTask.get)
            .map(_.workingSet)
            .sum
            .toSize} / ${state.hostInfo.memory.toSize}",
        if (cpus > 0) BoincFormater.convertTime(taskRuntime / cpus) else BoincFormater.convertTime(0D),
        Try(state.results.map(f => f.reportDeadline).min).getOrElse(-1D).toDate,
        s"%.1f %%".format((state.hostInfo.diskTotal - state.hostInfo.diskFree)/state.hostInfo.diskTotal*100),
        state.hostInfo.diskTotal.toString,
        (state.hostInfo.diskTotal - state.hostInfo.diskFree).toString,
        loadFileTransferData(client)
      )

      clientsDataSum.currentCPUs.update(_ + cpus)
      clientsDataSum.sumCPUs.update(_ + state.hostInfo.cpus)
      if (cpus > 0) clientsDataSum.runtime.update(_ + (taskRuntime / cpus))

      ClientCacheHelper.updateCache(client.hostname, state)
      this.clients.map { clients =>
        val entry = clients(name)
        entry.data := Some(Left(hostData))
        entry.details := Some(details)
      }.now

      (details, Left(hostData).asInstanceOf[Either[HostData, Exception]])
    }).recover {
      case e: Exception =>
        e.printStackTrace()
        this.clients.map { clients =>
          val entry = clients(name)
          entry.data := Some(Right(e))
        }.now

        (DetailData(client.hostname), Right(e))
    }
  }

  private def loadFileTransferData(client: BoincClient): Future[String] = {
    client.getFileTransfer.map(transfers => {
      val upload = transfers.filter(p => p.xfer.isUpload).map(p => p.byte - p.fileXfer.bytesXfered).sum
      val download = transfers.filter(p => !p.xfer.isUpload).map(p => p.byte - p.fileXfer.bytesXfered).sum

      clientsDataSum.networkUpload.update(_ + upload)
      clientsDataSum.networkDownload.update(_ + download)

      upload.toSize + " / " + download.toSize
    })
  }

  override def beforeRender(params: Dictionary[String]): Unit = {}
}
