package at.happywetter.boinc.web.helper

import at.happywetter.boinc.shared.ApplicationError
import at.happywetter.boinc.web.boincclient.FetchResponseException
import org.scalajs.dom.experimental.Response
import prickle.Unpickle

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 12.09.2017
  */
object ResponseHelper {

  implicit class ErrorResponseFeature(response: Response) {

    @throws(classOf[FetchResponseException])
    def tryGet: Future[String] = {
      response
        .text()
        .toFuture
        .recover {
          case ex: Exception =>
            ex.printStackTrace()
            throw FetchResponseException(
              response.status,
              ApplicationError("error_internal_error")
            )
        }
        .map( content => {
        if (response.status != 200)
          throw FetchResponseException(
            response.status,
            Unpickle[ApplicationError]
              .fromString(content)
              .getOrElse(ApplicationError("error_decoding_msg"))
          )

          content
        })
    }
  }


}