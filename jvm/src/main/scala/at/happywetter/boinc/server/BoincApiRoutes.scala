package at.happywetter.boinc.server

import at.happywetter.boinc.{AppConfig, BoincManager}
import at.happywetter.boinc.boincclient.WebRPC
import at.happywetter.boinc.shared.BoincRPC.{ProjectAction, WorkunitAction}
import at.happywetter.boinc.shared._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
    case GET -> Root / "health" => Ok(hostManager.checkHealth.map(Pickle.intoString(_)))
    case GET -> Root / "config" => Ok(Pickle.intoString(AppConfig.sharedConf))
    case GET -> Root / "groups" => Ok(Pickle.intoString(hostManager.getSerializableGroups))
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
          case "statistics" => Ok(client.getStatistics.map(Pickle.intoString(_)))
          case "messages" => Ok(client.getAllMessages.map(Pickle.intoString(_)))
          case "notices" => Ok(client.getAllNotices.map(Pickle.intoString(_)))

          case _ => NotAcceptable()
        }
      }).getOrElse(NotFound())


    // Modification of Tasks and Projects
    case request @ POST -> Root / "boinc" / name / "tasks" / task =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          Unpickle[WorkunitRequestBody]
            .fromString(body)
            .map(requestBody => client.workunit(requestBody.project, task, WorkunitAction.fromValue(requestBody.action).get))
            .map(f => f.map(response => Pickle.intoString(response)))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())


    case request @ POST -> Root / "boinc" / name / "project" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          Unpickle[AddProjectBody]
            .fromString(body)
            .map(requestBody => {
              WebRPC
                .lookupAccount(requestBody.projectUrl, requestBody.user, Some(requestBody.password))
                .map { case (_, auth) => auth.map(accKey => client.attachProject(requestBody.projectUrl, accKey, requestBody.projectName)) }
                .flatMap(result => result.getOrElse(Future {false}).map(s => Pickle.intoString(s)))
            })
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "projects" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          Unpickle[ProjectRequestBody]
            .fromString(body)
            .map(requestBody => client.project(requestBody.project, ProjectAction.fromValue(requestBody.action).get))
            .map(f => f.map(response => Pickle.intoString(response)))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())


    // Change run modes
    case request @ POST -> Root / "boinc" / name / "run_mode" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          Unpickle[BoincModeChange]
            .fromString(body)
            .map(requestBody => client.setRun(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration))
            .map(f => f.map(response => Pickle.intoString(response)))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "cpu" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          Unpickle[BoincModeChange]
            .fromString(body)
            .map(requestBody => client.setCpu(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration))
            .map(f => f.map(response => Pickle.intoString(response)))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "gpu" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          Unpickle[BoincModeChange]
            .fromString(body)
            .map(requestBody => client.setGpu(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration))
            .map(f => f.map(response => Pickle.intoString(response)))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "network" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          Unpickle[BoincModeChange]
            .fromString(body)
            .map(requestBody => client.setNetwork(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration))
            .map(f => f.map(response => Pickle.intoString(response)))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())

    case _ => NotAcceptable()
  }


}
