package at.happywetter.boinc.server

import scala.util.Try

import at.happywetter.boinc.BoincManager.AddedByUser
import at.happywetter.boinc.boincclient.WebRPC
import at.happywetter.boinc.dto.DatabaseDTO.CoreClient
import at.happywetter.boinc.shared.boincrpc.BoincRPC.{ProjectAction, WorkunitAction}
import at.happywetter.boinc.shared.boincrpc.{
  AddNewHostRequestBody,
  AddProjectBody,
  ApplicationError,
  BoincModeChange,
  BoincProjectMetaData,
  BoincRPC,
  GlobalPrefsOverride,
  ProjectRequestBody,
  RetryFileTransferBody,
  WorkunitRequestBody
}
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.rpc.DashboardDataEntry
import at.happywetter.boinc.util.PooledBoincClient
import at.happywetter.boinc.util.http4s.ResponseEncodingHelper
import at.happywetter.boinc.util.http4s.RichMsgPackRequest.RichMsgPacKResponse
import at.happywetter.boinc.{AppConfig, BoincManager, Database}

import cats.effect._
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.dsl.io._

/**
  * Created by:
  *
  * @author Raphael
  * @version 17.08.2017
  */
object BoincApiRoutes extends ResponseEncodingHelper:
  private def getIntParameter(name: String)(implicit params: Map[String, collection.Seq[String]]): Int =
    Try { params(name).head.toInt }.toOption.getOrElse(0)

  def apply(hostManager: BoincManager, projects: XMLProjectStore, db: Database): HttpRoutes[IO] = HttpRoutes.of[IO]:

    // Redirect to swagger
    case GET -> Root => SwaggerRoutes.redirectToEndpoint()

    // Basic Meta States
    case request @ GET -> Root / "boinc" => Ok(hostManager.getAllHostNames, request)
    // case request @ GET -> Root / "health"  => Ok(hostManager.checkHealth, request)
    case request @ GET -> Root / "config"                 => Ok(AppConfig.sharedConf, request)
    case request @ GET -> Root / "groups"                 => Ok(hostManager.getSerializableGroups, request)
    case request @ GET -> Root / "version"                => Ok(hostManager.getVersion, request)
    case request @ GET -> Root / "boinc" / "project_list" => OkWithEtag(projects.getProjects, projects.eTag, request)

    // Main route for Boinc Data
    case request @ GET -> Root / "boinc" / name / action :? requestParams =>
      hostManager
        .get(name)
        .semiflatMap(client => {
          implicit val params: Map[String, collection.Seq[String]] = requestParams

          if requestParams.contains("healthy") && client.deathCounter.get.unsafeRunSync() >= 1 then {
            encode(RequestTimeout, ApplicationError("core_client_is_not_healthy"), request)
          } else
            action match {
              case "tasks"                 => Ok(client.getTasks(), request)
              case "all_tasks"             => Ok(client.getTasks(active = false), request)
              case "hostinfo"              => Ok(client.getHostInfo, request)
              case "network"               => Ok(client.isNetworkAvailable, request)
              case "projects"              => Ok(client.getProjects, request)
              case "state"                 => Ok(client.getState, request)
              case "filetransfers"         => Ok(client.getFileTransfer, request)
              case "disk"                  => Ok(client.getDiskUsage, request)
              case "ccstate"               => Ok(client.getCCState, request)
              case "global_prefs_override" => Ok(client.getGlobalPrefsOverride, request)
              case "statistics"            => Ok(client.getStatistics, request)
              case "messages"              => Ok(client.getMessages(getIntParameter("seqno")), request)
              case "notices"               => Ok(client.getNotices(getIntParameter("seqno")), request)
              case "version"               => Ok(client.getVersion, request)
              case "app_config" => {
                val now = System.currentTimeMillis()
                Ok(client.getAppConfig(params("url").head), request).map(r => {
                  val future = System.currentTimeMillis()
                  val delta = future - now
                  System.out.println("GET app_config took " + delta + "ms")

                  r
                })
              }

              case _ => NotAcceptable()
            }
        })
        .getOrElseF(NotFound())

    case request @ GET -> Root / "webmanager" / "dashboard" / name =>
      hostManager
        .get(name)
        .semiflatMap(client => {
          for {
            state <- client.getState.map(v => Some(v)).handleError(ex => Option.empty)
            fTransfer <- client.getFileTransfer.map(v => Some(v)).handleError(ex => Option.empty)

            condition <- IO.pure(state.nonEmpty && fTransfer.nonEmpty)
            result <-
              if condition then Ok(DashboardDataEntry(state.get, fTransfer.get), request)
              else NotFound()
          } yield result
        })
        .getOrElseF(NotFound())

    // Modification of Tasks and Projects
    case request @ POST -> Root / "boinc" / name / "tasks" / task =>
      executeForClient[WorkunitRequestBody, Boolean](
        hostManager,
        name,
        request,
        (client, requestBody) => {
          client.workunit(requestBody.project, task, WorkunitAction.fromValue(requestBody.action).get)
        }
      )

    case request @ POST -> Root / "boinc" / name / "project" =>
      executeForClient[AddProjectBody, Boolean](
        hostManager,
        name,
        request,
        (client, requestBody) => {
          WebRPC
            .lookupAccount(requestBody.projectUrl, requestBody.user, Some(requestBody.password))
            .foldF(
              ex =>
                IO.println(ex) *>
                IO.pure(false),
              auth =>
                client.attachProject(requestBody.projectUrl, auth, requestBody.projectName) *>
                IO.pure(true)
            )
        }
      )

    case request @ PATCH -> Root / "boinc" / name / "project" =>
      executeForClient[ProjectRequestBody, Boolean](
        hostManager,
        name,
        request,
        (client, requestBody) => {
          client.project(requestBody.project, ProjectAction.fromValue(requestBody.action).get)
        }
      )

    // Change run modes
    case request @ POST -> Root / "boinc" / name / "run_mode" =>
      executeForClient[BoincModeChange, Boolean](
        hostManager,
        name,
        request,
        (client, requestBody) => {
          client.setRun(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration)
        }
      )

    case request @ POST -> Root / "boinc" / name / "cpu" =>
      executeForClient[BoincModeChange, Boolean](
        hostManager,
        name,
        request,
        (client, requestBody) => {
          client.setCpu(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration)
        }
      )

    case request @ POST -> Root / "boinc" / name / "gpu" =>
      executeForClient[BoincModeChange, Boolean](
        hostManager,
        name,
        request,
        (client, requestBody) => {
          client.setGpu(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration)
        }
      )

    case request @ POST -> Root / "boinc" / name / "network" =>
      executeForClient[BoincModeChange, Boolean](
        hostManager,
        name,
        request,
        (client, requestBody) => {
          client.setNetwork(BoincRPC.Modes.fromValue(requestBody.mode).get, requestBody.duration)
        }
      )

    case request @ POST -> Root / "boinc" / name / "global_prefs_override" =>
      executeForClient[GlobalPrefsOverride, Boolean](hostManager,
                                                     name,
                                                     request,
                                                     (client, requestBody) => {
                                                       client.setGlobalPrefsOverride(requestBody)
                                                     }
      )

    case request @ PATCH -> Root / "boinc" / name / "global_prefs_override" =>
      hostManager
        .get(name)
        .semiflatMap(client => {
          Ok(client.readGlobalPrefsOverride, request)
        })
        .getOrElseF(BadRequest())

    case request @ POST -> Root / "boinc" / name / "retry_file_transfer" =>
      executeForClient[RetryFileTransferBody, Boolean](hostManager,
                                                       name,
                                                       request,
                                                       (client, requestBody) => {
                                                         client.retryFileTransfer(requestBody.project, requestBody.file)
                                                       }
      )

    // Add / Remove boinc hosts
    case request @ POST -> Root / "boinc" / name =>
      request.decodeJson[AddNewHostRequestBody] { host =>
        // TODO: Implement correct state stuff ...
        Ok(
          db.clients.insert(CoreClient(name, host.address, host.port, host.password, CoreClient.ADDED_BY_USER)) *>
            hostManager.add(name, host.address, host.port, host.password, AddedByUser) *>
            IO.pure(true),
          request
        )
      }

    case request @ PATCH -> Root / "boinc" / name =>
      request.decodeJson[AddNewHostRequestBody] { host =>
        db.clients.update(CoreClient(name, host.address, host.port, host.password, CoreClient.ADDED_BY_USER))

        hostManager.remove(name)
        hostManager.add(name, host.address, host.port, host.password, AddedByUser)

        // TODO: Implement correct state stuff ...
        Ok(true, request)
      }

    case request @ POST -> Root / "boinc" / "app_config" =>
      Ok("Not Implemented", request)

    case request @ DELETE -> Root / "boinc" / name =>
      Ok(
        db.clients.delete(name) *>
          hostManager.remove(name) *>
          IO.pure(true),
        request
      )

    // More details
    case request @ GET -> Root / "boinc" / "host_details" =>
      Ok(hostManager.getDetailedHosts, request)

    // Add / Remove boinc stuff
    case request @ POST -> Root / "boinc" / "project_list" =>
      request.decodeJson[BoincProjectMetaData] { project =>
        projects.addProject(project.name, project).flatMap(_ => Ok(true, request))
      }

    // Let it fall though to another route handler
    // case _ => NotAcceptable()

  private def executeForClient[IN, OUT](hostManager: BoincManager,
                                        name: String,
                                        request: Request[IO],
                                        f: (PooledBoincClient, IN) => IO[OUT]
  )(implicit decoder: upickle.default.Reader[IN], encoder: upickle.default.Writer[OUT]): IO[Response[IO]] =
    hostManager
      .get(name)
      .semiflatMap(client =>
        request.decodeJson[IN] { requestBody =>
          Ok(f(client, requestBody), request)
        }
      )
      .getOrElseF(BadRequest())
