package at.happywetter.boinc.server

import at.happywetter.boinc.AppConfig.Config

import scalaz.concurrent.Task

/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.08.2017
  */
object WebResourcesRoute {

  import org.http4s._
  import org.http4s.dsl._

  // Workaround of content encoding Bugs
  private val contentTypes = Map[String, String](
    ".css" -> "text/css",
    ".html" -> "text/html",
    ".js" -> "application/javascript"
  )


  def apply(implicit config: Config): HttpService = HttpService {

    // Normal index Page which is served
    case request@GET -> Root =>
      StaticFile.fromString(config.server.webroot + "index.html", Some(request))
        .map(Task.now)
        .getOrElse(InternalServerError("Could not read index.html File!"))
        .putHeaders(Header("X-Frame-Options", "DENY"))
        .putHeaders(Header("X-XSS-Protection", "1"))

    // Static File content from Web root
    case request@GET -> "files" /: file => completeWithGipFile(file.toList.mkString("/"), request)

    // To alow the SPA to work any view will render the index page
    case GET -> "view" /: _ =>
      StaticFile.fromString(config.server.webroot + "index.html", None)
        .map(Task.now)
        .getOrElse(InternalServerError("Could not read index.html File!"))
        .putHeaders(Header("X-Frame-Options", "DENY"))
        .putHeaders(Header("X-XSS-Protection", "1"))

    // Default Handler
    case _ => NotFound(/* TODO: Implement Default 404-Page */)
  }


  private def completeWithGipFile(file: String, request: Request)(implicit config: Config): Task[Response] = {
    val zipFile = StaticFile
      .fromString(config.server.webroot + file + ".gz", Some(request))
      .map(Task.now)

    val cType = contentTypes.get(file.substring(file.lastIndexOf("."), file.length))

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

}
