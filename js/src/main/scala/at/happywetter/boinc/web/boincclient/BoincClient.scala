package at.happywetter.boinc.web.boincclient

import at.happywetter.boinc.shared.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared.{BoincRPC, _}
import at.happywetter.boinc.web.helper.FetchHelper
import org.scalajs.dom.experimental.{Fetch, Headers, HttpMethod, RequestInit}

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
  import prickle._

  def getTasks(active: Boolean = true): Future[List[Result]] = {
    val uri = if(active) "tasks" else "all_tasks"

    Fetch.fetch(baseURI + uri, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[List[Result]].fromString(json = data).get)
  }

  def getHostInfo: Future[HostInfo] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetHostInfo, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[HostInfo].fromString(json = data).get)
  }

  def isNetworkAvailable: Future[Boolean] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetNetworkAvailable, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[Boolean].fromString(json = data).get)
  }

  def getDiskUsage: Future[DiskUsage] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetDiskUsage, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[DiskUsage].fromString(json = data).get)
  }

  override def getProjects: Future[List[Project]] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetProjectStatus, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[List[Project]].fromString(json = data).get)
  }

  override def getState: Future[BoincState] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetState, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[BoincState].fromString(json = data).get)
  }

  override def getFileTransfer: Future[List[FileTransfer]] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetFileTransfer, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[List[FileTransfer]].fromString(json = data).get)
  }

  override def workunit(project: String, name: String, action: WorkunitAction): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + BoincRPC.Command.GetActiveResults + "/" + name,
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = Pickle.intoString(WorkunitRequestBody(project, action.toString))
        )
      )
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[Boolean].fromString(json = data).get)
  }

  override def project(name: String, action: ProjectAction): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + BoincRPC.Command.GetProjectStatus,
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = Pickle.intoString(ProjectRequestBody(name, action.toString))
        )
      )
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[Boolean].fromString(json = data).get)
  }

  override def getCCState: Future[CCState] = {
    Fetch.fetch(baseURI + BoincRPC.Command.GetCCStatus, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[CCState].fromString(json = data).get)
  }

  override def setCpu(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + "cpu",
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = Pickle.intoString(BoincModeChange(mode.toString, duration))
        )
      )
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[Boolean].fromString(json = data).get)
  }

  override def setGpu(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + "gpu",
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = Pickle.intoString(BoincModeChange(mode.toString, duration))
        )
      )
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[Boolean].fromString(json = data).get)
  }

  override def setNetwork(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + "network",
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = Pickle.intoString(BoincModeChange(mode.toString, duration))
        )
      )
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[Boolean].fromString(json = data).get)
  }

  override def getGlobalPrefsOverride: Future[GlobalPrefsOverride] = {
    Fetch.fetch(baseURI + BoincRPC.Command.ReadGlobalPrefsOverride, RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[GlobalPrefsOverride].fromString(json = data).get)
  }

  override def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride) = ???

  override def setRun(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] = {
    Fetch
      .fetch(
        baseURI + "run_mode",
        RequestInit(
          method = HttpMethod.POST,
          headers = FetchHelper.header,
          body = Pickle.intoString(BoincModeChange(mode.toString, duration))
        )
      )
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[Boolean].fromString(json = data).get)
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
          body = Pickle.intoString(AddProjectBody(url, name, username, password))
        )
      )
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
      .map(data => Unpickle[Boolean].fromString(json = data).get)
  }
}
