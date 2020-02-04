package at.happywetter.boinc.web.css.definitions.pages

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object ProjectSwarmPageStyle extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "project_swarm_page"

  val masterCheckbox = BoincSwarmPageStyle.masterCheckbox
  val checkbox = BoincSwarmPageStyle.checkbox
  val center = BoincSwarmPageStyle.center
  val button = BoincSwarmPageStyle.button
  val link = BoincProjectStyle.link

  val topNavigationAtion = CSSIdentifier("top_nav_action")
  val lastRowSmall = CSSIdentifier("last_row_small")
  val floatingMenu = CSSIdentifier("floating_menu")

  override private[css] def styles =
    Seq(topNavigationAtion, lastRowSmall, floatingMenu)

}
