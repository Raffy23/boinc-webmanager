package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater, FetchResponseException}
import at.happywetter.boinc.web.css.FloatingMenu
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincGlobalPrefsLayout.Style
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.swarm.BoincSwarmPage
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.language.postfixOps
import scala.scalajs.js
import scalacss.internal.mutable.StyleSheet
import scalacss.ProdDefaults._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{`type`, checked, input}
import scalatags.generic.{Attr, AttrPair}

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

    client.getGlobalPrefsOverride.map(f => {

      root.appendChild(
        div( id := "global_prefs", Style.root_pane,
          div(FloatingMenu.root, BoincClientLayout.Style.in_text_icon,
            a(BoincSwarmPage.Style.button, i(`class` := "fa fa-check"), "submit".localize)
          ),
          h2(BoincClientLayout.Style.pageHeader, i(`class` := "fa fa-cogs"),  "global_prefs".localize),

          h4(BoincClientLayout.Style.h4_without_line, "global_prefs_computing".localize),
          label("global_prefs_cpu_cores".localize, `for` := "NcpuPct"),
          input(Style.input, id := "NcpuPct", value := f.maxNCpuPct), br(),

          label("global_prefs_cpu_usage".localize, `for` := "cpuUsageLimit"),
          input(Style.input, id := "cpuUsageLimit", value := f.cpuUsageLimit), br(),

          label("global_prefs_sheduling_period".localize, `for` := "shedPeriod"),
          input(Style.input, value := f.cpuSchedulingPeriodMinutes, id := "shedPeriod"),

          h4(Style.h4, "global_prefs_pause".localize),
          label(checkbox(!f.runOnBatteries), "global_prefs_on_batteries".localize), br(),
          label(checkbox(!f.runIfUserActive), "global_prefs_cpu_active".localize), br(),
          label(checkbox(!f.runGPUIfUserActive), "global_prefs_gpu_active".localize), br(),

          h4(Style.h4, "global_prefs_save_time".localize),
          label("global_prefs_workbuffer_days".localize, `for` := "workBufferDays"),
          input(Style.input, value := f.workBufferMinDays, id := "workBufferDays"), br(),

          label("global_prefs_add_buffer_days".localize, `for` := "workBufferAdd"),
          input(Style.input, value := f.workBufferAdditionalDays, id := "workBufferAdd"), br(),

          label("global_prefs_disk_interval".localize, `for` := "diskInterval"),
          input(Style.input, value := f.diskInterval, id := "diskInterval"),

          h4(Style.h4, "global_prefs_network".localize),
          label("global_prefs_max_bytes_down".localize, `for` := "maxBytesDown"),
          input(Style.input, value := BoincFormater.convertSpeedValue(f.maxBytesSecDownload, 1), id := "maxBytesDown"), br(),

          label("global_prefs_max_bytes_up".localize, `for` := "maxBytesUp"),
          input(Style.input, value := BoincFormater.convertSpeedValue(f.maxBytesSecUpload, 1), id := "maxBytesUp"), br(),

          label("global_prefs_max_bytes".localize, `for` := "maxBytes"),
          input(Style.input, value := f.dailyXFerLimitMB, id := "maxBytes"), br(),

          label("global_prefs_max_bytes_period".localize, `for` := "maxBytesPeriod"),
          input(Style.input, value := f.dailyXFerPeriodDays, id := "maxBytesPeriod"), br(),

          label(checkbox(f.dontVerifyImages), "global_prefs_dont_verify_images".localize), br(),

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

          label(checkbox(f.leaveAppsInMemory), "global_prefs_leave_apps_in_memory".localize)
        ).render
      )

      import at.happywetter.boinc.web.hacks.NodeListConverter._
      dom.document.querySelectorAll("input").forEach((node, _, _) => node.asInstanceOf[HTMLElement].setAttribute("disabled",""))
    }).recover(ErrorDialogUtil.showDialog)
  }

  override val path = "global_prefs"

  private def checkbox(checkStatus: Boolean): TypedTag[dom.html.Input] = {
    import scalatags.JsDom.all._
    if (checkStatus) input(`type` := "checkbox", checked)
    else input(`type` := "checkbox")
  }
}
