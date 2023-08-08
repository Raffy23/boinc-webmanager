package at.happywetter.boinc

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import org.http4s.{HttpDate, ParseResult}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 11.11.2020
 */
package object server:

  val bootUpTime: String = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))

  val bootUpDate: HttpDate =
    HttpDate
      .fromZonedDateTime(ZonedDateTime.now(ZoneOffset.UTC))
      .getOrElse(throw new RuntimeException("Unable to compute bootup date!"))
