package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.BoincClient
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.component.BoincPageLayout

import scala.scalajs.js

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.08.2017
  */
class BoincGlobalPrefsLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {
  override def onRender(client: BoincClient): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getGlobalPrefsOverride.foreach(f => {

      root.appendChild(
        div( id := "global_prefs",
          h2(BoincClientLayout.Style.pageHeader, "Berechnungseinstellungen: (TESTING / READONLY)"),
          div(
            h3("Nutzungsbegrenzungen"),
            "Nutze höchstens", input(value := f.maxNCpuPct), "% der Prozessoren", br(),
            "Nutze höchstens", input(value := f.cpuUsageLimit), "% der Prozessorzeit"
          ),

          div(
            h3("Wann unterbrochen werden soll"),
            input(`type` := "checkbox", checked := (if(f.runOnBatteries) "true" else "false")), "Akku-Betrieb", br(),
            input(`type` := "checkbox", checked := (if(f.runIfUserActive) "true" else "false")), "wenn Rechner benutzt wird", br(),
            input(`type` := "checkbox", checked := (if(f.runGPUIfUserActive) "true" else "false")), "GPU wenn Rechner benutzt wird", br(),
          ),

          div(
            h3("Sonstiges"),
            "Speichere mindestens", input(value := f.workBufferMinDays), "Arbeitstage", br(),
            "Speichere zusätzlich für weitere", input(value := f.workBufferAdditionalDays), "Arbeitstage", br(),
            "Zwischen Aufgaben wechseln", input(value := f.cpuSchedulingPeriodMinutes), "Minuten", br(),
            "Sicherung der Aufgaben höchstens alle", input(value := f.diskInterval), "Sekunden"
          )
        ).render
      )
    })
  }
}
