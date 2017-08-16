package at.happywetter.boinc.web.boincclient

import at.happywetter.boinc.web.storage.{AppSettingsStorage, HostInfoCache, TaskSpecCache}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
object ClientCacheHelper {

  private var stateUpdate = false

  def updateClientCache(boinc: BoincClient, finishAction: () => Unit = () => {})(implicit boincClientName: String): Unit = {
    if (!stateUpdate) {
      stateUpdate = true
      boinc.getState.foreach(state => {
        state.apps.foreach(s => TaskSpecCache.save(boincClientName, s._1, s._2))
        state.workunits.foreach(workunit => AppSettingsStorage.save(boincClientName, workunit))
        HostInfoCache.saveFromState(boincClientName, state)

        TaskSpecCache.updateCacheTimeStamp(boincClientName)
        stateUpdate = false
        finishAction()
      })
    }
  }

}
