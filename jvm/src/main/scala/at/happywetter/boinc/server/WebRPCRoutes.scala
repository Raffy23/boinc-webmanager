package at.happywetter.boinc.server

import at.happywetter.boinc.AppConfig
import at.happywetter.boinc.boincclient.WebRPC
import at.happywetter.boinc.util.http4s.ResponseEncodingHelper
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl._
import org.http4s.dsl.io._
import at.happywetter.boinc.shared.parser._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 09.07.2019
  */
object WebRPCRoutes extends ResponseEncodingHelper:

  object ServerParamDecoder extends QueryParamDecoderMatcher[String]("server")

  def apply( /*implicit conf: AppConfig.WebRPC*/ ): HttpRoutes[IO] = HttpRoutes.of[IO]:

    case request @ GET -> Root => NotAcceptable()

    case request @ GET -> Root / "status" :? ServerParamDecoder(server) =>
      println("========================================================")
      println("        : " + server)
      println("========================================================")
      Ok(WebRPC.getServerStatus(server)(), request)
