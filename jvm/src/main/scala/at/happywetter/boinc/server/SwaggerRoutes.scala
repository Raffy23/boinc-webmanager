package at.happywetter.boinc.server

import cats.effect.IO
import org.http4s.dsl.io._
import org.http4s.headers.{Location, `Content-Type`, `Last-Modified`}
import org.http4s.{Charset, Headers, HttpRoutes, MediaType, Response, StaticFile, Uri}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 30.06.2020
 */
object SwaggerRoutes {

  private val SWAGGER_VERSION = "4.1.3"

  def redirectToEndpoint(): IO[Response[IO]] = {
    //val queryParameters = Map("url" -> Seq("swagger-config.yaml"))
    Uri
      .fromString(s"/swagger/")
      // .map(uri => uri.setQueryParams(queryParameters))
      .map(uri => PermanentRedirect(Location(uri)))
      .getOrElse(NotFound())
  }

  def apply(): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root =>
      Ok(indexHtmlContent,
        `Content-Type`(MediaType.text.html, Charset.`UTF-8`),
        `Last-Modified`(bootUpDate),
      )

    case GET -> Root / file ~ "yaml" =>
      StaticFile
        .fromResource[IO](s"/swagger/$file.yaml")
        .getOrElseF(NotFound())

    case GET -> Root / file =>
      val filePath = s"/META-INF/resources/webjars/swagger-ui/$SWAGGER_VERSION/$file"
      StaticFile
        .fromResource[IO](filePath)
        .getOrElseF(NotFound())

  }

  private val indexHtmlContent =
    """<!-- Custom version of swagger-ui index.html -->
      |<!-- Content copied from original index.html -->
      |<!DOCTYPE html>
      |<html lang="en">
      |  <head>
      |    <meta charset="UTF-8">
      |    <title>Swagger UI</title>
      |    <link rel="stylesheet" type="text/css" href="/swagger/swagger-ui.css" />
      |    <link rel="icon" type="image/png" href="/swagger/favicon-32x32.png" sizes="32x32" />
      |    <link rel="icon" type="image/png" href="/swagger/favicon-16x16.png" sizes="16x16" />
      |    <style>
      |      html
      |      {
      |        box-sizing: border-box;
      |        overflow: -moz-scrollbars-vertical;
      |        overflow-y: scroll;
      |      }
      |
      |      *,
      |      *:before,
      |      *:after
      |      {
      |        box-sizing: inherit;
      |      }
      |
      |      body
      |      {
      |        margin:0;
      |        background: #fafafa;
      |      }
      |    </style>
      |  </head>
      |
      |  <body>
      |    <div id="swagger-ui"></div>
      |
      |    <script src="/swagger/swagger-ui-bundle.js" charset="UTF-8"> </script>
      |    <script src="/swagger/swagger-ui-standalone-preset.js" charset="UTF-8"> </script>
      |    <script>
      |    window.onload = function() {
      |      // Begin Swagger UI call region
      |      const ui = SwaggerUIBundle({
      |        url: "/swagger/swagger-config.yaml",
      |        dom_id: '#swagger-ui',
      |        deepLinking: true,
      |        presets: [
      |          SwaggerUIBundle.presets.apis,
      |          SwaggerUIStandalonePreset
      |        ],
      |        plugins: [
      |          SwaggerUIBundle.plugins.DownloadUrl
      |        ],
      |        layout: "StandaloneLayout"
      |      });
      |      // End Swagger UI call region
      |
      |      window.ui = ui;
      |    };
      |  </script>
      |  </body>
      |</html>
      |""".stripMargin

}
