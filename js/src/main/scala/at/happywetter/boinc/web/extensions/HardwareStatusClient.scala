package at.happywetter.boinc.web.extensions

import at.happywetter.boinc.shared.HardwareData.SensorsData
import at.happywetter.boinc.web.helper.FetchHelper
import at.happywetter.boinc.web.helper.ResponseHelper._
import org.scalajs.dom.experimental.{Fetch, HttpMethod, RequestInit}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
class HardwareStatusClient(val hostname: String) {
  
  private val baseURI = "/api/hardware/" + hostname + "/"

  def getCpuFrequency: Future[Double] =
    Fetch.fetch(
      baseURI + "cpufrequency",
      RequestInit(method = HttpMethod.GET, headers = FetchHelper.header)
    )
      .mapData(data => decode[Double](data).toOption.get)

  def getSensorsData: Future[SensorsData] =
    Fetch.fetch(
      baseURI + "sensors",
      RequestInit(method = HttpMethod.GET, headers = FetchHelper.header)
    )
      .mapData(data => decode[SensorsData](data).toOption.get)

}

object HardwareStatusClient {

  def queryClients: Future[List[HardwareStatusClient]] =
    Fetch.fetch(
      "/api/hardware/",
      RequestInit(method = HttpMethod.GET, headers = FetchHelper.header)
    )
      .mapData(data => decode[List[String]](data).toOption.get)
      .map(_.map(new HardwareStatusClient(_)))
}
