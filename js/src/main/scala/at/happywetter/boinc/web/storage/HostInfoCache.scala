package at.happywetter.boinc.web.storage

import org.scalajs.dom
import scala.util.Try

import at.happywetter.boinc.shared.boincrpc.{BoincState, HostInfo}
import at.happywetter.boinc.shared.parser._

import upickle.default

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
object HostInfoCache:
  import upickle.default._

  @inline private def key(name: String) = s"$name/host-info"

  case class CacheEntry(hostInfo: HostInfo, platform: String, boincVersion: String, startTime: Double)
  object CacheEntry:
    def empty(): CacheEntry =
      CacheEntry(
        HostInfo("", "", "", 0, "", "", List.empty, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, "", "", List.empty, None),
        "",
        "",
        0.0d
      )

  implicit private val cacheEntryParser: default.ReadWriter[CacheEntry] = macroRW[CacheEntry]

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
