package at.happywetter.boinc.web.css.definitions.components

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object FloatingMenu extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "floating-menu"

  val root   = CSSIdentifier("")
  val active = CSSIdentifier("active")

  override private[css] def styles =
    Seq(root, active)

}
