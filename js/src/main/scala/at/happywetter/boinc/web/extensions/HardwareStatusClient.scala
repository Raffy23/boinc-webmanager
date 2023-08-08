package at.happywetter.boinc.web.extensions

import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import at.happywetter.boinc.shared.extension.HardwareData.Action
import at.happywetter.boinc.shared.extension.HardwareData.Actions
import at.happywetter.boinc.shared.extension.HardwareData.SensorsData
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.util.StringLengthAlphaOrdering
import at.happywetter.boinc.web.facade.Implicits._
import at.happywetter.boinc.web.util.FetchHelper

import upickle.default._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
class HardwareStatusClient(val hostname: String,
                           private var cachedFreq: Option[Double] = None,
                           private var cachedSensorsData: Option[SensorsData] = None
):

  private val baseURI = s"/hardware/host/${dom.window.encodeURIComponent(hostname)}/"

  def getCpuFrequency: Future[Double] =
    if cachedFreq.isDefined then
      var result = cachedFreq.get
      cachedFreq = None

      Future.successful(result)
    else
      FetchHelper
        .get[Double](baseURI + "cpufrequency")

  def getSensorsData: Future[SensorsData] =
    if cachedSensorsData.isDefined then
      var result = cachedSensorsData.get
      cachedSensorsData = None

      Future.successful(result)
    else FetchHelper.get[SensorsData](baseURI + "sensors")

object HardwareStatusClient:

  def queryClients: Future[List[HardwareStatusClient]] =
    FetchHelper
      .get[List[String]]("/hardware/host")
      .map(
        _.sorted(ord = StringLengthAlphaOrdering)
          .map(new HardwareStatusClient(_))
      )

  def queryClientsWithData: Future[List[HardwareStatusClient]] =
    FetchHelper
      .get[List[(String, Double, SensorsData)]]("/hardware/host-data")
      .map(
        _.sortBy(_._1)
          .map((host, cpuFreq, sensors) => new HardwareStatusClient(host, Some(cpuFreq), Some(sensors)))
      )

  def queryActions: Future[Actions] =
    FetchHelper.get[Actions]("/hardware/action")

  def executeGlobalAction(action: String): Future[String] =
    FetchHelper.post[String, String](
      s"/hardware/global-action/${dom.window.encodeURIComponent(action)}",
      Option.empty
    )

  def executeAction(host: String, action: String): Future[String] =
    FetchHelper.post[String, String](
      s"/hardware/host/${dom.window.encodeURIComponent(host)}/action/${dom.window.encodeURIComponent(action)}",
      Option.empty
    )
