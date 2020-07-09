package at.happywetter.boinc.server

import at.happywetter.boinc.BoincManager
import at.happywetter.boinc.shared.boincrpc.ApplicationError
import cats.effect.{Concurrent, IO}
import fs2.Pipe
import fs2.concurrent.Queue
import org.http4s.{HttpRoutes, Response}
import org.http4s.dsl.io._
import org.http4s.server.websocket._
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame._

import scodec.bits.ByteVector
import upickle.default.{readBinary, writeBinary, Writer}
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.websocket._
import at.happywetter.boinc.util.http4s.Implicits._
import scala.concurrent.duration._
import at.happywetter.boinc.util.IOAppTimer.timer

import scala.language.implicitConversions

/**
  * Created by: 
  *
  * @author Raphael
  * @version 16.07.2019
  */
object WebsocketRoutes {

  private case class State(callback: BoincManager => Unit)
  private case class Client(token: String, responseQueue: Queue[IO, WebSocketFrame], state: State)
  private object Client {
    def apply(token: String)(implicit concurrent: Concurrent[IO]): Client = {
      val responseQueue = Queue.unbounded[IO, WebSocketFrame].unsafeRunSync()
      val callback: BoincManager => Unit = manager => {
        responseQueue
          .enqueue1(HostInformationChanged(manager.getAllHostNames.toList, manager.getSerializableGroups))
          .unsafeRunAsyncAndForget()
      }

      new Client(token, responseQueue, State(callback))
    }
  }

  private object JWTToken extends QueryParamDecoderMatcher[String]("token")

  def apply(authService: AuthenticationService, boincManager: BoincManager)(implicit c: Concurrent[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root :? JWTToken(token) if !authService.validate(token).getOrElse(false) =>
      IO.pure(new Response[IO](status = Unauthorized, body = writeBinary(ApplicationError("error_no_token"))))

    case GET -> Root :? JWTToken(token) if authService.validate(token).getOrElse(false) =>
      val client = Client(token)
      val echoReply: Pipe[IO, WebSocketFrame, WebSocketFrame] =
        _.collect {
          case Binary(msg, _) => readBinary[WebSocketMessage](msg.toArray) match {

            case SubscribeToGroupChanges if boincManager.versionChangeListeners.contains(client.state.callback) => NACK
            case SubscribeToGroupChanges =>
              boincManager.versionChangeListeners.add(client.state.callback)
              println("Subscribed!")
              ACK

            case UnsubscribeToGroupChanges =>
              if (boincManager.versionChangeListeners.remove(client.state.callback)) ACK else NACK

          }

          case Close(_) =>
            boincManager.versionChangeListeners.remove(client.state.callback)
            CloseGracefully

          case _ => CloseWithUnknownRequst
        }

      Queue
        .unbounded[IO, WebSocketFrame]
        .flatMap { q =>
          val d = q.dequeue.through(echoReply).merge(client.responseQueue.dequeue)
          val e = q.enqueue

          WebSocketBuilder[IO].build(d, e)
        }

  }

  private implicit def convertWebSocketMessageToWebSocketFrame[T <: WebSocketMessage](msg: T)(implicit writer: Writer[T]): WebSocketFrame =
    Binary(ByteVector(writeBinary[T](msg)))

  private val CloseGracefully = Close(1000).toOption.get
  private val CloseWithUnknownRequst = Close(1008).toOption.get

}
