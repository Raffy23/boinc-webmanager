package at.happywetter.boinc.server

import at.happywetter.boinc.extensions.linux.HWStatusService
import at.happywetter.boinc.util.http4s.ResponseEncodingHelper
import at.happywetter.boinc.shared.parser._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import upickle.default._

import scala.language.implicitConversions

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.11.2017
  */
object HardwareAPIRoutes extends ResponseEncodingHelper:

  import cats.effect._
  import org.http4s._
  import org.http4s.dsl.io._

  def apply(hosts: Set[String], hwStatusService: HWStatusService): HttpRoutes[IO] = HttpRoutes.of[IO]:

    case request @ GET -> Root / "host" => Ok(hosts.toList, request)

    case request @ GET -> Root / "action" => Ok(hwStatusService.actions.keys, request)

    case request @ GET -> Root / "host" / name / "cpufrequency" =>
      hosts
        .find(_ == name)
        .map(_ => {
          Ok(hwStatusService.query(name).map(_._1), request)
        })
        .getOrElse(BadRequest())

    case request @ GET -> Root / "host" / name / "sensors" =>
      hosts
        .find(_ == name)
        .map(_ => {
          Ok(hwStatusService.query(name).map(_._2.toMap), request)
        })
        .getOrElse(BadRequest())

    case request @ POST -> Root / "host" / name / "action" / action =>
      hosts
        .find(_ == name)
        .map(_ => {
          hwStatusService
            .executeAction(name, action)
            .ifM(
              Ok("Ok", request),
              InternalServerError()
            )
        })
        .getOrElse(BadRequest())
