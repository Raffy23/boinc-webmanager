package at.happywetter.boinc.web.storage

import at.happywetter.boinc.shared.boincrpc.{BoincState, HostInfo}
import at.happywetter.boinc.shared.parser._
import org.scalajs.dom

import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
object HostInfoCache {
  import upickle.default._

  case class CacheEntry(hostInfo: HostInfo, platform: String, boincVersion: String)
  private implicit val cacheEntryParser = macroRW[CacheEntry]

  def saveFromState(name: String, boincState: BoincState): Unit =
    dom.window.localStorage.setItem(name+"/host-info",
      write(CacheEntry(boincState.hostInfo, boincState.platform, boincState.boincVersion))
    )

  def get(name: String): Option[CacheEntry] = Try(read[CacheEntry](
    dom.window.localStorage.getItem(name+"/host-info")
  )).toOption

}
