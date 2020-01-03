package at.happywetter.boinc.web.helper

import at.happywetter.boinc.web.helper.ResponseHelper._
import org.scalajs.dom
import org.scalajs.dom.experimental._
import org.scalajs.dom.raw.{Blob, BlobPropertyBag}
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.typedarray.{TypedArray, Uint8Array}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 23.07.2017
  */
object FetchHelper {

  private val USE_MESSAGE_PACK_FORMAT = true

  val header = new Headers()
  header.append("Content-Type", "application/json")
  header.set("Accept", if(USE_MESSAGE_PACK_FORMAT) "application/messagepack" else "application/json")

  def setToken(token: String): Unit = {
    header.set("X-Authorization", token)
  }

  def get[A](uri: String)(implicit decoder: Reader[A]): Future[A] = {
    dom.console.log("GET", uri)

    Fetch
      .fetch(uri, requestGetParameters())
      .mapData(data => readData[A](data))
  }

  def getCancelable[A](uri: String)(implicit decoder: Reader[A]): FetchRequest[A] = {
    dom.console.log("GET", uri)

    val controller = new AbortController()
    new FetchRequest(
      controller,
      Fetch
        .fetch(uri, requestGetParameters(controller.signal))
        .mapData(data => readData[A](data))
    )
  }

  def post[A,R](uri: String, data: A)(implicit encoder: Writer[A], decoder: Reader[R]): Future[R] = {
    dom.console.log("POST", uri)

    Fetch
      .fetch(uri, requestPostParameters(write(data)))
      .mapData(data => readData[R](data))
  }

  def patch[A](uri: String)(implicit decoder: Reader[A]): Future[A] = {
    dom.console.log("PATCH", uri)

    Fetch
      .fetch(uri, requestPatchParameters())
      .mapData(data => readData[A](data))
  }

  @inline
  private def readData[T](s: Array[Byte])(implicit decoder: Reader[T]): T =
    if (USE_MESSAGE_PACK_FORMAT) readBinary[T](s) else read[T](s)

  class FetchRequest[T](val controller: AbortController, val future: Future[T]) {
    def mapToFuture[S](f: (AbortController, T) => S)(implicit executor: ExecutionContext): Future[S] =
      future.map(data => f(controller, data))
  }

  private def requestGetParameters(_signal: UndefOr[AbortSignal] = js.undefined): RequestInit = new RequestInit {
    override var method: UndefOr[HttpMethod] = HttpMethod.GET
    override var headers: UndefOr[HeadersInit] = header
    override var body: UndefOr[BodyInit] = js.undefined
    override var referrer: UndefOr[String] = js.undefined
    override var referrerPolicy: UndefOr[ReferrerPolicy] = js.undefined
    override var mode: UndefOr[RequestMode] = js.undefined
    override var credentials: UndefOr[RequestCredentials] = js.undefined
    override var cache: UndefOr[RequestCache] = js.undefined
    override var redirect: UndefOr[RequestRedirect] = js.undefined
    override var integrity: UndefOr[String] = js.undefined
    override var keepalive: UndefOr[Boolean] = js.undefined
    override var signal: UndefOr[AbortSignal] = _signal
    override var window: UndefOr[Null] = js.undefined
  }

  import js.JSConverters._
  private def requestPostParameters(content: String): RequestInit = new RequestInit {
    override var method: UndefOr[HttpMethod] = HttpMethod.POST
    override var headers: UndefOr[HeadersInit] = header
    override var body: UndefOr[BodyInit] = content
    override var referrer: UndefOr[String] = js.undefined
    override var referrerPolicy: UndefOr[ReferrerPolicy] = js.undefined
    override var mode: UndefOr[RequestMode] = js.undefined
    override var credentials: UndefOr[RequestCredentials] = js.undefined
    override var cache: UndefOr[RequestCache] = js.undefined
    override var redirect: UndefOr[RequestRedirect] = js.undefined
    override var integrity: UndefOr[String] = js.undefined
    override var keepalive: UndefOr[Boolean] = js.undefined
    override var signal: UndefOr[AbortSignal] = js.undefined
    override var window: UndefOr[Null] = js.undefined
  }

  // WTF?
  private def requestPostParameters(content: Array[Byte]): RequestInit = new RequestInit {
    override var method: UndefOr[HttpMethod] = HttpMethod.POST
    override var headers: UndefOr[HeadersInit] = header
    override var body: UndefOr[BodyInit] = content.map(_.toShort).mkString("")
    override var referrer: UndefOr[String] = js.undefined
    override var referrerPolicy: UndefOr[ReferrerPolicy] = js.undefined
    override var mode: UndefOr[RequestMode] = js.undefined
    override var credentials: UndefOr[RequestCredentials] = js.undefined
    override var cache: UndefOr[RequestCache] = js.undefined
    override var redirect: UndefOr[RequestRedirect] = js.undefined
    override var integrity: UndefOr[String] = js.undefined
    override var keepalive: UndefOr[Boolean] = js.undefined
    override var signal: UndefOr[AbortSignal] = js.undefined
    override var window: UndefOr[Null] = js.undefined
  }

  private def requestPatchParameters(content: UndefOr[BodyInit] = js.undefined): RequestInit = new RequestInit {
    override var method: UndefOr[HttpMethod] = HttpMethod.PATCH
    override var headers: UndefOr[HeadersInit] = header
    override var body: UndefOr[BodyInit] = content
    override var referrer: UndefOr[String] = js.undefined
    override var referrerPolicy: UndefOr[ReferrerPolicy] = js.undefined
    override var mode: UndefOr[RequestMode] = js.undefined
    override var credentials: UndefOr[RequestCredentials] = js.undefined
    override var cache: UndefOr[RequestCache] = js.undefined
    override var redirect: UndefOr[RequestRedirect] = js.undefined
    override var integrity: UndefOr[String] = js.undefined
    override var keepalive: UndefOr[Boolean] = js.undefined
    override var signal: UndefOr[AbortSignal] = js.undefined
    override var window: UndefOr[Null] = js.undefined
  }

}
