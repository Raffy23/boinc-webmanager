package at.happywetter.boinc.web.css.definitions.pages

import at.happywetter.boinc.web.css.{CSSIdentifier, StyleDefinitions}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincMessageStyle extends StyleDefinitions {

  override protected[this] implicit val prefix: String = "boinc_message_layout"

  val dateCol = CSSIdentifier("date_column")
  val tableRow = CSSIdentifier("table_row")
  val noticeList = CSSIdentifier("notice_list")

  override private[css] def styles =
    Seq(dateCol, tableRow, noticeList)

}
