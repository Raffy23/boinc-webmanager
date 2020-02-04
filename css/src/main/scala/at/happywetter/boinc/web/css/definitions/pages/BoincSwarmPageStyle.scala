package at.happywetter.boinc.web.css.definitions.pages

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}
import at.happywetter.boinc.web.css.definitions.Misc

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincSwarmPageStyle extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "boinc_swarm_page"

  val checkbox = CSSIdentifier("checkbox")
  val masterCheckbox = CSSIdentifier("master_checkbox")

  val center = Misc.centeredText
  val button = CSSIdentifier("button")

  override private[css] def styles =
    Seq(checkbox, masterCheckbox, button)

}
