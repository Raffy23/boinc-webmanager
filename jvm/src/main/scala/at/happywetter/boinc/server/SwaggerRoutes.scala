package at.happywetter.boinc.server

import cats.effect.{Blocker, ContextShift, IO}
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.{HttpRoutes, Response, StaticFile, Uri}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 30.06.2020
 */
object SwaggerRoutes {

  def redirectToEndpoint(): IO[Response[IO]] = {
    val queryParameters = Map("url" -> Seq("swagger-config.yaml"))
    Uri
      .fromString(s"/swagger/index.html")
      .map(uri => uri.setQueryParams(queryParameters))
      .map(uri => PermanentRedirect(Location(uri)))
      .getOrElse(NotFound())
  }

  def apply(blocker: Blocker)(implicit cS: ContextShift[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case path@GET -> Root =>
      val queryParameters = Map("url" -> Seq("swagger-config.yaml"))
      Uri
        .fromString(s"${path.uri}/index.html")
        .map(uri => uri.setQueryParams(queryParameters))
        .map(uri => PermanentRedirect(Location(uri)))
        .getOrElse(NotFound())

    case GET -> Root / file ~ "yaml" =>
      StaticFile
        .fromResource[IO](s"/swagger/$file.yaml", blocker)
        .getOrElseF(NotFound())

    case GET -> Root / file =>
      val filePath = s"/META-INF/resources/webjars/swagger-ui/3.25.0/$file"
      StaticFile
        .fromResource[IO](filePath, blocker)
        .getOrElseF(NotFound())

  }

}
