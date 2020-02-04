package at.happywetter.boinc.shared.extension

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
object HardwareData {

  case class SensorsRow(value: Double, unit: String, arguments: List[(String, String)], flags: String) {
    def toValueUnitString: String = s"$value $unit"
  }

  type SensorsData = Map[String, SensorsRow]

}
