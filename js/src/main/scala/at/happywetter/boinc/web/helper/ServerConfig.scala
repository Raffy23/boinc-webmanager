package at.happywetter.boinc.web.helper

import at.happywetter.boinc.shared.webrpc.ServerSharedConfig
import at.happywetter.boinc.shared.parser._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.09.2017
  */
object ServerConfig {

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
    FetchHelper.get[ServerSharedConfig]("/api/config")

}
