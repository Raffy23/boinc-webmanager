package at.happywetter.boinc.server

import scala.util.Random

import at.happywetter.boinc.AppConfig
import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.shared.boincrpc.ServerSharedConfig
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.util.http4s.ResponseEncodingHelper.withNotMatchingEtag
import at.happywetter.boinc.util.webjar.ResourceResolver
import at.happywetter.boinc.web.css.CSSRenderer

import cats.effect._
import org.http4s.dsl.io._
import org.http4s.headers.{ETag, `Content-Encoding`, `Content-Type`, `Last-Modified`}
import org.slf4j.LoggerFactory
import upickle.default.write

/**
  * Created by:
  *
  * @author Raphael
  * @version 17.08.2017
  */
object WebResourcesRoute:
  import org.http4s._

  private val LOG = LoggerFactory.getILoggerFactory.getLogger(this.getClass.getCanonicalName)

  // Workaround of content encoding Bugs
  private val contentTypes = Map[String, MediaType](
    ".css" -> MediaType.text.css,
    ".html" -> MediaType.text.html,
    ".js" -> MediaType.application.javascript
  )

  private val indexContentEtag = Random.alphanumeric.take(12).mkString
  private def indexContent(config: ServerSharedConfig): String =
    import scalatags.Text.all._

    "<!DOCTYPE html>" +
      html(
        head(
          meta(charset := "UTF-8"),
          meta(name := "viewport", content := "width=device-width, initial-scale=1"),
          link(rel := "shortcut icon", href := "/favicon.ico", `type` := "image/x-icon"),
          scalatags.Text.tags2.title("BOINC Webmanager"),
          link(rel := "stylesheet", `type` := "text/css", href := "/files/css/app.css"),
          link(rel := "stylesheet", `type` := "text/css", href := "/files/app.css")
        ),
        body(
          div(
            id := "app",
            p("Boinc Webmanager is loading ..."),
            raw("""<noscript>
                  | <p style="color:red">This Web-Application needs JavaScript to function properly!</p>
                  |</noscript>
                  |"""".stripMargin)
          ),
          script(`type` := "text/javascript", src := "/files/app.js")
        )
      ).render

  // TODO: Render into file and serve from there ...
  private val cssEtag = Random.alphanumeric.take(12).mkString
  private lazy val cssContent: String = CSSRenderer.render()

  private def indexPage(sharedConfig: ServerSharedConfig) =
    Ok(indexContent(sharedConfig),
       `Content-Type`(MediaType.text.html, Charset.`UTF-8`),
       `Last-Modified`(bootUpDate),
       ETag(indexContentEtag)
    )

  def apply(implicit config: Config): HttpRoutes[IO] = HttpRoutes.of[IO]:

    // Normal index Page which is served
    case request @ GET -> Root => withNotMatchingEtag(request, indexContentEtag) { indexPage(AppConfig.sharedConf) }

    case request @ GET -> Root / "files" / "app.js" => completeWithGipFile(appJS, request)

    case request @ GET -> Root / "files" / "app.css" => completeWithGipFile(appCSS, request)

    case request @ GET -> Root / "favicon.ico" => completeWithGipFile("favicon.ico", request)

    case request @ GET -> Root / "files" / "css" / "app.css" =>
      withNotMatchingEtag(request, cssEtag):
        Ok(cssContent, `Content-Type`(MediaType.text.css, Charset.`UTF-8`), `Last-Modified`(bootUpDate), ETag(cssEtag))

    // Static File content from Web root
    case request @ GET -> "files" /: file => completeWithGipFile(file.segments.mkString("/"), request)

    // To alow the SPA to work any view will render the index page
    case request @ GET -> "view" /: _ =>
      withNotMatchingEtag(request, indexContentEtag) { indexPage(AppConfig.sharedConf) }

    // Default Handler
    case _ => NotFound( /* TODO: Implement Default 404-Page */ )

  private def appJS(implicit config: Config): String =
    if config.development.getOrElse(false) then "boinc-webmanager_client-fastopt-bundle.js"
    else "boinc-webmanager_client-opt-bundle.js"

  private def appCSS(implicit config: Config): String =
    if config.development.getOrElse(false) then "boinc-webmanager_client-fastopt-bundle.css"
    else "boinc-webmanager_client-opt-bundle.css"

  private def fromResource(file: String, request: Request[IO]) =
    StaticFile.fromResource("/public/" + file, Some(request))

  private def fromWebJarResource(root: String, file: String, request: Request[IO]) =
    println(s"fromWebJarResource ${root + file}")
    StaticFile
      .fromResource(root + file, Some(request))
      .semiflatTap(resp =>
        IO {
          println((resp.status, file))
        }
      )

  private def fromFile(file: String, request: Request[IO])(implicit config: Config) =
    StaticFile.fromString(config.server.webroot + file, Some(request))

  // TODO: Check Accept-Encoding: gzip, br
  private def completeWithGipFile(file: String, request: Request[IO])(implicit config: Config) =
    lazy val zipFile =
      if config.development.getOrElse(false) then fromFile(file + ".gz", request)
      else fromResource(file + ".gz", request)

    lazy val normalFile =
      if config.development.getOrElse(false) then fromFile(file, request)
      else fromResource(file, request)

    val cType =
      contentTypes.getOrElse(file.substring(file.lastIndexOf("."), file.length), MediaType.application.`octet-stream`)

    zipFile
      .map(
        _.putHeaders(
          `Content-Encoding`(ContentCoding.gzip),
          `Content-Type`(cType)
        )
      )
      .getOrElseF(
        normalFile.getOrElseF {
          NotFound()
        }
      )
