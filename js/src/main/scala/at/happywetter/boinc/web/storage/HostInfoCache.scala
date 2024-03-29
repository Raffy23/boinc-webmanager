package at.happywetter.boinc.web.storage

import at.happywetter.boinc.shared.boincrpc.{BoincState, HostInfo}
import at.happywetter.boinc.shared.parser._
import org.scalajs.dom
import upickle.default

import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
object HostInfoCache {
  import upickle.default._

  @inline private def key(name: String) = s"$name/host-info"

  case class CacheEntry(hostInfo: HostInfo, platform: String, boincVersion: String, startTime: Double)
  object CacheEntry {
    def empty(): CacheEntry =
      CacheEntry(
        HostInfo("","","",0,"","",List.empty,0D,0D,0D,0D,0D,0D,0D,0D,"","",List.empty,None),
        "",
        "",
        0.0D
      )
  }

  private implicit val cacheEntryParser: default.ReadWriter[CacheEntry] = macroRW[CacheEntry]

  def saveFromState(name: String, boincState: BoincState): Unit =
    dom.window.localStorage.setItem(key(name),
      write(
        CacheEntry(
          boincState.hostInfo,
          boincState.platform,
          boincState.boincVersion,
          boincState.timeStats.clientStartTime
        )
      )
    )

  def get(name: String): Option[CacheEntry] = Try(
    read[CacheEntry](
      dom.window.localStorage.getItem(key(name))
    )
  ).toOption

}
