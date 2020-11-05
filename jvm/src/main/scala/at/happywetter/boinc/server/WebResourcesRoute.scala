package at.happywetter.boinc.server

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.concurrent.{Executors, ThreadFactory}

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.util.IOAppTimer
import at.happywetter.boinc.web.css.CSSRenderer
import cats.effect._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.slf4j.LoggerFactory
import org.typelevel.ci.CIString

import scala.concurrent.ExecutionContext
import scala.util.Random

/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.08.2017
  */
object WebResourcesRoute {
  import org.http4s._

  private val blocker = IOAppTimer.blocker
  private implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  private val LOG = LoggerFactory.getILoggerFactory.getLogger(this.getClass.getCanonicalName)

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
        meta(name := "viewport", content := "width=device-width, initial-scale=1"),
        link(rel := "shortcut icon", href := "/favicon.ico", `type` := "image/x-icon"),

        scalatags.Text.tags2.title("BOINC Webmanager"),

        link( rel := "stylesheet", `type` := "text/css", href := "/files/css/font-awesome.min.css"),
        link( rel := "stylesheet", `type` := "text/css", href := "/files/css/nprogress.css"),
        link( rel := "stylesheet", `type` := "text/css", href := "/files/css/app.css"),

        script( `type` := "text/javascript", src := "/files/app-jsdeps.js")
      ),

      body(
        div( id := "app-container",
          p("Boinc Webmanager is loading ...")
        ),

        script( `type` := "text/javascript", src := "/files/app.js"),
        script( `type` := "text/javascript", launchScript )
      )
    ).render
  }

  // TODO: Render into file and serve from there ...
  private val cssEtag = Random.alphanumeric.take(12).mkString
  private lazy val cssContent: IO[Response[IO]] = Ok(CSSRenderer.render()).map(
    _.putHeaders(
      Header("Content-Type", "text/css; charset=UTF-8"),

      // This should probably be the build time and not the startup time of the generated CSS ...
      Header("Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))),
      Header("ETag", cssEtag)
    )
  )

  private lazy val launchScript =
    """
      | if(Main === undefined) {
      |   alert('Can not start Application, maybe app.js could not be loaded?')
      | } else {
      |   Main.launch()
      | }
    """.stripMargin

  private lazy val indexPage =
    Ok(indexContent,
      Header("X-Frame-Options", "DENY"),
      Header("X-XSS-Protection", "1"),
      Header("Content-Type", "text/html; charset=UTF-8")
    )

  def apply(implicit config: Config): HttpRoutes[IO] = HttpRoutes.of[IO] {

    // Normal index Page which is served
    case GET -> Root => indexPage

    case request@GET -> Root / "files" / "app.js" => completeWithGipFile(appJS, request)

    case request@GET -> Root / "files" / "app-jsdeps.js" => completeWithGipFile(appDeptJS, request)

    case request@GET -> Root / "favicon.ico" => completeWithGipFile("favicon.ico", request)

    case request@GET -> Root / "files" / "css" / "app.css" =>
      request.headers.get(CIString("If-None-Match")).map { etag =>
        if (etag.value == cssEtag) IO { new Response[IO](status = NotModified) }
        else cssContent
      }.getOrElse(cssContent)

    // Static File content from Web root
    case request@GET -> "files" /: file => completeWithGipFile(file.toList.mkString("/"), request)

    // To alow the SPA to work any view will render the index page
    case GET -> "view" /: _ => indexPage

    // Default Handler
    case _ => NotFound(/* TODO: Implement Default 404-Page */)
  }

  // TODO: Rename static resources, a change in build.sbt has renamed them
  private def appJS(implicit config: Config): String =
    if (config.development.getOrElse(false)) "boinc-webmanager_client-fastopt.js"
    else "boinc-webmanager_client-opt.js"

  private def appDeptJS(implicit config: Config): String =
    if (config.development.getOrElse(false)) "boinc-webmanager_client-jsdeps.js"
    else "boinc-webmanager_client-jsdeps.min.js"

  private def fromResource(file: String, request: Request[IO]) =
    StaticFile.fromResource("/web-root/" + file, blocker, Some(request))

  private def fromFile(file: String, request: Request[IO])(implicit config: Config) =
    StaticFile.fromString(config.server.webroot + file, blocker, Some(request))


  private def completeWithGipFile(file: String, request: Request[IO])(implicit config: Config) = {
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
        normalFile.getOrElseF {
          LOG.error(s"Can not load $file")
          NotFound()
        }
      )
  }

}
