package at.happywetter.boinc.server

import cats.effect.IO


/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.08.2017
  */
object JsonMiddleware {

  import org.http4s._

  def apply(service: HttpService[IO]): HttpService[IO] = Service.lift { req =>
    service(req).map {
      case Status.Successful(response) => response.putHeaders(Header("Content-Type","application/json; charset=utf-8"))
      case response => response
    }
  }

}
