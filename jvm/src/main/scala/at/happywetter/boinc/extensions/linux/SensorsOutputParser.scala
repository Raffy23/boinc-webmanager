package at.happywetter.boinc.extensions.linux

import at.happywetter.boinc.shared.extension.HardwareData.{SensorsData, SensorsRow}

/**
  * Utility which can parse the Output of the Sensors program under linux
  *
  * @author Raphael
  * @version 02.11.2017
  */
object SensorsOutputParser {
  private val Pattern = """(.*):\s+([+\-.0-9]+)([\w\s°]+)\s*\((.*)\)\s*(.*)""".r
  private val NumberPattern = """([+\-.0-9]+)([\w\s°]+)""".r

  def parse(output: String): SensorsData = {
    output.split("\n").filter(_.contains(":")).map {
      case Pattern(identifier, value, unit, arguments, flags) =>
        (identifier.trim,
          SensorsRow(value.toDouble, unit, arguments.split(",").map { pair =>
            val data = pair.split("=")
            (data(0).trim, data(1).trim)
          }.toList, flags)
        )

      // Just in case the regex doesn't work ...
      case str: String =>
        val data = str.split(":\\s*")
        val values = data(1).split("\\s*\\(")

        if (values.length < 2)
          ("", SensorsRow(0, "", List(), ""))
        else {
          val args1  = values(1).split("\\)\\s*")
          val args   = args1(0).split(",").map { pair =>
            val data = pair.split("=")
            (data(0).trim, data(1).trim)
          }.toList

          val nV = values(0) match {
            case NumberPattern(num, unit) => (num, unit)
          }

          println(data(0))
          (data(0).trim,
            SensorsRow(
              nV._1.toDouble,
              nV._2,
              args,
              if (args1.length < 1) "" else args1.last.trim
            )
          )

        }
    }.toMap.filter(_._1.nonEmpty)
  }

}
