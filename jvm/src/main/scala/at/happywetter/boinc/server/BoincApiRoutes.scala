package at.happywetter.boinc.server

import at.happywetter.boinc.BoincManager
import at.happywetter.boinc.shared.BoincRPC.{ProjectAction, WorkunitAction}
import at.happywetter.boinc.shared.{BoincModeChange, BoincRPC, ProjectRequestBody, WorkunitRequestBody}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.08.2017
  */
object BoincApiRoutes {

  import org.http4s._
  import org.http4s.dsl._
  import prickle._

  def apply(hostManager: BoincManager, projects: XMLProjectStore): HttpService = HttpService {

    // Basic Meta States
    case GET -> Root / "boinc" => Ok(Pickle.intoString(hostManager.getAllHostNames))
    case GET -> Root / "boinc" / "project_list" => Ok(Pickle.intoString(projects.getProjects))

    // Main route for Boinc Data
    case GET -> Root / "boinc" / name / action =>
      hostManager.get(name).map(client => {
        action match {
          case "tasks" => Ok(client.getTasks().map(Pickle.intoString(_)))
          case "all_tasks" => Ok(client.getTasks(active = false).map(Pickle.intoString(_)))
          case "hostinfo" => Ok(client.getHostInfo.map(Pickle.intoString(_)))
          case "network" => Ok(client.isNetworkAvailable.map(Pickle.intoString(_)))
          case "projects" => Ok(client.getProjects.map(Pickle.intoString(_)))
          case "state" => Ok(client.getState.map(Pickle.intoString(_)))
          case "filetransfers" => Ok(client.getFileTransfer.map(Pickle.intoString(_)))
          case "disk" => Ok(client.getDiskUsage.map(Pickle.intoString(_)))
          case "ccstate" => Ok(client.getCCState.map(Pickle.intoString(_)))
          case "global_prefs_override" => Ok(client.getGlobalPrefsOverride.map(Pickle.intoString(_)))

          case _ => NotAcceptable()
        }
      }).getOrElse(NotFound())


    // Modification of Tasks and Projects
    case request @ POST -> Root / "boinc" / name / "tasks" / task =>
      hostManager.get(name).map(client => {
        Ok(
          request.body
            .map(b => Unpickle[WorkunitRequestBody].fromString(b.decodeUtf8.right.get))
            .map(requestBody => client.workunit(requestBody.get.project, task, WorkunitAction.fromValue(requestBody.get.action).get))
            .map(f => f.map(response => Pickle.intoString(response)))
            .runLast.unsafePerformSync.get
        )
      }).getOrElse(BadRequest())


    case request @ POST -> Root / "boinc" / name / "projects" =>
      hostManager.get(name).map(client => {
        Ok(
          request.body
            .map(b => Unpickle[ProjectRequestBody].fromString(b.decodeUtf8.right.get))
            .map(requestBody => client.project(requestBody.get.project, ProjectAction.fromValue(requestBody.get.action).get))
            .map(f => f.map(response => Pickle.intoString(response)))
            .runLast.unsafePerformSync.get
        )
      }).getOrElse(BadRequest())


    // Change run modes
    case request @ POST -> Root / "boinc" / name / "run_mode" =>
      hostManager.get(name).map(client => {
        Ok(
          request.body
            .map(b => Unpickle[BoincModeChange].fromString(b.decodeUtf8.right.get))
            .map(requestBody => client.setRun(BoincRPC.Modes.fromValue(requestBody.get.mode).get, requestBody.get.duration))
            .map(f => f.map(response => Pickle.intoString(response)))
            .runLast.unsafePerformSync.get
        )
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "cpu" =>
      hostManager.get(name).map(client => {
        Ok(
          request.body
            .map(b => Unpickle[BoincModeChange].fromString(b.decodeUtf8.right.get))
            .map(requestBody => client.setCpu(BoincRPC.Modes.fromValue(requestBody.get.mode).get, requestBody.get.duration))
            .map(f => f.map(response => Pickle.intoString(response)))
            .runLast.unsafePerformSync.get
        )
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "gpu" =>
      hostManager.get(name).map(client => {
        Ok(
          request.body
            .map(b => Unpickle[BoincModeChange].fromString(b.decodeUtf8.right.get))
            .map(requestBody => client.setGpu(BoincRPC.Modes.fromValue(requestBody.get.mode).get, requestBody.get.duration))
            .map(f => f.map(response => Pickle.intoString(response)))
            .runLast.unsafePerformSync.get
        )
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "network" =>
      hostManager.get(name).map(client => {
        Ok(
          request.body
            .map(b => Unpickle[BoincModeChange].fromString(b.decodeUtf8.right.get))
            .map(requestBody => client.setNetwork(BoincRPC.Modes.fromValue(requestBody.get.mode).get, requestBody.get.duration))
            .map(f => f.map(response => Pickle.intoString(response)))
            .runLast.unsafePerformSync.get
        )
      }).getOrElse(BadRequest())


    case _ => NotAcceptable()
  }


}
