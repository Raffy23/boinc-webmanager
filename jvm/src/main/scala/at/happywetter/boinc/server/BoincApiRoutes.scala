package at.happywetter.boinc.server

import at.happywetter.boinc.boincclient.WebRPC
import at.happywetter.boinc.shared.boincrpc.BoincRPC.{ProjectAction, WorkunitAction}
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.boincrpc.{BoincRPC, GlobalPrefsOverride}
import at.happywetter.boinc.shared.webrpc._
import at.happywetter.boinc.util.PooledBoincClient
import at.happywetter.boinc.util.http4s.MsgPackRequRespHelper
import at.happywetter.boinc.{AppConfig, BoincManager}
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import at.happywetter.boinc.shared.parser._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import at.happywetter.boinc.util.http4s.RichMsgPackRequest.RichMsgPacKResponse

/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.08.2017
  */
object BoincApiRoutes extends MsgPackRequRespHelper {
  private def getIntParameter(name: String)(implicit params: Map[String,Seq[String]]): Int =
    Try { params(name).head.toInt }.toOption.getOrElse(0)

  def apply(hostManager: BoincManager, projects: XMLProjectStore): HttpRoutes[IO] = HttpRoutes.of[IO] {

    // Basic Meta States
    case GET -> Root / "boinc" => Ok(hostManager.getAllHostNames)
    case GET -> Root / "health" => Ok(hostManager.checkHealth)
    case GET -> Root / "config" => Ok(AppConfig.sharedConf)
    case GET -> Root / "groups" => Ok(hostManager.getSerializableGroups)
    case GET -> Root / "boinc" / "project_list" => Ok(projects.getProjects)

    // Main route for Boinc Data
    case GET -> Root / "boinc" / name / action :? requestParams =>
      hostManager.get(name).map(client => {
        implicit val params: Map[String, Seq[String]] = requestParams

        action match {
          case "tasks" => Ok(client.getTasks())
          case "all_tasks" => Ok(client.getTasks(active = false))
          case "hostinfo" => Ok(client.getHostInfo)
          case "network" => Ok(client.isNetworkAvailable)
          case "projects" => Ok(client.getProjects)
          case "state" => Ok(client.getState)
          case "filetransfers" => Ok(client.getFileTransfer)
          case "disk" => Ok(client.getDiskUsage)
          case "ccstate" => Ok(client.getCCState)
          case "global_prefs_override" => Ok(client.getGlobalPrefsOverride)
          case "statistics" => Ok(client.getStatistics)
          case "messages" => Ok(client.getMessages(getIntParameter("seqno")))
          case "notices" => Ok(client.getNotices(getIntParameter("seqno")))

          case _ => NotAcceptable()
        }
      }).getOrElse(NotFound())


    // Modification of Tasks and Projects
    case request @ POST -> Root / "boinc" / name / "tasks" / task =>
      executeForClient[WorkunitRequestBody, Boolean](hostManager, name, request, (client, requestBody) => {
        client.workunit(requestBody.project, task, WorkunitAction.fromValue(requestBody.action).get)
      })

    case request @ POST -> Root / "boinc" / name / "project" =>
      executeForClient[AddProjectBody, Boolean](hostManager, name, request, (client, requestBody) => {
        WebRPC
          .lookupAccount(requestBody.projectUrl, requestBody.user, Some(requestBody.password))
          .map { case (_, auth) => auth.map(accKey => client.attachProject(requestBody.projectUrl, accKey, requestBody.projectName)) }
          .flatMap(result => result.getOrElse(Future {false}))
      })

    case request @ POST -> Root / "boinc" / name / "projects" =>
      executeForClient[ProjectRequestBody, Boolean](hostManager, name, request, (client, requestBody) =>  {
        client.project(requestBody.project, ProjectAction.fromValue(requestBody.action).get)
      })

    // Change run modes
    case request @ POST -> Root / "boinc" / name / "run_mode" =>
      executeForClient[BoincModeChange, Boolean](hostManager, name, request, (client, requestBody) => {
        client.setRun(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration)
      })

    case request @ POST -> Root / "boinc" / name / "cpu" =>
      executeForClient[BoincModeChange, Boolean](hostManager, name, request, (client, requestBody) => {
        client.setCpu(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration)
      })

    case request @ POST -> Root / "boinc" / name / "gpu" =>
      executeForClient[BoincModeChange, Boolean](hostManager, name, request, (client, requestBody) => {
        client.setGpu(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration)
      })

    case request @ POST -> Root / "boinc" / name / "network" =>
      executeForClient[BoincModeChange, Boolean](hostManager, name, request, (client, requestBody) => {
        client.setNetwork(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration)
      })

    case request @ POST -> Root / "boinc" / name / "global_prefs_override" =>
      executeForClient[GlobalPrefsOverride, Boolean](hostManager, name, request, (client, requestBody) => {
        client.setGlobalPrefsOverride(requestBody)
      })

    case PATCH -> Root / "boinc" / name / "global_prefs_override" =>
      hostManager.get(name).map(client => {
        Ok(client.readGlobalPrefsOverride)
      }).getOrElse(BadRequest())

    case _ => NotAcceptable()
  }

  private def executeForClient[IN, OUT](hostManager: BoincManager, name: String, request: Request[IO], f: (PooledBoincClient, IN) => Future[OUT])(implicit decoder: upickle.default.Reader[IN], encoder: upickle.default.Writer[OUT]): IO[Response[IO]] = {
    hostManager.get(name).map(client => {
      request.decodeJson[IN]{ requestBody =>
        Ok(f(client, requestBody))
      }
    }).getOrElse(BadRequest())
  }

}
