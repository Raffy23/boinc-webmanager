package at.happywetter.boinc.server

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

import scala.util.Random

/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.08.2017
  */
object WebResourcesRoute {
  import org.http4s._

  private val LOG = LoggerFactory.getILoggerFactory.getLogger(this.getClass.getCanonicalName)

  // Workaround of content encoding Bugs
  private val contentTypes = Map[String, MediaType](
    ".css" -> MediaType.text.css,
    ".html" -> MediaType.text.html,
    ".js"   -> MediaType.application.javascript,
  )

  // Load resource roots that are dependencies
  private val nprogessPath    = ResourceResolver.getResourceRoot(repo="bower", name="nprogress")
  private val fontAwesomePath = ResourceResolver.getResourceRoot(repo=""     , name="font-awesome")
  private val flagIconCssPath = ResourceResolver.getResourceRoot(repo="npm"  , name="flag-icon-css")
  private val pureCssPath     = ResourceResolver.getResourceRoot(repo="npm"  , name="purecss")

  private val indexContentEtag = Random.alphanumeric.take(12).mkString
  private def indexContent(config: ServerSharedConfig): String = {
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
        script( `type` := "text/javascript", raw(launchScript(config)) )
      )
    ).render
  }

  // TODO: Render into file and serve from there ...
  private val cssEtag = Random.alphanumeric.take(12).mkString
  private lazy val cssContent: String = CSSRenderer.render()

  private def launchScript(config: ServerSharedConfig) =
    s"""
      | if(Main === undefined) {
      |   alert('Can not start Application, maybe app.js could not be loaded?')
      | } else {
      |   Main.launch(${write(config)})
      | }
    """.stripMargin

  private def indexPage(sharedConfig: ServerSharedConfig) =
    Ok(indexContent(sharedConfig),
      `Content-Type`(MediaType.text.html, Charset.`UTF-8`),
      `Last-Modified`(bootUpDate),
      ETag(indexContentEtag)
    )

  def apply(implicit config: Config): HttpRoutes[IO] = HttpRoutes.of[IO] {

    // Normal index Page which is served
    case request@GET -> Root => withNotMatchingEtag(request, indexContentEtag) { indexPage(AppConfig.sharedConf) }

    case request@GET -> Root / "files" / "app.js" => completeWithGipFile(appJS, request)

    case request@GET -> Root / "files" / "app-jsdeps.js" => completeWithGipFile(appDeptJS, request)

    case request@GET -> Root / "favicon.ico" => completeWithGipFile("favicon.ico", request)

    case request@GET -> Root / "files" / "css" / "app.css" =>
      withNotMatchingEtag(request, cssEtag) {
        Ok(cssContent,
          `Content-Type`(MediaType.text.css, Charset.`UTF-8`),
          `Last-Modified`(bootUpDate),
          ETag(cssEtag)
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
    case request@GET -> "files" /: file => completeWithGipFile(file.segments.mkString("/"), request)

    // To alow the SPA to work any view will render the index page
    case request@GET -> "view" /: _ => withNotMatchingEtag(request, indexContentEtag) { indexPage(AppConfig.sharedConf) }

    // Default Handler
    case _ => NotFound(/* TODO: Implement Default 404-Page */)
  }
  
  private def appJS(implicit config: Config): String =
    if (config.development.getOrElse(false)) "boinc-webmanager_client-fastopt.js"
    else "boinc-webmanager_client-opt.js"

  private def appDeptJS(implicit config: Config): String =
    if (config.development.getOrElse(false)) "boinc-webmanager_client-jsdeps.js"
    else "boinc-webmanager_client-jsdeps.min.js"

  private def fromResource(file: String, request: Request[IO]) =
    StaticFile.fromResource("/web-root/" + file, Some(request))

  private def fromWebJarResource(root: String, file: String, request: Request[IO]) =
    StaticFile.fromResource(root + file, Some(request))

  private def fromFile(file: String, request: Request[IO])(implicit config: Config) =
    StaticFile.fromString(config.server.webroot + file, Some(request))

  private def completeWithGipFile(file: String, request: Request[IO])(implicit config: Config) = {
    lazy val zipFile =
      if (config.development.getOrElse(false)) fromFile(file + ".gz", request)
      else fromResource(file + ".gz", request)

    lazy val normalFile =
      if (config.development.getOrElse(false)) fromFile(file, request)
      else fromResource(file, request)

    val cType = contentTypes.getOrElse(file.substring(file.lastIndexOf("."), file.length), MediaType.application.`octet-stream`)

    zipFile
      .map(
        _.putHeaders(
          `Content-Encoding`(ContentCoding.gzip),
          `Content-Type`(cType)
        )
      ).getOrElseF(
        normalFile.getOrElseF {
          LOG.error(s"Can not load $file")
          NotFound()
        }
      )
  }

}
