package at.happywetter.boinc.server

import at.happywetter.boinc.{AppConfig, BoincManager}
import at.happywetter.boinc.boincclient.WebRPC
import at.happywetter.boinc.shared.BoincRPC.{ProjectAction, WorkunitAction}
import at.happywetter.boinc.shared._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.08.2017
  */
object BoincApiRoutes {

  import cats.effect._
  import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._
  import org.http4s.circe._
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

  private def getIntParameter(name: String)(implicit params: Map[String,Seq[String]]): Int =
    Try {
      params(name).head.toInt
    }.toOption.getOrElse(0)

  def apply(hostManager: BoincManager, projects: XMLProjectStore): HttpService[IO] = HttpService[IO] {

    // Basic Meta States
    case GET -> Root / "boinc" => Ok(hostManager.getAllHostNames.asJson)
    case GET -> Root / "health" => Ok(hostManager.checkHealth.map(_.asJson))
    case GET -> Root / "config" => Ok(AppConfig.sharedConf.asJson)
    case GET -> Root / "groups" => Ok(hostManager.getSerializableGroups.asJson)
    case GET -> Root / "boinc" / "project_list" => Ok(projects.getProjects.asJson)

    // Main route for Boinc Data
    case GET -> Root / "boinc" / name / action :? requestParams =>
      hostManager.get(name).map(client => {
        implicit val params = requestParams

        action match {
          case "tasks" => Ok(client.getTasks().map(_.asJson))
          case "all_tasks" => Ok(client.getTasks(active = false).map(_.asJson))
          case "hostinfo" => Ok(client.getHostInfo.map(_.asJson))
          case "network" => Ok(client.isNetworkAvailable.map(_.asJson))
          case "projects" => Ok(client.getProjects.map(_.asJson))
          case "state" => Ok(client.getState.map(_.asJson))
          case "filetransfers" => Ok(client.getFileTransfer.map(_.asJson))
          case "disk" => Ok(client.getDiskUsage.map(_.asJson))
          case "ccstate" => Ok(client.getCCState.map(_.asJson))
          case "global_prefs_override" => Ok(client.getGlobalPrefsOverride.map(_.asJson))
          case "statistics" => Ok(client.getStatistics.map(_.asJson))
          case "messages" => Ok(client.getMessages(getIntParameter("seqno")).map(_.asJson))
          case "notices" => Ok(client.getNotices(getIntParameter("seqno")).map(_.asJson))

          case _ => NotAcceptable()
        }
      }).getOrElse(NotFound())


    // Modification of Tasks and Projects
    case request @ POST -> Root / "boinc" / name / "tasks" / task =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          decode[WorkunitRequestBody](body)
            .toOption
            .map(requestBody => client.workunit(requestBody.project, task, WorkunitAction.fromValue(requestBody.action).get))
            .map(f => f.map(_.asJson))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())


    case request @ POST -> Root / "boinc" / name / "project" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          decode[AddProjectBody](body)
            .toOption
            .map(requestBody => {
              WebRPC
                .lookupAccount(requestBody.projectUrl, requestBody.user, Some(requestBody.password))
                .map { case (_, auth) => auth.map(accKey => client.attachProject(requestBody.projectUrl, accKey, requestBody.projectName)) }
                .flatMap(result => result.getOrElse(Future {false}).map(_.asJson))
            })
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "projects" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          decode[ProjectRequestBody](body)
            .toOption
            .map(requestBody => client.project(requestBody.project, ProjectAction.fromValue(requestBody.action).get))
            .map(f => f.map(_.asJson))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())


    // Change run modes
    case request @ POST -> Root / "boinc" / name / "run_mode" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          decode[BoincModeChange](body)
            .toOption
            .map(requestBody => client.setRun(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration))
            .map(f => f.map(_.asJson))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "cpu" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          decode[BoincModeChange](body)
            .toOption
            .map(requestBody => client.setCpu(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration))
            .map(f => f.map(_.asJson))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "gpu" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          decode[BoincModeChange](body)
            .toOption
            .map(requestBody => client.setGpu(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration))
            .map(f => f.map(_.asJson))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "network" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          decode[BoincModeChange](body)
            .toOption
            .map(requestBody => client.setNetwork(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration))
            .map(f => f.map(_.asJson))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())

    case request @ POST -> Root / "boinc" / name / "global_prefs_override" =>
      hostManager.get(name).map(client => {
        request.decode[String] { body =>
          decode[GlobalPrefsOverride](body)
            .toOption
            .map(requestBody => client.setGlobalPrefsOverride(requestBody))
            .map(f => f.map(_.asJson))
            .map(content => Ok(content))
            .getOrElse(InternalServerError())
        }
      }).getOrElse(BadRequest())


    case PATCH -> Root / "boinc" / name / "global_prefs_override" =>
      hostManager.get(name).map(client => {
        Ok(client.readGlobalPrefsOverride.map(_.asJson))
      }).getOrElse(BadRequest())

    case _ => NotAcceptable()
  }


}
