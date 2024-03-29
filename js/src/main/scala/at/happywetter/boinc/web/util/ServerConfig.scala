package at.happywetter.boinc.web.util

import at.happywetter.boinc.shared.boincrpc.ServerSharedConfig
import at.happywetter.boinc.shared.parser._
import mhtml.Var

import scala.concurrent.Future
import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by:
  *
  * @author Raphael
  * @version 20.09.2017
  */
object ServerConfig {
  val config: Var[ServerSharedConfig] = Var(
    ServerSharedConfig(12 * 60 * 60 * 1000, hardware = false)
  )

  def query: Future[ServerSharedConfig] =
    queryFromServer.map(config => {
      this.config := config
      config
    })

  def queryFromServer: Future[ServerSharedConfig] =
    FetchHelper.get[ServerSharedConfig]("/api/config")

}
