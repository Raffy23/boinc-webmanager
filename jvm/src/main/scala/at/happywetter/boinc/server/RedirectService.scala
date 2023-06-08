package at.happywetter.boinc.server

import at.happywetter.boinc.AppConfig.Config
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s.headers.{Host, Location}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.09.2017
  */
object RedirectService:
  import cats.effect._
  import org.http4s._
  import org.http4s.dsl.io._

  def apply(config: Config): HttpRoutes[IO] = HttpRoutes.of[IO]:
    case request =>
      request.headers.get[Host] match
        case Some(Host(host, _)) => MovedPermanently(buildUri(request, host, config.server.port))
        case _                   => BadRequest()

  private def buildUri(request: Request[IO], host: String, securePort: Int) = Location(
    request.uri
      .copy(
        scheme = Some(Scheme.https),
        authority = Some(
          Authority(
            request.uri.authority.flatMap(_.userInfo),
            RegName(host),
            port = Some(securePort)
          )
        )
      )
      .withPath(request.uri.path)
  )
