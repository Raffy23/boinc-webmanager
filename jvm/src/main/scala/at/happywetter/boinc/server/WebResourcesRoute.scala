package at.happywetter.boinc.server

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.util.IOAppTimer
import at.happywetter.boinc.util.http4s.ResponseEncodingHelper.withNotMatchingEtag
import at.happywetter.boinc.util.webjar.ResourceResolver
import at.happywetter.boinc.web.css.CSSRenderer
import cats.effect._
import org.http4s.dsl.io._
import org.slf4j.LoggerFactory
import org.typelevel.ci.CIString

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
  private val LOG = LoggerFactory.getILoggerFactory.getLogger(this.getClass.getCanonicalName)

  // Workaround of content encoding Bugs
  private val contentTypes = Map[String, String](
    ".css" -> "text/css",
    ".html" -> "text/html",
    ".js" -> "application/javascript",
  )

  // Load resource roots that are dependencies
  private val nprogessPath    = ResourceResolver.getResourceRoot(repo="bower", name="nprogress")
  private val fontAwesomePath = ResourceResolver.getResourceRoot(repo=""     , name="font-awesome")
  private val flagIconCssPath = ResourceResolver.getResourceRoot(repo="npm"  , name="flag-icon-css")
  private val pureCssPath     = ResourceResolver.getResourceRoot(repo="npm"  , name="purecss")

  private val indexContentEtag = Random.alphanumeric.take(12).mkString
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
        link( rel := "stylesheet", `type` := "text/css", href := "/files/css/flag-icon.css"),
        link( rel := "stylesheet", `type` := "text/css", href := "/files/css/app.css"),

        script( `type` := "text/javascript", src := "/files/app-jsdeps.js")
      ),

      body(
        div( id := "app-container",
          p("Boinc Webmanager is loading ..."),
          raw(
            """<noscript>
              | <p style="color:red">This Web-Application needs JavaScript to function properly!</p>
              |</noscript>
              |"""".stripMargin)
        ),

        script( `type` := "text/javascript", src := "/files/app.js"),
        script( `type` := "text/javascript", launchScript )
      )
    ).render
  }

  // TODO: Render into file and serve from there ...
  private val cssEtag = Random.alphanumeric.take(12).mkString
  private lazy val cssContent: String = CSSRenderer.render()

  private lazy val launchScript =
    """
      | if(Main === undefined) {
      |   alert('Can not start Application, maybe app.js could not be loaded?')
      | } else {
      |   Main.launch()
      | }
    """.stripMargin

  private def indexPage =
    Ok(indexContent,
      Header("Content-Type", "text/html; charset=UTF-8"),
      Header("X-Frame-Options", "DENY"),
      Header("Last-Modified", bootUpTime),
      Header("ETag", indexContentEtag)
    )

  def apply(implicit config: Config, cS: ContextShift[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {

    // Normal index Page which is served
    case request@GET -> Root => withNotMatchingEtag(request, indexContentEtag) { indexPage }

    case request@GET -> Root / "files" / "app.js" => completeWithGipFile(appJS, request)

    case request@GET -> Root / "files" / "app-jsdeps.js" => completeWithGipFile(appDeptJS, request)

    case request@GET -> Root / "favicon.ico" => completeWithGipFile("favicon.ico", request)

    case request@GET -> Root / "files" / "css" / "app.css" =>
      withNotMatchingEtag(request, cssEtag) {
        Ok(cssContent,
          Header("Content-Type", "text/css; charset=UTF-8"),
          Header("Last-Modified", bootUpTime),
          Header("ETag", cssEtag)
        )
      }

    case request@GET -> Root / "files" / "css" /  "font-awesome.min.css" =>
      fromWebJarResource(fontAwesomePath, "css/all.min.css", request).getOrElseF(NotFound())

    case request@GET -> Root / "files" / "webfonts" / font =>
      fromWebJarResource(fontAwesomePath, s"webfonts/$font", request).getOrElseF(NotFound())

    case request@GET -> Root / "files" / "css" / "nprogress.css" =>
      fromWebJarResource(nprogessPath, "nprogress.css", request).getOrElseF(NotFound())

    case request@GET -> Root / "files" / "css" / "flag-icon.css" =>
      fromWebJarResource(flagIconCssPath, "css/flag-icon.min.css", request).getOrElseF(NotFound())

    case request@GET -> Root / "files" / "flags" / size / file =>
      fromWebJarResource(flagIconCssPath, s"flags/$size/$file", request).getOrElseF(NotFound())

    // Static File content from Web root
    case request@GET -> "files" /: file => completeWithGipFile(file.toList.mkString("/"), request)

    // To alow the SPA to work any view will render the index page
    case request@GET -> "view" /: _ => withNotMatchingEtag(request, indexContentEtag) { indexPage }

    // Default Handler
    case _ => NotFound(/* TODO: Implement Default 404-Page */)
  }
  
  private def appJS(implicit config: Config): String =
    if (config.development.getOrElse(false)) "boinc-webmanager_client-fastopt.js"
    else "boinc-webmanager_client-opt.js"

  private def appDeptJS(implicit config: Config): String =
    if (config.development.getOrElse(false)) "boinc-webmanager_client-jsdeps.js"
    else "boinc-webmanager_client-jsdeps.min.js"

  private def fromResource(file: String, request: Request[IO])(implicit cS: ContextShift[IO]) =
    StaticFile.fromResource("/web-root/" + file, blocker, Some(request))

  private def fromWebJarResource(root: String, file: String, request: Request[IO])(implicit cS: ContextShift[IO]) =
    StaticFile.fromResource(root + file, blocker, Some(request))

  private def fromFile(file: String, request: Request[IO])(implicit config: Config, cS: ContextShift[IO]) =
    StaticFile.fromString(config.server.webroot + file, blocker, Some(request))

  private def completeWithGipFile(file: String, request: Request[IO])(implicit config: Config, cS: ContextShift[IO]) = {
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
