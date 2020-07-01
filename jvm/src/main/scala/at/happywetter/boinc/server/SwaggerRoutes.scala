package at.happywetter.boinc.server

import at.happywetter.boinc.util.IOAppTimer
import cats.effect.{ContextShift, IO}
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.{HttpRoutes, Response, StaticFile, Uri}

import scala.concurrent.ExecutionContext

/**
 * Created by: 
 *
 * @author Raphael
 * @version 30.06.2020
 */
object SwaggerRoutes {

  /**
   * A blocker that is used for all actions that can be blocking.
   */
  private val blocker = IOAppTimer.blocker

  /**
   * We use the default execution context as ContextShift
   */
  private implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)


  def redirectToEndpoint(): IO[Response[IO]] = {
    val queryParameters = Map("url" -> Seq("swagger-config.yaml"))
    Uri
      .fromString(s"/swagger/index.html")
      .map(uri => uri.setQueryParams(queryParameters))
      .map(uri => PermanentRedirect(Location(uri)))
      .getOrElse(NotFound())
  }

  def apply(): HttpRoutes[IO] = HttpRoutes.of[IO] {

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
