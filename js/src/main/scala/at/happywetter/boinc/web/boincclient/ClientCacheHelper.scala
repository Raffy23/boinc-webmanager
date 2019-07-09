package at.happywetter.boinc.web.boincclient

import at.happywetter.boinc.shared.boincrpc.BoincState
import at.happywetter.boinc.web.storage.{AppSettingsStorage, HostInfoCache, TaskSpecCache}
import at.happywetter.boinc.web.util.ErrorDialogUtil

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
object ClientCacheHelper {

  private var stateUpdate = false

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

    try {
      HostInfoCache.saveFromState(name, state)
      TaskSpecCache.updateCacheTimeStamp(name)
    } catch {
      case ex: Exception => ex.printStackTrace()
    }

  }

}
