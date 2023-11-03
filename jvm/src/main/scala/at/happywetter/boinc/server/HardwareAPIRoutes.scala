package at.happywetter.boinc.server

import scala.language.implicitConversions

import at.happywetter.boinc.extensions.linux.HWStatusService
import at.happywetter.boinc.shared.extension.HardwareData.Actions
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.util.http4s.ResponseEncodingHelper

import cats.Parallel
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.otel4s.trace.Tracer
import upickle.default._

/**
  * Created by:
  *
  * @author Raphael
  * @version 02.11.2017
  */
object HardwareAPIRoutes extends ResponseEncodingHelper:

  import cats.effect._
  import cats.syntax.all._
  import org.http4s._
  import org.http4s.dsl.io._

  def apply(hosts: Set[String], hwStatusService: HWStatusService)(implicit T: Tracer[IO]): HttpRoutes[IO] =
    HttpRoutes.of[IO]:

      case request @ GET -> Root / "host" => Ok(hosts.toList, request)

      case request @ GET -> Root / "host-data" =>
        Ok(
          hosts.toList
            .map(host => {
              hwStatusService
                .query(host)
                .map { case (cpuFreq, sensors) => (host, cpuFreq, sensors) }
                .handleError { case throwable => {
                  throwable.printStackTrace();

                  (host, Double.NaN, Map.empty)
                }}
            })
            .parUnorderedSequence,
          request
        )

      case request @ GET -> Root / "action" =>
        Ok(Actions(hwStatusService.globalActions, hwStatusService.actions), request)

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
              .flatMap((success, stdout) =>
                if success then Ok(stdout, request)
                else InternalServerError()
              )
          })
          .getOrElse(BadRequest())

      case request @ POST -> Root / "global-action" / action =>
        hwStatusService
          .executeGlobalAction(action)
          .flatMap((success, stdout) =>
            if success then Ok(stdout, request)
            else FailedDependency(stdout)
          )
