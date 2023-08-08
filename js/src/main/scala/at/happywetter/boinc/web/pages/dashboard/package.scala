package at.happywetter.boinc.web.pages

import scala.util.Try

import at.happywetter.boinc.shared.boincrpc.{App, BoincState, Result, Workunit}
import at.happywetter.boinc.web.boincclient.BoincFormatter
import at.happywetter.boinc.web.boincclient.BoincFormatter.Implicits._
import at.happywetter.boinc.web.util.boincrpc.Implicits._

import mhtml.Var

/**
 * Created by: 
 *
 * @author Raphael
 * @version 22.11.2020
 */
package object dashboard:

  case class DetailData(client: String,
                        projects: Map[String, List[Result]] = Map().empty,
                        workunits: List[Workunit] = List(),
                        apps: Map[String, App] = Map().empty
  )

  case class HostData(cpu: String,
                      memory: String,
                      time: String,
                      deadline: String,
                      disk: String,
                      disk_max: String,
                      disk_value: String,
                      network: String
  )

  object HostData:
    import Ordering.Double.TotalOrdering

    def apply(state: BoincState, usedCpus: Int, upload: Double, download: Double, taskRuntime: Double): HostData =
      HostData(
        s"${usedCpus} / ${state.hostInfo.cpus}",
        s"${state.getCurrentUsedMemory.toSize} / ${state.hostInfo.memory.toSize}",
        if (usedCpus > 0) BoincFormatter.convertTime(taskRuntime / usedCpus) else BoincFormatter.convertTime(0d),
        Try(state.results.map(f => f.reportDeadline).min).getOrElse(-1d).toDate,
        s"%.1f %%".format((state.hostInfo.diskTotal - state.hostInfo.diskFree) / state.hostInfo.diskTotal * 100),
        state.hostInfo.diskTotal.toString,
        (state.hostInfo.diskTotal - state.hostInfo.diskFree).toString,
        upload.toSize + " / " + download.toSize
      )

  case class HostSumData(currentCPUs: Var[Int],
                         sumCPUs: Var[Int],
                         runtime: Var[Double],
                         networkUpload: Var[Double],
                         networkDownload: Var[Double]
  ):

    def clear(): Unit =
      currentCPUs := 0
      networkDownload := 0d
      networkUpload := 0d
      runtime := 0d
      sumCPUs := 0

  case class ClientData(name: String, data: Var[Option[Either[HostData, Exception]]], details: Var[Option[DetailData]])

  object ClientData:

    @inline
    def apply(name: String): ClientData = new ClientData(name, Var(None), Var(None))

    val Empty = new ClientData("", Var(None), Var(None))
