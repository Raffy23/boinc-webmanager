package at.happywetter.boinc.web.util

import org.scalajs.dom
import org.scalajs.dom.{Blob, FileReader, WebSocket}
import upickle.default.{readBinary, writeBinary, Reader, Writer}
import scala.collection.mutable
import scala.scalajs.js.typedarray.{ArrayBuffer, DataView}

import at.happywetter.boinc.shared.websocket.WebSocketMessage
import at.happywetter.boinc.shared.websocket.webSocketMessageParser

/**
  * Created by:
  *
  * @author Raphael
  * @version 17.07.2019
  */
object WebSocketClient:

  val listener = new mutable.ListBuffer[WebSocketMessage => Any]

  private val messageBuffer = new mutable.ArrayBuffer[WebSocketMessage]()
  private var connected = false

  private val webSocketURI =
    val protocol = dom.window.location.protocol match
      case "http:"  => "ws"
      case "https:" => "wss"

    s"$protocol://${dom.window.location.hostname}:${dom.window.location.port}/ws?token=${dom.window.localStorage.getItem("auth/token")}"

  private var ws: Option[WebSocket] = None

  def start(): Unit =
    ws = Some(new WebSocket(webSocketURI))
    ws.foreach(_.onclose = _ => { println("[WebSocket]: Connection is closed!"); connected = false; ws = None })
    ws.foreach(_.onopen = _ => {
      println("[WebSocket]: Opened a connection")
      connected = true

      messageBuffer.foreach(msg => sendOverWebsocket(msg))
      messageBuffer.clear()
    })
    ws.foreach(_.onmessage = event => {
      val reader = new FileReader()
      reader.onload = _ => {
        val message = parseResponseFromArrayBuffer[WebSocketMessage](reader.result.asInstanceOf[ArrayBuffer])
        listener.foreach(_(message))
      }

      reader.readAsArrayBuffer(event.data.asInstanceOf[Blob])
    })

  def stop(): Unit =
    ws.foreach(_.close())
    ws = None

  def isOpend: Boolean = ws.isDefined

  def send[T <: WebSocketMessage](message: T)(implicit w: Writer[T]): Unit =
    if (!connected) messageBuffer.append(message)
    else sendOverWebsocket(message)

  private def sendOverWebsocket[T <: WebSocketMessage](message: T)(implicit w: Writer[T]): Unit =
    val binaryMessage = writeBinary(message)
    val arrayBuffer = new ArrayBuffer(binaryMessage.length)
    val dataView = new DataView(arrayBuffer)
    binaryMessage.indices.foreach(idx => dataView.setInt8(idx, binaryMessage(idx)))

    ws.foreach(_.send(arrayBuffer))

  private def parseResponseFromArrayBuffer[T <: WebSocketMessage](buffer: ArrayBuffer)(implicit w: Reader[T]): T =
    val dataView = new DataView(buffer)
    val data: Array[Byte] = Array.fill[Byte](dataView.byteLength)(0x0)
    data.indices.foreach(idx => data(idx) = dataView.getInt8(idx))

    readBinary[T](data)
