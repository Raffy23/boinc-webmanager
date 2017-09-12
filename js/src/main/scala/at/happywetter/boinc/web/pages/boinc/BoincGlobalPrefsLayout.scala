package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.BoincClient
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincGlobalPrefsLayout.Style
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.util.I18N._

import scala.language.postfixOps
import scala.scalajs.js
import scalacss.internal.mutable.StyleSheet
import scalacss.ProdDefaults._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.08.2017
  */
object BoincGlobalPrefsLayout {

  object Style extends StyleSheet.Inline {
    import dsl._

    val root_pane = style(
      unsafeChild("label")(
        marginLeft(15 px)
      )
    )

    val input = style(
      outline.`0`,
      backgroundColor(c"#FFF"),
      width(4 em),
      border :=! "1px #AAA solid",
      margin(3 px, 6 px),
      padding(6 px, 8 px),
      boxSizing.borderBox,
      fontSize(14 px)
    )

    val h4 = style(
      BoincClientLayout.Style.h4_without_line,
      marginTop(30 px)
    )
  }

}

class BoincGlobalPrefsLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {
  override def onRender(client: BoincClient): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getGlobalPrefsOverride.foreach(f => {

      root.appendChild(
        div( id := "global_prefs", Style.root_pane,
          h2(BoincClientLayout.Style.pageHeader, i(`class` := "fa fa-cogs"),  "global_prefs".localize),

          h4(BoincClientLayout.Style.h4_without_line, "global_prefs_computing".localize),
          label("global_prefs_cpu_cores".localize, `for` := "NcpuPct"),
          input(Style.input, id := "NcpuPct", value := f.maxNCpuPct), br(),

          label("global_prefs_cpu_usage".localize, `for` := "cpuUsageLimit"),
          input(Style.input, id := "cpuUsageLimit", value := f.cpuUsageLimit), br(),

          label("global_prefs_sheduling_period".localize, `for` := "shedPeriod"),
          input(Style.input, value := f.cpuSchedulingPeriodMinutes, id := "shedPeriod"),

          h4(Style.h4, "global_prefs_pause".localize),
          label(input(`type` := "checkbox", checked := (if(!f.runOnBatteries) "true" else "false")), "global_prefs_on_batteries".localize), br(),
          label(input(`type` := "checkbox", checked := (if(!f.runIfUserActive) "true" else "false")), "global_prefs_cpu_active".localize), br(),
          label(input(`type` := "checkbox", checked := (if(!f.runGPUIfUserActive) "true" else "false")), "global_prefs_gpu_active".localize), br(),

          h4(Style.h4, "global_prefs_save_time".localize),
          label("global_prefs_workbuffer_days".localize, `for` := "workBufferDays"),
          input(Style.input, value := f.workBufferMinDays, id := "workBufferDays"), br(),

          label("global_prefs_add_buffer_days".localize, `for` := "workBufferAdd"),
          input(Style.input, value := f.workBufferAdditionalDays, id := "workBufferAdd"), br(),

          label("global_prefs_disk_interval".localize, `for` := "diskInterval"),
          input(Style.input, value := f.diskInterval, id := "diskInterval"),

          h4(Style.h4, "global_prefs_network".localize),
          label("global_prefs_max_bytes_down".localize, `for` := "maxBytesDown"),
          input(Style.input, value := f.maxBytesSecDownload, id := "maxBytesDown"), br(),

          label("global_prefs_max_bytes_up".localize, `for` := "maxBytesUp"),
          input(Style.input, value := f.maxBytesSecUpload, id := "maxBytesUp"), br(),

          label("global_prefs_max_bytes".localize, `for` := "maxBytes"),
          input(Style.input, value := f.dailyXFerLimitMB, id := "maxBytes"), br(),

          label("global_prefs_max_bytes_period".localize, `for` := "maxBytesPeriod"),
          input(Style.input, value := f.dailyXFerPeriodDays, id := "maxBytesPeriod"), br(),

          label(input(`type` := "checkbox", checked := (if(f.dontVerifyImages) "true" else "false")), "global_prefs_dont_verify_images".localize), br(),

          h4(Style.h4, "global_prefs_disk".localize),
          label("global_prefs_min_disk_free".localize, `for` := "min_disk_free"),
          input(Style.input, value := f.diskMinFreeGB, id := "min_disk_free"), br(),

          label("global_prefs_max_disk_usage".localize, `for` := "max_disk_usage"),
          input(Style.input, value := f.diskMaxUsedGB, id := "max_disk_usage"), br(),

          label("global_prefs_max_disk_usage_pct".localize, `for` := "max_disk_usage_pct"),
          input(Style.input, value := f.diskMaxUsedPct, id := "max_disk_usage_pct"), br(),


          h4(Style.h4, "global_prefs_memory".localize),
          label("global_prefs_ram_used_busy".localize, `for` := "ram_used_busy"),
          input(Style.input, value := f.ramUsedBusyPct, id := "ram_used_busy"), br(),

          label("global_prefs_ram_used_idle".localize, `for` := "ram_used_idle"),
          input(Style.input, value := f.ramUsedIdlePct, id := "ram_used_idle"), br(),

          label(input(`type` := "checkbox", checked := (if(f.leaveAppsInMemory) "true" else "false")), "global_prefs_leave_apps_in_memory".localize), br(),
        ).render
      )

      new OkDialog("__read_only__", List("__formular_data_is_read_only__")).renderToBody().show()
    })
  }

  override val path = "global_prefs"
}
