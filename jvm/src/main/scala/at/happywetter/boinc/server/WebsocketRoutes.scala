package at.happywetter.boinc.server

import at.happywetter.boinc.BoincManager
import at.happywetter.boinc.shared.boincrpc.ApplicationError
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.websocket._
import at.happywetter.boinc.util.http4s.Implicits._
import cats.effect.IO
import cats.effect.std.{Queue, Supervisor}
import fs2.Pipe
import org.http4s.dsl.io._
import org.http4s.server.websocket._
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame._
import org.http4s.{HttpRoutes, Response}
import scodec.bits.ByteVector
import upickle.default.{Writer, readBinary, writeBinary}

import scala.language.implicitConversions

/**
  * Created by: 
  *
  * @author Raphael
  * @version 16.07.2019
  */
object WebsocketRoutes {

  private case class State(supervisor: Supervisor[IO], finalizer: IO[Unit], callback: BoincManager => IO[Unit])
  private case class Client(token: String, responseQueue: Queue[IO, Option[WebSocketFrame]], state: State)
  private object Client {
    def apply(token: String): IO[Client] = {
      for {
        supervisor <- Supervisor[IO].allocated
        result     <- Queue
          .unbounded[IO, Option[WebSocketFrame]]
          .map { responseQueue =>
            val callback: BoincManager => IO[Unit] = manager => for {
              names <- manager.getAllHostNames
              groups <- manager.getSerializableGroups

              _ <- responseQueue.offer(
                Some(HostInformationChanged(names, groups))
              )
            } yield ()

            new Client(token, responseQueue, State(supervisor._1, supervisor._2, callback))
          }
      } yield result
    }
  }

  private object JWTToken extends QueryParamDecoderMatcher[String]("token")

  def apply(authService: AuthenticationService, boincManager: BoincManager): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root :? JWTToken(token) if !authService.validate(token).getOrElse(false) =>
      IO.pure(Response[IO](status = Unauthorized, body = writeBinary(ApplicationError("error_no_token"))))

    case GET -> Root :? JWTToken(token) if authService.validate(token).getOrElse(false) =>

      Client(token).flatMap { client =>
        val clientReply: PartialFunction[WebSocketFrame, IO[WebSocketFrame]] = {
          case Binary(msg, _) => readBinary[WebSocketMessage](msg.toArray) match {

            case SubscribeToGroupChanges =>
              boincManager
                .changeListener
                .contains(client.state.callback)
                .ifM(
                  IO.pure(NACK),
                  IO {
                    boincManager.changeListener.register(client.state.callback)
                    ACK
                  }
                )

            case UnsubscribeToGroupChanges =>
              boincManager
                .changeListener
                .contains(client.state.callback)
                .ifM(
                  boincManager.changeListener.unregister(client.state.callback).map(_ => ACK),
                  IO.pure(NACK)
                )
          }

          case Close(_) =>
            boincManager
              .changeListener
              .contains(client.state.callback)
              .ifM(
                boincManager.changeListener.unregister(client.state.callback),
                IO.unit
              )
              .flatMap(_ => client.state.finalizer)
              .as(CloseGracefully)

          case _ =>
            boincManager
              .changeListener
              .contains(client.state.callback)
              .ifM(
                boincManager.changeListener.unregister(client.state.callback),
                IO.unit
              )
              .flatMap(_ => client.state.finalizer)
              .as(CloseWithUnknownRequst)

        }

        val receive: Pipe[IO, WebSocketFrame, Unit] =
          _.collect {
            case payload =>
                client.state.supervisor.supervise(
                  clientReply
                    .lift(payload)
                    .map { responseIO =>
                      responseIO.flatMap(response =>
                        client.responseQueue.offer(Some(response))
                      )
                    }
                    .getOrElse(IO.unit)
                )
          }

        val send = fs2.Stream.fromQueueNoneTerminated(client.responseQueue)

        WebSocketBuilder[IO].build(send, receive)
      }

  }

  private implicit def convertWebSocketMessageToWebSocketFrame[T <: WebSocketMessage](msg: T)(implicit writer: Writer[T]): WebSocketFrame =
    Binary(ByteVector(writeBinary[T](msg)))

  private val CloseGracefully = Close(1000).toOption.get
  private val CloseWithUnknownRequst = Close(1008).toOption.get

}
