package at.happywetter.boinc.web.css.definitions.components

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BasicModalStyle extends StyleDefinitions:

  implicit override protected[this] val prefix: String = "modal"

  val modal = CSSIdentifier("")
  val content = CSSIdentifier("content")
  val body = CSSIdentifier("body")
  val header = CSSIdentifier("header")
  val footer = CSSIdentifier("footer")
  val button = CSSIdentifier("button")
  val actionList = CSSIdentifier("action_list")
  val action = CSSIdentifier("action")

  override private[css] def styles =
    Seq(modal, content, body, header, footer, button, actionList, action)
