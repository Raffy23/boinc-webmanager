package at.happywetter.boinc.server

import at.happywetter.boinc.extensions.linux.HWStatusService
import at.happywetter.boinc.util.http4s.MsgPackRequRespHelper
import at.happywetter.boinc.shared.parser._
import upickle.default._
import scala.language.implicitConversions

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.11.2017
  */
object HardwareAPIRoutes extends MsgPackRequRespHelper {

  import cats.effect._
  import org.http4s._
  import org.http4s.dsl.io._

  def apply(hosts: Set[String], hwStatusService: HWStatusService): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case request @ GET -> Root => Ok(hosts.toList, request)

    case request @ GET -> Root / name / "cpufrequency" =>
      hosts.find(_ == name).map(_ => {
        Ok(hwStatusService.query(name).map(_._1), request)
      }).getOrElse(BadRequest())

    case request @ GET -> Root / name / "sensors" =>
      hosts.find(_ == name).map(_ => {
        Ok(hwStatusService.query(name).map(_._2.toMap), request)
      }).getOrElse(BadRequest())
  }

}
