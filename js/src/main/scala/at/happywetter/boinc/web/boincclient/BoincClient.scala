package at.happywetter.boinc.web.boincclient

import at.happywetter.boinc.shared.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared.{BoincRPC, _}
import at.happywetter.boinc.web.helper.FetchHelper
import at.happywetter.boinc.web.helper.ResponseHelper._
import org.scalajs.dom.experimental.{Fetch, HttpMethod, RequestInit}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.07.2017
  */
class BoincClient(val hostname: String) extends BoincCoreClient {

  private val baseURI = "/api/boinc/" + hostname + "/"
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

  def getTasks(active: Boolean = true): Future[List[Result]] = {
    val uri = if(active) "tasks" else "all_tasks"

    Fetch.fetch(baseURI + uri, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[List[Result]](data).toOption.get)
  }

  def getHostInfo: Future[HostInfo] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetHostInfo, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[HostInfo](data).toOption.get)
  }

  def isNetworkAvailable: Future[Boolean] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetNetworkAvailable, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[Boolean](data).toOption.get)
  }

  def getDiskUsage: Future[DiskUsage] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetDiskUsage, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[DiskUsage](data).toOption.get)
  }

  override def getProjects: Future[List[Project]] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetProjectStatus, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[List[Project]](data).toOption.get)
  }

  override def getState: Future[BoincState] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetState, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[BoincState](data).toOption.get)
  }

  override def getFileTransfer: Future[List[FileTransfer]] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetFileTransfer, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[List[FileTransfer]](data).toOption.get)
  }

  override def workunit(project: String, name: String, action: WorkunitAction): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + BoincRPC.Command.GetActiveResults + "/" + name,
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = WorkunitRequestBody(project, action.toString).asJson.noSpaces
        )
      )
      .mapData(data => decode[Boolean](data).toOption.get)
  }

  override def project(name: String, action: ProjectAction): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + BoincRPC.Command.GetProjectStatus,
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = ProjectRequestBody(name, action.toString).asJson.noSpaces
        )
      )
      .mapData(data => decode[Boolean](data).toOption.get)
  }

  override def getCCState: Future[CCState] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetCCStatus, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[CCState](data).toOption.get)
  }

  override def setCpu(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + "cpu",
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = BoincModeChange(mode.toString, duration).asJson.noSpaces
        )
      )
      .mapData(data => decode[Boolean](data).toOption.get)
  }

  override def setGpu(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + "gpu",
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = BoincModeChange(mode.toString, duration).asJson.noSpaces
        )
      )
      .mapData(data => decode[Boolean](data).toOption.get)
  }

  override def setNetwork(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + "network",
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = BoincModeChange(mode.toString, duration).asJson.noSpaces
        )
      )
      .mapData(data => decode[Boolean](data).toOption.get)
  }

  override def getGlobalPrefsOverride: Future[GlobalPrefsOverride] = {
    Fetch
      .fetch(baseURI + BoincRPC.Command.ReadGlobalPrefsOverride, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .mapData(data => decode[GlobalPrefsOverride](data).toOption.get)
  }

  override def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + BoincRPC.Command.ReadGlobalPrefsOverride,
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = globalPrefsOverride.asJson.noSpaces
        )
      )
      .mapData(data => decode[Boolean](data).toOption.get)
  }

  override def setRun(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + "run_mode",
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = BoincModeChange(mode.toString, duration).asJson.noSpaces
        )
      )
      .mapData(data => decode[Boolean](data).toOption.get)
  }

  // Needs to have WebRPC exposed to client ...
  override def attachProject(url: String, authenticator: String, name: String) = ???

  def attachProject(url: String, username: String, password: String, name: String): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + "project",
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = AddProjectBody(url, name, username, password).asJson.noSpaces
        )
      )
      .mapData(data => decode[Boolean](data).toOption.get)
  }

  override def getStatistics: Future[Statistics] = {
    Fetch
      .fetch(
        baseURI + BoincRPC.Command.GetStatistics,
        RequestInit(
          method = HttpMethod.GET,
          headers = FetchHelper.header
        )
      )
      .mapData(data => decode[Statistics](data).toOption.get)
  }

  override def getMessages(seqno: Int): Future[List[Message]] = {
    Fetch
      .fetch(
        baseURI + BoincRPC.Command.GetMessages + "?seqno=" + seqno,
        RequestInit(
          method = HttpMethod.GET,
          headers = FetchHelper.header
        )
      )
      .mapData(data => decode[List[Message]](data).toOption.get)
  }

  override def getNotices(seqno: Int): Future[List[Notice]] = {
    Fetch
      .fetch(
        baseURI + BoincRPC.Command.GetNotices + "?seqno=" + seqno,
        RequestInit(
          method = HttpMethod.GET,
          headers = FetchHelper.header
        )
      )
      .mapData(data => decode[List[Notice]](data).toOption.get)
  }

  override def readGlobalPrefsOverride: Future[Boolean] = {
    Fetch.fetch(baseURI + BoincRPC.Command.ReadGlobalPrefsOverride, RequestInit(method = HttpMethod.PATCH, headers = FetchHelper.header))
      .mapData(data => decode[Boolean](data).toOption.get)
  }
}
