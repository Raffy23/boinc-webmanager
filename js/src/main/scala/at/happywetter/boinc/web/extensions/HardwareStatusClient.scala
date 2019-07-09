package at.happywetter.boinc.web.extensions

import at.happywetter.boinc.shared.extension.HardwareData.SensorsData
import at.happywetter.boinc.web.helper.FetchHelper
import upickle.default._
import at.happywetter.boinc.shared.parser._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
class HardwareStatusClient(val hostname: String) {
  
  private val baseURI = "/api/hardware/" + hostname + "/"

  def getCpuFrequency: Future[Double] =
    FetchHelper.get[Double](baseURI + "cpufrequency")

  def getSensorsData: Future[SensorsData] =
    FetchHelper.get[SensorsData](baseURI + "sensors")

}

object HardwareStatusClient {

  def queryClients: Future[List[HardwareStatusClient]] =
    FetchHelper.get[List[String]]("/api/hardware")
      .map(_.map(new HardwareStatusClient(_)))

}
