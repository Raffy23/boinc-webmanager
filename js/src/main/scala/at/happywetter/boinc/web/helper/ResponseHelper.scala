package at.happywetter.boinc.web.helper

import at.happywetter.boinc.shared.webrpc.ApplicationError
import at.happywetter.boinc.web.boincclient.FetchResponseException
import org.scalajs.dom
import org.scalajs.dom.experimental.Response

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.typedarray.Int8Array
import at.happywetter.boinc.shared.parser._

import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 12.09.2017
  */
object ResponseHelper {

  implicit class ErrorResponseFeature(response: Response) {
    import upickle.default._

    @throws(classOf[FetchResponseException])
    def tryGet: Future[Array[Byte]] = {
      response
        .arrayBuffer()
        .toFuture
        .map(jsArr => actionStatus(new Int8Array(jsArr).toArray))
        .recover(exceptionHandler)
    }

    @throws(classOf[FetchResponseException])
    def tryGetString: Future[String] = {
      response
        .text()
        .toFuture
        .recover(exceptionHandler)
    }

    private def exceptionHandler[T]: PartialFunction[Throwable, T] = {
      case ex: Exception =>
        dom.console.log("Error in tryGet: " + ex.getMessage)
        //ex.printStackTrace()
        throw FetchResponseException(
          response.status,
          ApplicationError("error_internal_error")
        )
    }

    @throws(classOf[FetchResponseException])
    private def actionStatus(content: Array[Byte]): Array[Byte] =  {
      if (response.status != 200)
        throw FetchResponseException(
          response.status,

          // If content is empty ScalaJS throws a non-catchable exception
          if (content.nonEmpty) Try(readBinary[ApplicationError](content)).getOrElse(ApplicationError("error_decoding_msg"))
          else ApplicationError("error_decoding_msg")
        )

      content
    }

  }

  implicit class RichPromise(promise: js.Promise[Response]) {
    def mapData[T]( mapper: Array[Byte] => T ): Future[T] =
      promise.toFuture.flatMap(_.tryGet).map(mapper)
  }

}
