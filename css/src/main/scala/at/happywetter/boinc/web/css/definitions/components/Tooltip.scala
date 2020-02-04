package at.happywetter.boinc.web.css.definitions.components

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object Tooltip extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "tooltip"

  val errorIcon = CSSIdentifier("error")
  val loadingIcon = CSSIdentifier("loading")
  val tooltipText = CSSIdentifier("text")
  val topText = CSSIdentifier("top_text")
  val leftText = CSSIdentifier("left_text")
  val tooltip = CSSIdentifier("")

  override private[css] def styles =
    Seq(errorIcon, tooltipText, tooltip, topText, leftText, loadingIcon)

}
