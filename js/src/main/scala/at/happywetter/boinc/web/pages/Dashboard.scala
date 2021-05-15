package at.happywetter.boinc.web.pages

import at.happywetter.boinc.shared.boincrpc.Result
import at.happywetter.boinc.shared.util.StringLengthAlphaOrdering
import at.happywetter.boinc.web.boincclient._
import at.happywetter.boinc.web.css.definitions.components.{FloatingMenu, TableTheme}
import at.happywetter.boinc.web.css.definitions.pages.{BoincClientStyle, BoincProjectStyle}
import at.happywetter.boinc.web.css.definitions.{Misc => Style}
import at.happywetter.boinc.web.pages.boinc.BoincClientLayout
import at.happywetter.boinc.web.pages.component.{DashboardMenu, Tooltip}
import at.happywetter.boinc.web.pages.dashboard.{DashboardDataModel, HostData}
import at.happywetter.boinc.web.routes.{AppRouter, NProgress, Navigo}
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.web.util.RichRx._
import at.happywetter.boinc.web.util.XMLHelper._
import at.happywetter.boinc.web.util.{ErrorDialogUtil, WebSocketClient}
import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.{Elem, Node}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 24.07.2017
  */
object Dashboard extends Layout {

  override val path = "dashboard"

  private val clientData = new DashboardDataModel()

  override def already(): Unit = onRender()

  override def onRender(): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    NProgress.start()

    DashboardMenu.selectByMenuId("dashboard")
    if (!WebSocketClient.isOpend) {
      WebSocketClient.start()
    }

    /* "force" to reload cache, probably should be a little bit smarter than that ... */
    if (dom.window.sessionStorage.getItem("session_project_cache_populated") == null) {
      ClientManager.queryCompleteProjectList().map(data => {
        ProjectNameCache.saveAll(data.toList.map { case (url, project) => (url, project.name) })
        dom.window.sessionStorage.setItem("session_project_cache_populated", "true")
      })
    }

