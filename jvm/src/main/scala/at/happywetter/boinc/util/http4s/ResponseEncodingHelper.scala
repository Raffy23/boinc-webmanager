package at.happywetter.boinc.util.http4s

import cats.effect.{ContextShift, IO}
import org.http4s.dsl.io._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Headers, Request, Response, Status}
import upickle.default.{Writer, write, writeBinary}
import at.happywetter.boinc.util.http4s.Implicits._
import org.typelevel.ci.CIString

import scala.concurrent.{ExecutionContext, Future}

/**
  * Hint: This was caused by updating http4s from 0.18.0 to 0.21.0-M4
  *       and by switching from circe to uPickle (json -> messagepack)
  *
  * Created by:
  *
  * @author Raphael
  * @version 05.07.2019
  */
trait ResponseEncodingHelper {

  protected implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private val HEADER_MSGPACK = Header("Content-Type", "application/messagepack")
  private val HEADER_JSON    = Header("Content-Type", "application/json")

  @inline protected def Ok[T](f: Future[T], req: Request[IO])(implicit encoder: Writer[T]): IO[Response[IO]] =
    IO.fromFuture( IO(f) ).flatMap(a => encode(a, req))

  @inline protected def Ok[T](f: IO[T], req: Request[IO])(implicit encoder: Writer[T]): IO[Response[IO]] =
    f.flatMap(a => encode(a, req))

  @inline protected def Ok[T](f: T, req: Request[IO])(implicit encoder: Writer[T]): IO[Response[IO]] =
    encode(f, req)

  @inline
  private def encode[T](response: T, request: Request[IO])(implicit encoder: Writer[T]): IO[Response[IO]] =
    request.headers.get(CIString("Accept")).map(_.value) match {
      case Some("application/json")        => org.http4s.dsl.io.Ok(write(response), HEADER_JSON)
      case Some("application/messagepack") => org.http4s.dsl.io.Ok(writeBinary(response), HEADER_MSGPACK)
      case Some("*/*")                     => org.http4s.dsl.io.Ok(writeBinary(response), HEADER_MSGPACK)

      case _ => org.http4s.dsl.io.Ok(write(response), HEADER_JSON)
    }

  def encode[T](status: Status, body: T, request: Request[IO])(implicit encoder: Writer[T]): IO[Response[IO]] =
    IO.pure(
      request.headers.get(CIString("Accept")).map(_.value) match {
        case Some("application/json")        => new Response[IO](status = status, body = write(body), headers = Headers.of(HEADER_JSON))
        case Some("application/messagepack") => new Response[IO](status = status, body = writeBinary(body), headers = Headers.of(HEADER_MSGPACK))
        case Some("*/*")                     => new Response[IO](status = status, body = writeBinary(body), headers = Headers.of(HEADER_MSGPACK))

        case _ => new Response[IO](status = status, body = writeBinary(body), headers = Headers.of(HEADER_MSGPACK))
      }
    )


}