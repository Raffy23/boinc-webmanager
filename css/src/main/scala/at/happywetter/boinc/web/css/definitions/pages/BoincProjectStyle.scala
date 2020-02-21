package at.happywetter.boinc.web.css.definitions.pages

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincProjectStyle extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "boinc_project"

  val link = CSSIdentifier("link")
  val firstRowFixedWith = CSSIdentifier("first_row_fixed")
  val floatingHeadbar = CSSIdentifier("floating_headbar")
  val floatingHeadbarButton = CSSIdentifier("floating_headbar_button")

  override private[css] def styles =
    Seq(link, firstRowFixedWith, floatingHeadbar, floatingHeadbarButton)

}
