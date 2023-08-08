package at.happywetter.boinc.util.http4s

import scala.language.higherKinds
import scala.util.Try

import cats.{Applicative, Monad}
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.`Content-Type`
import upickle.default.{Reader, read, readBinary}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.07.2019
  */
object RichMsgPackRequest:

  private val HEADER_MSGPACK = `Content-Type`(CustomMediaTypes.messagepack)
  private val HEADER_JSON = `Content-Type`(MediaType.application.json)

  implicit class RichMsgPacKResponse[F[_]: Applicative](request: Request[F]):

    def decodeMessagePack[T](
      f: T => F[Response[F]]
    )(implicit F: Monad[F], contentReader: Reader[T], entityDecoder: EntityDecoder[F, Array[Byte]]): F[Response[F]] =
      request.decode[F, Array[Byte]] { body =>
        Try(readBinary(body))
          .map(f)
          // .map(resp => F.map(resp)(_.withHeaders(Headers(HEADER_MSGPACK))))
          .recover { case ex: Exception => ex.printStackTrace(); F.pure(Response[F](status = InternalServerError)) }
          .get
      }

    def decodeJson[T](
      f: T => F[Response[F]]
    )(implicit F: Monad[F], contentReader: Reader[T], entityDecoder: EntityDecoder[F, Array[Byte]]): F[Response[F]] =
      request.decode[F, Array[Byte]] { body =>
        Try(read(body))
          .map(f)
          // .map(resp => F.map(resp)(_.withHeaders(Headers(HEADER_JSON))))
          .recover { case ex: Exception => ex.printStackTrace(); F.pure(Response[F](status = InternalServerError)) }
          .get
      }
