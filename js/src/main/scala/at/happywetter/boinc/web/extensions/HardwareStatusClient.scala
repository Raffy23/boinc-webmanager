package at.happywetter.boinc.web.extensions

import at.happywetter.boinc.shared.HardwareData.SensorsData
import at.happywetter.boinc.web.helper.FetchHelper
import at.happywetter.boinc.web.helper.ResponseHelper._
import org.scalajs.dom.experimental.{Fetch, HttpMethod, RequestInit}
import prickle.Unpickle

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
class HardwareStatusClient(val hostname: String) {

  private val baseURI = "/api/hardware/" + hostname + "/"
  import prickle._

  def getCpuFrequency: Future[Double] =
    Fetch.fetch(
      baseURI + "cpufrequency",
      RequestInit(method = HttpMethod.GET, headers = FetchHelper.header)
    )
      .mapData(data => Unpickle[Double].fromString(json = data).get)

  def getSensorsData: Future[SensorsData] =
    Fetch.fetch(
      baseURI + "sensors",
      RequestInit(method = HttpMethod.GET, headers = FetchHelper.header)
    )
      .mapData(data => Unpickle[SensorsData].fromString(json = data).get)

}

object HardwareStatusClient {

  def queryClients: Future[List[HardwareStatusClient]] =
    Fetch.fetch(
      "/api/hardware/",
      RequestInit(method = HttpMethod.GET, headers = FetchHelper.header)
    )
      .mapData(data => Unpickle[List[String]].fromString(json = data).get)
      .map(_.map(new HardwareStatusClient(_)))
}
