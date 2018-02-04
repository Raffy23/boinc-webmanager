package at.happywetter.boinc.web.storage

import at.happywetter.boinc.shared.{BoincState, HostInfo}
import org.scalajs.dom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
object HostInfoCache {
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

  case class CacheEntry(hostInfo: HostInfo, platform: String, boincVersion: String)

  def saveFromState(name: String, boincState: BoincState): Unit =
    dom.window.localStorage.setItem(name+"/host-info",
      CacheEntry(boincState.hostInfo, boincState.platform, boincState.boincVersion).asJson.noSpaces
    )

  def get(name: String): Option[CacheEntry] = decode[CacheEntry](
    dom.window.localStorage.getItem(name+"/host-info")
  ).toOption


}
