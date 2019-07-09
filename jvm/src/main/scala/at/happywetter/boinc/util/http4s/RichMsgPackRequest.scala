package at.happywetter.boinc.util.http4s

import cats.{Applicative, Monad}
import org.http4s.dsl.io._
import org.http4s.{EntityDecoder, Request, Response}
import upickle.default.{Reader, readBinary, read}

import scala.language.higherKinds
import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.07.2019
  */
object RichMsgPackRequest {

  implicit class RichMsgPacKResponse[F[_]: Applicative](request: Request[F]) {

    def decodeMessagePack[T](f: T => F[Response[F]])(implicit F: Monad[F], contentReader: Reader[T], entityDecoder: EntityDecoder[F, Array[Byte]]): F[Response[F]] = {
      println(contentReader.getClass)
      request.decode[Array[Byte]] { body =>
        println(new String(body))
        println(body.mkString(""))
        Try(readBinary(body)).map(f).recover{ case ex: Exception => ex.printStackTrace(); F.pure(Response[F](status = InternalServerError))}.get
      }
    }

    def decodeJson[T](f: T => F[Response[F]])(implicit F: Monad[F], contentReader: Reader[T], entityDecoder: EntityDecoder[F, Array[Byte]]): F[Response[F]] = {
      request.decode[Array[Byte]] { body =>
        Try(read(body)).map(f).recover{ case ex: Exception => ex.printStackTrace(); F.pure(Response[F](status = InternalServerError))}.get
      }
    }

  }

}
