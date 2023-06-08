package at.happywetter.boinc.web.util

import org.scalajs.dom
import org.scalajs.dom.AbortController
import org.scalajs.dom.AbortSignal
import org.scalajs.dom.BodyInit
import org.scalajs.dom.Fetch
import org.scalajs.dom.Headers
import org.scalajs.dom.HttpMethod
import org.scalajs.dom.RequestInit
import upickle.default.Reader
import upickle.default.Writer
import upickle.default.read
import upickle.default.readBinary
import upickle.default.write

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.UndefOr

import ResponseHelper._

/**
  * Created by:
  *
  * @author Raphael
  * @version 23.07.2017
  */
object FetchHelper:

  private val USE_MESSAGE_PACK_FORMAT = true

  val header = new Headers()
  header.append("Content-Type", "application/json")
  header.set("Accept", if (USE_MESSAGE_PACK_FORMAT) "application/messagepack" else "application/json")

  def setToken(token: String): Unit =
    header.set("Authorization", "Bearer " + token)

  def hasToken: Boolean =
    header.get("Authorization") != null && header.get("Authorization").nonEmpty

  def get[A](uri: String)(implicit decoder: Reader[A]): Future[A] =
    dom.console.log("GET", uri)

    Fetch
      .fetch(uri, requestGetParameters())
      .mapData(data => readData[A](data))

  def getCancelable[A](uri: String)(implicit decoder: Reader[A]): FetchRequest[A] =
    dom.console.log("GET", uri)

    val controller = new AbortController()
    new FetchRequest(
      controller,
      Fetch
        .fetch(uri, requestGetParameters(controller.signal))
        .mapData(data => readData[A](data))
    )

  def post[A, R](uri: String, data: A)(implicit encoder: Writer[A], decoder: Reader[R]): Future[R] =
    dom.console.log("POST", uri)

    Fetch
      .fetch(uri, requestPostParameters(write(data)))
      .mapData(data => readData[R](data))

  def patch[A](uri: String)(implicit decoder: Reader[A]): Future[A] =
    dom.console.log("PATCH", uri)

    Fetch
      .fetch(uri, requestPatchParameters())
      .mapData(data => readData[A](data))

  def patch[A, R](uri: String, data: A)(implicit encoder: Writer[A], decoder: Reader[R]): Future[R] =
    dom.console.log("PATCH", uri)

    Fetch
      .fetch(uri, requestPatchParameters(write(data)))
      .mapData(data => readData[R](data))

  def delete[R](uri: String)(implicit decoder: Reader[R]): Future[R] =
    dom.console.log("DELETE", uri)

    Fetch
      .fetch(uri, requestDeleteParameters())
      .mapData(data => readData[R](data))

  @inline
  private def readData[T](s: Array[Byte])(implicit decoder: Reader[T]): T =
    if (USE_MESSAGE_PACK_FORMAT) readBinary[T](s) else read[T](s)

  class FetchRequest[T](val controller: AbortController, val future: Future[T]):
    def mapToFuture[S](f: (AbortController, T) => S)(implicit executor: ExecutionContext): Future[S] =
      future.map(data => f(controller, data))

  private def requestGetParameters(_signal: UndefOr[AbortSignal] = js.undefined) = new RequestInit:
    this.method = HttpMethod.GET
    this.headers = header
    this.signal = _signal

  private def requestPostParameters(content: String) = new RequestInit:
    this.method = HttpMethod.POST
    this.headers = header
    this.body = content

  private def requestPatchParameters(content: UndefOr[BodyInit] = js.undefined) = new RequestInit:
    this.method = HttpMethod.PATCH
    this.headers = header
    this.body = content

  private def requestDeleteParameters(content: UndefOr[BodyInit] = js.undefined) = new RequestInit:
    this.method = HttpMethod.DELETE
    this.headers = header
    this.body = content
