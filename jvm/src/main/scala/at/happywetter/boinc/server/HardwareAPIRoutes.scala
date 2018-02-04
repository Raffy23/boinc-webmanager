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

  import cats.effect._
  import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._
  import org.http4s.circe._
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._


  def apply(hosts: Set[String], hwStatusService: HWStatusService): HttpService[IO] = HttpService[IO] {

    case GET -> Root => Ok(hosts.toList.asJson)

    case GET -> Root / name / "cpufrequency" =>
      hosts.find(_ == name).map(_ => {
        Ok(hwStatusService.query(name).map(_._1).map(_.asJson))
      }).getOrElse(BadRequest())

    case GET -> Root / name / "sensors" =>
      hosts.find(_ == name).map(_ => {
        Ok(hwStatusService.query(name).map(_._2.toMap).map(_.asJson))
      }).getOrElse(BadRequest())
  }

}
