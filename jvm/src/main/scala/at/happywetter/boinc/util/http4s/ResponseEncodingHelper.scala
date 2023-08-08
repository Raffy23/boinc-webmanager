package at.happywetter.boinc.util.http4s

import at.happywetter.boinc.server.{bootUpDate, bootUpTime}
import at.happywetter.boinc.util.http4s.Implicits._
import at.happywetter.boinc.util.http4s.ResponseEncodingHelper.withNotMatchingEtag

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.{ETag, `Content-Type`, `Last-Modified`}
import org.http4s.implicits._
import org.typelevel.ci.CIString
import upickle.default.{Writer, write, writeBinary}

/**
  * Hint: This was caused by updating http4s from 0.18.0 to 0.21.0-M4
  *       and by switching from circe to uPickle (json -> messagepack)
  *
  * Created by:
  *
  * @author Raphael
  * @version 05.07.2019
  */
trait ResponseEncodingHelper:

  private val HEADER_MSGPACK = `Content-Type`(CustomMediaTypes.messagepack)
  private val HEADER_JSON = `Content-Type`(MediaType.application.json)

  @inline protected def Ok[T](f: IO[T], req: Request[IO])(implicit encoder: Writer[T]): IO[Response[IO]] =
    f.flatMap(a => encode(a, req))

  @inline protected def Ok[T](f: T, req: Request[IO])(implicit encoder: Writer[T]): IO[Response[IO]] =
    encode(f, req)

  @inline protected def OkWithEtag[T](f: T, eTag: String, req: Request[IO])(implicit
    encoder: Writer[T]
  ): IO[Response[IO]] =
    encodeWithEtag(f, eTag, req)

  @inline
  private def encode[T](response: T, request: Request[IO])(implicit encoder: Writer[T]): IO[Response[IO]] =
    request.headers.get(CIString("Accept")).map(_.head.value) match
      case Some("application/json")        => org.http4s.dsl.io.Ok(write(response), HEADER_JSON)
      case Some("application/messagepack") => org.http4s.dsl.io.Ok(writeBinary(response), HEADER_MSGPACK)
      case Some("*/*")                     => org.http4s.dsl.io.Ok(writeBinary(response), HEADER_MSGPACK)

      case _ => org.http4s.dsl.io.Ok(write(response), HEADER_JSON)

  def encode[T](status: Status, body: T, request: Request[IO])(implicit encoder: Writer[T]): IO[Response[IO]] =
    IO.pure(
      request.headers.get(CIString("Accept")).map(_.head.value) match {
        case Some("application/json") =>
          Response[IO](
            status = status,
            entity = write(body),
            headers = Headers(HEADER_JSON)
          )
        case _ =>
          Response[IO](
            status = status,
            entity = writeBinary(body),
            headers = Headers(HEADER_MSGPACK)
          )
      }
    )

  private def encodeWithEtag[T](body: T, eTag: String, request: Request[IO])(implicit
    encoder: Writer[T]
  ): IO[Response[IO]] =
    withNotMatchingEtag(request, eTag):
      encode(body, request).map(
        _.putHeaders(
          `Last-Modified`(bootUpDate),
          ETag(eTag)
        )
      )

object ResponseEncodingHelper {

  def withNotMatchingEtag(request: Request[IO], eTag: String)(response: IO[Response[IO]]): IO[Response[IO]] =
    request.headers
      .get(CIString("If-None-Match"))
      .map { etag =>
        if etag.head.value == eTag then IO.pure(Response[IO](status = NotModified))
        else response
      }
      .getOrElse(response)

}
