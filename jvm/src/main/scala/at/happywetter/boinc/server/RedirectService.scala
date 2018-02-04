package at.happywetter.boinc.server

import at.happywetter.boinc.AppConfig.Config
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s.headers.Host

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.09.2017
  */
object RedirectService {
  import cats.effect._
  import org.http4s._
  import org.http4s.dsl.io._

  def apply(config: Config):  HttpService[IO] = HttpService[IO] {
    case request =>
      request.headers.get(Host) match {
        case Some(Host(host, _)) =>
          MovedPermanently(buildUri(request, host, config.server.port).withPath(request.uri.path))
        case _ => BadRequest()
      }
  }

  private def buildUri(request: Request[IO], host: String, securePort: Int) = request.uri.copy(
      scheme    = Some(Scheme.https),
      authority = Some(
        Authority(
          request.uri.authority.flatMap(_.userInfo),
          RegName(host),
          port = Some(securePort)
        )
      )
    )

}
