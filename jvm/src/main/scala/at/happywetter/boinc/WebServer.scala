package at.happywetter.boinc

import java.util.concurrent.Executors

import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.server.XMLProjectStore
import at.happywetter.boinc.shared.BoincRPC.{ProjectAction, WorkunitAction}
import at.happywetter.boinc.shared.{ProjectRequestBody, WorkunitRequestBody}
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s._

import scala.io.StdIn
import scalaz.concurrent.Task
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author Raphael
  * @version 19.07.2017
  */
object WebServer extends App  {

  private implicit val scheduler = Executors.newScheduledThreadPool(8)

  private val hostManager = new BoincManager()
  private val config = AppConfig.conf
  private val projects = new XMLProjectStore(AppConfig.conf.boinc.projects.xmlSource)
  config.boinc.hosts.foreach { case (name, host) => hostManager.add(name, new BoincClient(address = host.address, port = host.port, password = host.password))}

  import prickle._
  private val boincRestService = HttpService {
    case GET -> Root / "boinc" =>
      Ok(Pickle.intoString(hostManager.getAllHostNames))
        .putHeaders(Header("Content-Type","application/json"))

    case GET -> Root / "boinc" / "project_list" =>
      Ok(Pickle.intoString(projects.getProjects)).putHeaders(Header("Content-Type","application/json"))

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

          case _ => NotAcceptable()
        }
      }).getOrElse(NotFound()).putHeaders(Header("Content-Type","application/json"))

    case request @ POST -> Root / "boinc" / name / "tasks" / task =>
      hostManager.get(name).map(client => {
        Ok(
          request.body
            .map(b => Unpickle[WorkunitRequestBody].fromString(b.decodeUtf8.right.get))
            .map(requestBody => client.workunit(requestBody.get.project, task, WorkunitAction.fromValue(requestBody.get.action).get))
            .map(f => f.map(response => Pickle.intoString(response)))
            .runLast.unsafePerformSync.get
        )
      }).getOrElse(BadRequest()).putHeaders(Header("Content-Type","application/json"))


    case request @ POST -> Root / "boinc" / name / "projects" =>
      hostManager.get(name).map(client => {
        Ok(
          request.body
            .map(b => Unpickle[ProjectRequestBody].fromString(b.decodeUtf8.right.get))
            .map(requestBody => client.project(requestBody.get.project, ProjectAction.fromValue(requestBody.get.action).get))
            .map(f => f.map(response => Pickle.intoString(response)))
            .runLast.unsafePerformSync.get
        )
      }).getOrElse(BadRequest()).putHeaders(Header("Content-Type","application/json"))

  }

  private val contentTypes = Map[String, String](
    ".css" -> "text/css",
    ".html" -> "text/html",
    ".js" -> "application/javascript"
  )


  private def completeWithGipFile(file: String, request: Request): Task[Response] = {
    val zipFile = StaticFile
      .fromString(config.server.webroot + file + ".gz", Some(request))
      .map(Task.now)

    val cType = contentTypes.get(file.substring(file.lastIndexOf("."), file.length))
    println(file + "->" + cType + " (" + file.substring(file.lastIndexOf("."), file.length) + ")")

    if( zipFile.isEmpty )
      StaticFile.fromString(config.server.webroot + file, Some(request))
        .map(Task.now)
        .getOrElse(NotFound())
    else
      zipFile
        .get
        .putHeaders(
          Header("Content-Encoding","gzip"),
          cType.map(x => Header("Content-Type", x)).getOrElse(Header("",""))
        )

  }

  private val basicWebService = HttpService {
    case request @ GET -> Root =>
      StaticFile.fromString(config.server.webroot + "index.html", Some(request))
        .map(Task.now)
        .getOrElse(InternalServerError())
        .putHeaders(Header("X-Frame-Options","DENY"))
        .putHeaders(Header("X-XSS-Protection","1"))

    case request @ GET -> "files" /: file => completeWithGipFile(file.toList.mkString("/"), request)
    case GET -> "view" /: _ =>
      StaticFile.fromString(config.server.webroot + "index.html", None)
        .map(Task.now)
        .getOrElse(InternalServerError())
        .putHeaders(Header("X-Frame-Options", "DENY"))
        .putHeaders(Header("X-XSS-Protection", "1"))

    case _ => NotFound()
  }

  private val builder =
    BlazeBuilder
    .bindHttp(config.server.port, config.server.address)
    .mountService(boincRestService, "/api")
    .mountService(basicWebService, "/")

  private val server = builder.run

  println(s"Server online at http://${config.server.address}:${config.server.port}/\nPress RETURN to stop...")
  StdIn.readLine()               // let it run until user presses return
  scheduler.shutdown()
  server.shutdownNow()

}
