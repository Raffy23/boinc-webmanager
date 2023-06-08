package at.happywetter.boinc.web.extensions

import at.happywetter.boinc.shared.extension.HardwareData.SensorsData
import upickle.default._
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.util.StringLengthAlphaOrdering
import at.happywetter.boinc.web.util.FetchHelper
import at.happywetter.boinc.web.facade.Implicits._
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
class HardwareStatusClient(val hostname: String):

  private val baseURI = s"/hardware/host/${dom.window.encodeURIComponent(hostname)}/"

  def getCpuFrequency: Future[Double] =
    FetchHelper.get[Double](baseURI + "cpufrequency")

  def getSensorsData: Future[SensorsData] =
    FetchHelper.get[SensorsData](baseURI + "sensors")

object HardwareStatusClient:

  def queryClients: Future[List[HardwareStatusClient]] =
    FetchHelper
      .get[List[String]]("/hardware/host")
      .map(
        _.sorted(ord = StringLengthAlphaOrdering)
          .map(new HardwareStatusClient(_))
      )

  def queryActions: Future[List[String]] =
    FetchHelper.get[List[String]]("/hardware/action")

  def executeAction(host: String, action: String): Future[String] =
    FetchHelper.post[String, String](
      s"/hardware/host/${dom.window.encodeURIComponent(host)}/action/${dom.window.encodeURIComponent(action)}",
      ""
    )
