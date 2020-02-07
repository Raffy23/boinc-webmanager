package at.happywetter.boinc.web.boincclient

import at.happywetter.boinc.shared.boincrpc.BoincState
import at.happywetter.boinc.web.storage.{AppSettingsStorage, HostInfoCache, MessageCache, TaskSpecCache}
import at.happywetter.boinc.web.util.ErrorDialogUtil

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.Date

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
object ClientCacheHelper {

  private var stateUpdate = false

  def init(): Unit = {
    // Clear old cached data from the client database ...
    val maxCacheLifetime = {
      val a = new Date()
      a.setDate(new Date().getDate() - 14)

      a
    }
    println(s"Delete old working cache entry older then ${maxCacheLifetime.toLocaleDateString()}")
    AppSettingsStorage.delete(maxCacheLifetime)
  }

  def updateClientCache(boinc: BoincClient, finishAction: BoincState => Unit = _ => {}): Unit = {
    if (!stateUpdate) {
      stateUpdate = true
      boinc.getState.map(state => {
        updateCache(boinc.hostname, state)

        stateUpdate = false
        finishAction(state)
      }).recover(ErrorDialogUtil.showDialog)
    }
  }

  def updateCache(name: String, state: BoincState): Unit = {
    state.apps.foreach(s => TaskSpecCache.save(name, s._1, s._2))
    state.workunits.foreach(workunit => AppSettingsStorage.save(name, workunit))

    HostInfoCache.get(name).map(_.startTime != state.timeStats.clientStartTime).foreach {
      case true =>
        println(s"Client $name has been restarted, clearing message cache ...")
        MessageCache.delete(name).foreach(count => s"Deleted $count entries for $name")
    }

    HostInfoCache.saveFromState(name, state)
    TaskSpecCache.updateCacheTimeStamp(name)
  }

}
