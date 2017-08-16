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
  import prickle._

  case class CacheEntry(hostInfo: HostInfo, platform: String, boincVersion: String)

  def saveFromState(name: String, boincState: BoincState): Unit =
    dom.window.localStorage.setItem(name+"/host-info",
      Pickle.intoString(CacheEntry(boincState.hostInfo, boincState.platform, boincState.boincVersion))
    )

  def get(name: String): Option[CacheEntry] = Unpickle[CacheEntry].fromString(
    dom.window.localStorage.getItem(name+"/host-info")
  ).toOption


}
