package at.happywetter.boinc.web.helper

import at.happywetter.boinc.web.helper.ResponseHelper._
import org.scalajs.dom
import org.scalajs.dom.experimental._
import org.scalajs.dom.raw.{Blob, BlobPropertyBag}
import upickle.default._

import scala.concurrent.Future
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

  val header = new Headers()
  header.append("Content-Type", "application/json")
  header.set("Accept", "application/messagepack")

  def setToken(token: String): Unit = {
    header.set("X-Authorization", token)
  }

  def get[A](uri: String)(implicit decoder: Reader[A]): Future[A] = {
    dom.console.log("Fetch ", uri)
    Fetch
      .fetch(uri, requestGetParameters)
      .mapData(data => readBinary[A](data))
  }

  def post[A,R](uri: String, data: A)(implicit encoder: Writer[A], decoder: Reader[R]): Future[R] =
    Fetch
      .fetch(uri, requestPostParameters(write(data)))
      .mapData(data => readBinary[R](data))

  def patch[A](uri: String)(implicit decoder: Reader[A]): Future[A] =
    Fetch
      .fetch(uri, requestPatchParameters())
      .mapData(data => readBinary[A](data))

  private def requestGetParameters: RequestInit = new RequestInit {
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
    override var signal: UndefOr[AbortSignal] = js.undefined
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
