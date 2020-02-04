package at.happywetter.boinc.web.css.definitions.components

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object DropDownMenuStyle extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "dropdown_menu"

  val dropdown = CSSIdentifier("dropdown")
  val button = CSSIdentifier("button")
  val dropdownContent = CSSIdentifier("dropdown_content")

  override private[css] def styles =
    Seq(dropdown, button, dropdownContent)

}
