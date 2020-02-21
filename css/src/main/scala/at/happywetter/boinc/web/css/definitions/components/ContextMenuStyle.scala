package at.happywetter.boinc.web.css.definitions.components

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object ContextMenuStyle extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "context_menu"

  val contextMenu = CSSIdentifier("")
  val elem = CSSIdentifier("element")

  override private[css] def styles =
    Seq(contextMenu, elem)

}
