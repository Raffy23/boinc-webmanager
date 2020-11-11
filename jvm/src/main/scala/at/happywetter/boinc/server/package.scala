package at.happywetter.boinc

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 11.11.2020
 */
package object server {

  val bootUpTime: String = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))

}
