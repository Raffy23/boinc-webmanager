package at.happywetter.boinc.web.css.definitions.components

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object TableTheme extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "table"

  val container = CSSIdentifier("container")
  val table = CSSIdentifier("")
  val lastRowSmall = CSSIdentifier("last_row_small")
  val verticalText = CSSIdentifier("vertical-text")
  val noBorder = CSSIdentifier("no_border")
  val sortable = CSSIdentifier("sortable")

  override private[css] def styles =
    Seq(table, lastRowSmall, verticalText, noBorder, container, sortable)

}
