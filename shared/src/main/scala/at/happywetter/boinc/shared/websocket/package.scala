package at.happywetter.boinc.shared

import upickle.default.{ReadWriter, macroRW}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 16.07.2019
  */
package object websocket:

  sealed trait WebSocketMessage
  case object SubscribeToGroupChanges extends WebSocketMessage
  case object UnsubscribeToGroupChanges extends WebSocketMessage

  case class HostInformationChanged(hosts: Seq[String], groups: Map[String, List[String]]) extends WebSocketMessage

  case object ACK extends WebSocketMessage
  case object NACK extends WebSocketMessage

  implicit val websocketAckParser: ReadWriter[ACK.type] = macroRW
  implicit val websocketNackParser: ReadWriter[NACK.type] = macroRW
  implicit val hostInformationChangedParser: ReadWriter[HostInformationChanged] = macroRW
  implicit val subscribeToGroupChangesParser: ReadWriter[SubscribeToGroupChanges.type] = macroRW
  implicit val unsubscribeToGroupChangesParser: ReadWriter[UnsubscribeToGroupChanges.type] = macroRW
  implicit val webSocketMessageParser: ReadWriter[WebSocketMessage] = macroRW