    clientData
      .load()
      .map(_ => NProgress.done(true))
      .recover(ErrorDialogUtil.showDialog)
  }

  override def render: Elem = {
    import at.happywetter.boinc.web.facade.NodeListConverter._
    import clientData.{clients, clientsDataSum, projects}

    val sortedProjects = projects.map(_.toSeq.sortBy(_._1)(ord = StringLengthAlphaOrdering))
    val sortedClients = clients.map(_.toSeq.sortBy(_._1)(ord = StringLengthAlphaOrdering))

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

          calculateOffsetOfWorkunitsTable()
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
              sortedClients.map(_.map(c => {
                implicit val data: Rx[Option[Either[HostData, Exception]]] = c._2.data
                val client = ClientManager.clients(c._1)

                <tr>
                  <td>{injectErrorTooltip(hostLink(client.hostname))}</td>
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
                {clientsDataSum.networkUpload.map(u => BoincFormatter.convertSize(u))} /
                {clientsDataSum.networkDownload.map(d => BoincFormatter.convertSize(d))}
              </td>
              <td class={Style.centeredText.htmlClass}>{clientsDataSum.runtime.map(r => BoincFormatter.convertTime(r))}</td>
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
                  sortedProjects.map(_.map(project => {
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
                sortedClients.zip(sortedProjects).map { case (clients, projects) =>
                  clients.map(client => {
                    <tr id={s"dashboard_${client._1}_details"}>
                      <td>{injectErrorTooltip(hostLink(client._1))(client._2.data)}</td>
                      {
                        projects.map(project => {
                          val rData = client._2.details.map(_.map(_.projects.getOrElse(project._1, List.empty)).getOrElse(List.empty)).now

                          val active = rData
                            .filter(p => p.activeTask.nonEmpty)
                            .count(t => !t.supsended && Result.ActiveTaskState(t.activeTask.get.activeTaskState) == Result.ActiveTaskState.PROCESS_EXECUTING)

                          val done = rData
                            .filter(p => p.activeTask.isEmpty)
                            .count(t => t.state >= 3)

                          val time = rData
                            .filter(r => !r.plan.contains("nci"))
                            .map(r => r.remainingCPU).sum / (if(active == 0) 1 else active)

                          <td>
                            {
                              if (rData.nonEmpty) {
                                Seq(
                                  <span>{done} / {rData.size}</span>,
                                  <br/>,
                                  <small>{if (time > 0) Some(BoincFormatter.convertTime(time)) else None}</small>
                                )
                              } else {
                                Seq("".toXML)
                              }
                           }
                          </td>
                      })
                    }
                    </tr>
                  })
                }
              }
              <tr>
                <td><b>{"sum".localize}</b></td>
                {
                  projects.zip(clients).map { case (projects, clients) =>
                    projects.map { case (projectUrl, _) =>
                      val data = clients.foldLeft((0, 0, 0D)) { case (x@(done, size, time), data) =>
                        data._2.details.now.map { details =>
                          val rData  = details.projects.getOrElse(projectUrl, List.empty)

                          val activeClient = rData
                            .filter(p => p.activeTask.nonEmpty)
                            .count(t => !t.supsended && Result.ActiveTaskState(t.activeTask.get.activeTaskState) == Result.ActiveTaskState.PROCESS_EXECUTING)

                          val doneClient = rData
                            .filter(p => p.activeTask.isEmpty)
                            .count(t => t.state >= 3)

                          val timeClient = rData
                            .filter(r => !r.plan.contains("nci"))
                            .map(r => r.remainingCPU).sum / (if(activeClient == 0) 1 else activeClient)

                          (done + doneClient, size + rData.length, time + timeClient)
                        }.getOrElse(x)
                      }

                      <td>
                        {
                          // println(projectUrl, data)
                          if (data._2 > 0) {
                            Seq(
                              <span>{data._1} / {data._2}</span>,
                              <br/>,
                              <small>{if (data._3 > 0) Some(BoincFormatter.convertTime(data._3)) else None}</small>
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

  private def injectErrorTooltip(name: Elem)(implicit data: Rx[Option[Either[HostData, Exception]]]): Rx[Seq[Node]] = {
    data.map { dataOption =>

      dataOption.map { data =>
        data.fold(
          _ => Seq(name),
          ex =>
            Seq(
              ex match {
                case _: FetchResponseException => Tooltip.warningTriangle("offline").toXML
                case _ => Tooltip.warningTriangle("error".localize).toXML
              },
              name
            )
        )
      }.getOrElse(
        Seq(
          Tooltip.loadingSpinner("loading").toXML,
          name
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

  override def beforeRender(params: Dictionary[String]): Unit = {}

  private def hostLink(hostname: String): Elem =
    <a href={BoincClientLayout.link(hostname)} onclick={AppRouter.onClick} class={BoincProjectStyle.link.htmlClass}>
      {hostname}
    </a>

  private def getNetwork(errorTooltip: Boolean = false, default: String = "-- / --")(implicit data: Rx[Option[Either[HostData, Exception]]]): Rx[Node] = {
    data.flatMap { dataOption =>
      dataOption.map(_.fold(
        hostData => Var(hostData.network.toXML),
        ex => { ex.printStackTrace(); Var(if(errorTooltip) "ERROR".toXML else "".toXML) }
      )).getOrElse(Var(default))
    }
  }

  private def calculateOffsetOfWorkunitsTable(): Unit = {
    import at.happywetter.boinc.web.facade.NodeListConverter._

    var max = 0D
    dom.document.querySelectorAll("#workunits_table_container > table > thead > tr span").forEach((node, _, _) => {
      val result = node.asInstanceOf[HTMLElement].offsetWidth * Math.cos(4.10152) * -1
      if (max < result)
        max = result
    })

    dom.document.getElementById("workunits_table_container").asInstanceOf[HTMLElement].style="margin-top:"+max+"px"
  }

}
