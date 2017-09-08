package at.happywetter.boinc.server


/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.08.2017
  */
object JsonMiddleware {

  import org.http4s._

  def apply(service: HttpService): HttpService = Service.lift { req =>
    service(req).map {
      case Status.Successful(response) => response.putHeaders(Header("Content-Type","application/json"))
      case response => response
    }
  }

}
