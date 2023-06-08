package at.happywetter.boinc.web.pages.dashboard

import at.happywetter.boinc.shared.boincrpc.Result
import at.happywetter.boinc.shared.rpc.DashboardDataEntry
import at.happywetter.boinc.web.boincclient.{BoincClient, ClientCacheHelper, ClientManager}
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.RichRx._
import mhtml.Var
import at.happywetter.boinc.web.util.boincrpc.Implicits._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Created by: 
 *
 * @author Raphael
 * @version 22.11.2020
 */
class DashboardDataModel:

  val clients: Var[Map[String, ClientData]] = Var(Map.empty[String, ClientData])
  val projects: Var[Map[String, String]] = Var(Map.empty[String, String])
  val clientsDataSum: HostSumData = HostSumData(Var(0), Var(0), Var(0d), Var(0d), Var(0d))

  def load(): Future[Unit] =
    ClientManager
      .readClients()
      .map(clients => {
        // DashboardMenuBuilder.renderClients(clients)
        this.clients := clients.map(name => (name, ClientData(name))).toMap
        this.clientsDataSum.clear()

        // Sequence futures for the project names, data is partially updated with Rx[...] and Var[...]
        // Future.sequence(
        clients
          .map(name => (name, ClientManager.clients(name)))
          .map { case (name, client) =>
            loadStateData(name, client).map { case (detailData, error) =>
              updateProjectNameCache(detailData.projects.keySet)
            }
          }
        // ).flatMap(details =>
        //  updateProjectNameCache(details.map(_._1).flatMap(_.projects.keySet).toSet)
        // )
      })

  private def loadStateData(name: String, client: BoincClient): Future[(DetailData, Either[HostData, Exception])] =
    client.getDashboardData
      .map { case DashboardDataEntry(state, fileTransfers) =>
        val clientData = this.clients.now(name)
        val usedCPUs = state.getCurrentUsedCPUs
        val taskRuntime = state.results.filter(_.state < 3).map(r => r.remainingCPU).sum

        val upload = fileTransfers.filter(p => p.xfer.isUpload).map(p => p.byte - p.fileXfer.bytesXfered).sum
        val download = fileTransfers.filter(p => !p.xfer.isUpload).map(p => p.byte - p.fileXfer.bytesXfered).sum

        clientsDataSum.currentCPUs.update(_ + usedCPUs)
        clientsDataSum.sumCPUs.update(_ + state.hostInfo.cpus)
        clientsDataSum.networkUpload.update(_ + upload)
        clientsDataSum.networkDownload.update(_ + download)

        if (usedCPUs > 0)
          clientsDataSum.runtime.update(_ + (taskRuntime / usedCPUs))

        ClientCacheHelper.updateCache(client.hostname, state)

        val hostData = HostData(state, usedCPUs, upload, download, taskRuntime)
        val details = DetailData(client.hostname, state.results.groupBy(f => f.project), state.workunits, state.apps)

        clientData.data := Some(Left(hostData))
        clientData.details := Some(details)

        (details, Left(hostData).asInstanceOf[Either[HostData, Exception]])
      }
      .recover:
        case e: Exception =>
          val clientData = this.clients.now(name)
          clientData.data := Some(Right(e))

          (DetailData(client.hostname), Right(e))

  protected def updateProjectNameCache(projects: Set[String]): Future[Unit] =
    ProjectNameCache
      .getAll(projects.toList)
      .map(projectNameData => projectNameData.map { case (url, maybeUrl) => (url, maybeUrl.getOrElse(url)) })
      .map(projectNameData => projectNameData.toMap)
      .map(projectNames => {
        this.projects.update(_ ++ projectNames)
      })
