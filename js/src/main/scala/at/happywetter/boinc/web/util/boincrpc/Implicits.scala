package at.happywetter.boinc.web.util.boincrpc

import at.happywetter.boinc.shared.boincrpc.{BoincState, Result}

/**
 * Created by: 
 *
 * @author Raphael
 * @version 22.11.2020
 */
object Implicits {

  implicit class RichBoincState(private val state: BoincState) extends AnyVal {
    def getCurrentUsedMemory: Double = {
      state.results
        .filter(_.activeTask.nonEmpty)
        .map(_.activeTask.get)
        .map(_.workingSet)
        .sum
    }

    def getCurrentUsedCPUs: Int = {
      state.results
        .filter(p => p.activeTask.nonEmpty)
        .filter(t => !t.supsended && Result.ActiveTaskState(t.activeTask.get.activeTaskState) == Result.ActiveTaskState.PROCESS_EXECUTING)
        .map(p =>
          state.workunits
            .find(wu => wu.name == p.wuName)
            .flatMap(wu => state.apps.get(wu.appName))
            .map(app => if (app.nonCpuIntensive) 0 else app.version.maxCpus.getOrElse(1.0D).ceil.toInt)
            .getOrElse(0)
        ).sum
    }
  }




}
