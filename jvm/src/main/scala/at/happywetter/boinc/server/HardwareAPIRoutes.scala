package at.happywetter.boinc.server

import at.happywetter.boinc.BoincManager
import at.happywetter.boinc.extensions.linux.HWStatusService
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.11.2017
  */
object HardwareAPIRoutes {

  import org.http4s._
  import org.http4s.dsl._
  import prickle._

  def apply(hosts: Set[String], hwStatusService: HWStatusService): HttpService = HttpService {

    case GET -> Root => Ok(Pickle.intoString(hosts.toList))

    case GET -> Root / name / "cpufrequency" =>
      hosts.find(_ == name).map(_ => {
        Ok(hwStatusService.query(name).map(_._1).map(Pickle.intoString(_)))
      }).getOrElse(BadRequest())

    case GET -> Root / name / "sensors" =>
      hosts.find(_ == name).map(_ => {
        Ok(hwStatusService.query(name).map(_._2.toMap).map(Pickle.intoString(_)))
      }).getOrElse(BadRequest())
  }

}
