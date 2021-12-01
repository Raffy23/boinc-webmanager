package at.happywetter.boinc.web.boincclient

import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.boincrpc.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared.boincrpc.{BoincCoreClient, BoincRPC}
import at.happywetter.boinc.shared.boincrpc._
import at.happywetter.boinc.shared.rpc.DashboardDataEntry
import org.scalajs.dom
import at.happywetter.boinc.web.facade.Implicits._
import at.happywetter.boinc.web.util.FetchHelper

import scala.concurrent.Future

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.07.2017
  */
class BoincClient(val hostname: String, val queryHealthyOnly: Boolean = false) extends WebmanagerClient[Future] {
  import at.happywetter.boinc.shared.parser._

  private val baseURI     = s"/api/boinc/${dom.window.encodeURIComponent(hostname)}/"
  private val healthyFlag = "?healthy"

  @inline def uri(cmd: BoincRPC.Command.Command): String =
    if(queryHealthyOnly) baseURI + cmd + healthyFlag
    else baseURI + cmd

  @inline def uri(cmd: BoincRPC.Command.Command, p: String) =
    if(queryHealthyOnly) baseURI + cmd + "/" + p + healthyFlag
    else baseURI + cmd + "/" + p

  @inline def uri(cmd: String): String =
    if(queryHealthyOnly) baseURI + cmd + healthyFlag
    else baseURI + cmd

  def getTasks(active: Boolean = true): Future[List[Result]] =
    FetchHelper.get[List[Result]](uri(if(active) "tasks" else "all_tasks"))

  override def getHostInfo: Future[HostInfo] =
    FetchHelper.get[HostInfo](uri(BoincRPC.Command.GetHostInfo))

  override def isNetworkAvailable: Future[Boolean] =
    FetchHelper.get[Boolean](uri(BoincRPC.Command.GetNetworkAvailable))

  override def getDiskUsage: Future[DiskUsage] =
    FetchHelper.get[DiskUsage](uri(BoincRPC.Command.GetDiskUsage))

  override def getProjects: Future[List[Project]] =
    FetchHelper.get[List[Project]](uri(BoincRPC.Command.GetProjectStatus))

  override def getState: Future[BoincState] =
    FetchHelper.get[BoincState](uri(BoincRPC.Command.GetState))

  override def getFileTransfer: Future[List[FileTransfer]] =
    FetchHelper.get[List[FileTransfer]](uri(BoincRPC.Command.GetFileTransfer))

  override def workunit(project: String, name: String, action: WorkunitAction): Future[Boolean] =
    FetchHelper.post[WorkunitRequestBody, Boolean](
      uri(BoincRPC.Command.GetActiveResults, name),
      WorkunitRequestBody(project, action.toString)
    )

  override def project(url: String, action: ProjectAction): Future[Boolean] =
    FetchHelper.patch[ProjectRequestBody, Boolean](
      uri(BoincRPC.Command.UpdateProject),
      ProjectRequestBody(url, action.toString)
    )

  override def getCCState: Future[CCState] =
    FetchHelper.get[CCState](uri(BoincRPC.Command.GetCCStatus))

  override def setCpu(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] =
    FetchHelper.post[BoincModeChange, Boolean](
      uri("cpu"),
      BoincModeChange(mode.toString, duration)
    )

  override def setGpu(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] =
    FetchHelper.post[BoincModeChange, Boolean](
      uri("gpu"),
      BoincModeChange(mode.toString, duration)
    )

  override def setNetwork(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] =
    FetchHelper.post[BoincModeChange, Boolean](
      uri("network"),
      BoincModeChange(mode.toString, duration)
    )

  override def getGlobalPrefsOverride: Future[GlobalPrefsOverride] =
    FetchHelper.get[GlobalPrefsOverride](uri(BoincRPC.Command.ReadGlobalPrefsOverride))

  override def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride): Future[Boolean] =
    FetchHelper.post[GlobalPrefsOverride, Boolean](
      uri(BoincRPC.Command.ReadGlobalPrefsOverride),
      globalPrefsOverride
    )

  override def setRun(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] =
    FetchHelper.post[BoincModeChange, Boolean](
      uri("run_mode"),
      BoincModeChange(mode.toString, duration)
    )

  // Needs to have WebRPC exposed to client ...
  override def attachProject(url: String, authenticator: String, name: String) = ???

  def attachProject(url: String, username: String, password: String, name: String): Future[Boolean] =
    FetchHelper.post[AddProjectBody, Boolean](
      uri(BoincRPC.Command.UpdateProject),
      AddProjectBody(url, name, username, password)
    )

  override def getStatistics: Future[Statistics] =
    FetchHelper.get[Statistics](uri(BoincRPC.Command.GetStatistics))

  override def getMessages(seqno: Int): Future[List[Message]] =
    FetchHelper.get[List[Message]](uri(BoincRPC.Command.GetMessages.toString + "?seqno=" + seqno))

  override def getNotices(seqno: Int): Future[List[Notice]] =
    FetchHelper.get[List[Notice]](uri(BoincRPC.Command.GetNotices.toString + "?seqno=" + seqno))

  override def readGlobalPrefsOverride: Future[Boolean] =
    FetchHelper.patch[Boolean](uri(BoincRPC.Command.ReadGlobalPrefsOverride))

  override def retryFileTransfer(project: String, file: String): Future[Boolean] =
    FetchHelper.post[RetryFileTransferBody, Boolean](
      uri(BoincRPC.Command.RetryFileTransfer),
      RetryFileTransferBody(project, file)
    )

  override def getVersion: Future[BoincVersion] =
    FetchHelper.get[BoincVersion](baseURI + "version")

  override def getDashboardData: Future[DashboardDataEntry] =
    FetchHelper.get[DashboardDataEntry](s"/api/webmanager/dashboard/${dom.window.encodeURIComponent(hostname)}")

  def asQueryOnlyHealthy(): BoincClient = new BoincClient(hostname, true)

  override def getAppConfig(url: String): Future[AppConfig] =
    FetchHelper.get[AppConfig](uri(s"app_config?url=${dom.window.encodeURIComponent(url)}"))

  override def setAppConfig(url: String, config: AppConfig): Future[Boolean] =
    FetchHelper.post[AppConfig, Boolean](uri(s"app_config?url=${dom.window.encodeURIComponent(url)}"), config)

  override def quit(): Future[Unit] = ???
}
