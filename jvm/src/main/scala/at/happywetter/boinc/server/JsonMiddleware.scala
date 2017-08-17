package at.happywetter.boinc.server

import org.http4s.{Header, HttpService, Service}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.08.2017
  */
object JsonMiddleware {

  def apply(service: HttpService): HttpService = Service.lift { req =>
    service(req).map { response =>
      response.putHeaders(Header("Content-Type","application/json"))
    }
  }

}
