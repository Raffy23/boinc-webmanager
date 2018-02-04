package at.happywetter.boinc.web.helper

import at.happywetter.boinc.shared.ServerSharedConfig
import at.happywetter.boinc.web.helper.ResponseHelper.ErrorResponseFeature
import org.scalajs.dom.experimental.{Fetch, HttpMethod, RequestInit}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.09.2017
  */
object ServerConfig {

  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

  private var populated = false
  private var serverConfig = ServerSharedConfig(12 * 60 * 60 * 1000, hardware = false)

  def get: Future[ServerSharedConfig] =
    if (!populated)
      queryFromServer.map(config => {
        serverConfig = config
        populated = true

        config
      })
    else
      Future { serverConfig }

  def queryFromServer: Future[ServerSharedConfig] =
    Fetch.fetch("/api/config", RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .flatMap(_.tryGet)
      .map(data => decode[ServerSharedConfig](data).toOption.get)

}
