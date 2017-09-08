package at.happywetter.boinc.server

import at.happywetter.boinc.AppConfig.Config

import fs2.Task
import fs2.interop.cats._

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

  private lazy val indexContent: String = {
    import scalatags.Text.all._

    "<!DOCTYPE html>" +
    html(
      head(
        meta(charset := "UTF-8"),
        scalatags.Text.tags2.title("BOINC Webmanager"),

        link( rel := "stylesheet", `type` := "text/css", href := "/files/css/font-awesome.min.css"),
        link( rel := "stylesheet", `type` := "text/css", href := "/files/css/nprogress.css"),

        script( `type` := "text/javascript", src := "/files/boinc-webmanager-jsdeps.js")
      ),

      body(
        div( id := "app-container",
          p("Boinc Webmanager is loading ...")
        ),

        script( `type` := "text/javascript", src := "/files/boinc-webmanager-fastopt.js"),
        script( `type` := "text/javascript", "Main.launch()" )
      )
    ).render
  }

  private lazy val indexPage =
    Ok(indexContent)
      .putHeaders(Header("X-Frame-Options", "DENY"))
      .putHeaders(Header("X-XSS-Protection", "1"))
      .putHeaders(Header("Content-Type", "text/html; charset=UTF-8"))


  def apply(implicit config: Config): HttpService = HttpService {

    // Normal index Page which is served
    case GET -> Root => indexPage

    // Static File content from Web root
    case request@GET -> "files" /: file => completeWithGipFile(file.toList.mkString("/"), request)

    // To alow the SPA to work any view will render the index page
    case GET -> "view" /: _ => indexPage

    // Default Handler
    case _ => NotFound(/* TODO: Implement Default 404-Page */)
  }


  private def fromResource(file: String, request: Request) =
    StaticFile.fromResource("/web-root/" + file, Some(request))

  private def fromFile(file: String, request: Request)(implicit config: Config) =
    StaticFile.fromString(config.server.webroot + file, Some(request))

  private def completeWithGipFile(file: String, request: Request)(implicit config: Config) = {
    lazy val zipFile =
      if (config.development.getOrElse(false)) fromFile(file + ".gz", request)
      else fromResource(file + ".gz", request)

    lazy val normalFile =
      if (config.development.getOrElse(false)) fromFile(file, request)
      else fromResource(file, request)


    val cType = contentTypes.get(file.substring(file.lastIndexOf("."), file.length))

    zipFile
      .map(
        _.putHeaders(
          Header("Content-Encoding", "gzip"),
          cType.map(x => Header("Content-Type", x)).getOrElse(Header("", ""))
        )
      ).getOrElseF(
        normalFile.getOrElseF(NotFound())
      )
  }

}
