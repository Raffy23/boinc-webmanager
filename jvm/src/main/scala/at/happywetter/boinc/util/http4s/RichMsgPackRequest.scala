package at.happywetter.boinc.util.http4s

import cats.effect.IO
import cats.{Applicative, Monad}
import org.http4s.dsl.io._
import org.http4s.{EntityDecoder, Header, Headers, Request, Response}
import upickle.default.{Reader, read, readBinary}

import scala.language.higherKinds
import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.07.2019
  */
object RichMsgPackRequest {

  private val HEADER_MSGPACK = "application/messagepack"
  private val HEADER_JSON    = "application/json; charset=utf-8"

  implicit class RichMsgPacKResponse[F[_]: Applicative](request: Request[F]) {

    def decodeMessagePack[T](f: T => F[Response[F]])(implicit F: Monad[F], contentReader: Reader[T], entityDecoder: EntityDecoder[F, Array[Byte]]): F[Response[F]] =
      request.decode[Array[Byte]] { body =>
        Try(readBinary(body))
          .map(f)
          .map(resp => F.map(resp)(_.withHeaders(Headers.of(Header("Content-Type", HEADER_MSGPACK)))))
          .recover{ case ex: Exception => ex.printStackTrace(); F.pure(Response[F](status = InternalServerError)) }
          .get
      }

    def decodeJson[T](f: T => F[Response[F]])(implicit F: Monad[F], contentReader: Reader[T], entityDecoder: EntityDecoder[F, Array[Byte]]): F[Response[F]] =
      request.decode[Array[Byte]] { body =>
        Try(read(body))
          .map(f)
          .map(resp => F.map(resp)(_.withHeaders(Headers.of(Header("Content-Type", HEADER_JSON)))))
          .recover{ case ex: Exception => ex.printStackTrace(); F.pure(Response[F](status = InternalServerError))}
          .get
      }

  }

}
