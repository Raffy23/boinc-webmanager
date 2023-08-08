package at.happywetter.boinc.web.util

import org.scalajs.dom
import org.scalajs.dom.Response
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.Int8Array
import scala.util.Try
import scalajs.concurrent.JSExecutionContext.Implicits.queue

import at.happywetter.boinc.shared.boincrpc.ApplicationError
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.web.boincclient.FetchResponseException
import at.happywetter.boinc.web.pages.LoginPage
import at.happywetter.boinc.web.routes.AppRouter

/**
  * Created by:
  *
  * @author Raphael
  * @version 12.09.2017
  */
object ResponseHelper:

  implicit class ErrorResponseFeature(response: Response):
    import upickle.default._

    @throws(classOf[FetchResponseException])
    def tryGet: Future[Array[Byte]] =
      response
        .arrayBuffer()
        .toFuture
        .map(jsArr => actionStatus(new Int8Array(jsArr).toArray))
        .recover(exceptionHandler)

    @throws(classOf[FetchResponseException])
    def tryGetString: Future[String] =
      response
        .text()
        .toFuture
        .recover(exceptionHandler)

    private def exceptionHandler[T]: PartialFunction[Throwable, T] =
      case ex: Exception =>
        dom.console.log("Error can not fetch, reason: " + ex.getMessage)

        if (response.status == 401)
          AuthClient.validateSavedCredentials().map { succ =>
            if (!succ)
              AppRouter.navigate(LoginPage.link)
          }

          throw FetchResponseException(response.status, ApplicationError("not_authenticated"))

        throw FetchResponseException(response.status, ApplicationError("error_internal_error"))

    @throws(classOf[FetchResponseException])
    private def actionStatus(content: Array[Byte]): Array[Byte] =
      if (response.status != 200)
        throw FetchResponseException(
          response.status,

          // If content is empty ScalaJS throws a non-catchable exception
          if (content.nonEmpty)
            Try(readBinary[ApplicationError](content)).getOrElse(ApplicationError("error_decoding_msg"))
          else ApplicationError("error_decoding_msg")
        )

      content

  implicit class RichPromise(promise: js.Promise[Response]):
    def mapData[T](mapper: Array[Byte] => T): Future[T] =
      promise.toFuture.flatMap(_.tryGet).map(mapper)
