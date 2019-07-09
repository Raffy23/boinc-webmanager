package at.happywetter.boinc.util.http4s

import cats.effect.{ContextShift, IO}
import org.http4s.{Header, Response}
import upickle.default.{writeBinary, Writer}
import org.http4s.dsl.io._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Hint: This was caused by updating http4s from 0.18.0 to 0.21.0-M4
  *       and by switching from circe to uPickle (json -> messagepack)
  *
  * Created by:
  *
  * @author Raphael
  * @version 05.07.2019
  */
trait MsgPackRequRespHelper {

  protected implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  private val header = Header("Content-Type", "application/messagepack")

  @inline protected def Ok[T](f: Future[T])(implicit encoder: Writer[T]): IO[Response[IO]] =
    IO.fromFuture( IO( f.map(x => writeBinary(x)) ))
      .flatMap(f => org.http4s.dsl.io.Ok(f, header))

  @inline protected def Ok[T](f: IO[T])(implicit encoder: Writer[T]): IO[Response[IO]] =
    f.flatMap(f => org.http4s.dsl.io.Ok(writeBinary(f), header))

  @inline protected def Ok[T](f: T)(implicit encoder: Writer[T]): IO[Response[IO]] =
    org.http4s.dsl.io.Ok(writeBinary(f), header)

}