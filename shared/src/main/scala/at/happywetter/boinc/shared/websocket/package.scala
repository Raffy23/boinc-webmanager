package at.happywetter.boinc.shared

import upickle.default.macroRW

/**
  * Created by: 
  *
  * @author Raphael
  * @version 16.07.2019
  */
package object websocket {


  sealed trait WebSocketMessage
  object SubscribeToGroupChanges extends WebSocketMessage
  object UnsubscribeToGroupChanges extends WebSocketMessage

  case class HostInformationChanged(hosts: List[String], groups: Map[String, List[String]]) extends WebSocketMessage

  object ACK extends WebSocketMessage
  object NACK extends WebSocketMessage


  implicit val hostInformationChangedParser = macroRW[HostInformationChanged]
  implicit val webSocketMessageParser = macroRW[WebSocketMessage]

}
