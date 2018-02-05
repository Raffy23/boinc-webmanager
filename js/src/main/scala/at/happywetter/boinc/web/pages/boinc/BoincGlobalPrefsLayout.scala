package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.GlobalPrefsOverride
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater, FetchResponseException}
import at.happywetter.boinc.web.css.FloatingMenu
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincGlobalPrefsLayout.Style
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.pages.swarm.BoincSwarmPage
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

import scala.language.postfixOps
import scala.scalajs.js
import scala.xml.Elem
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
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  private var globalPrefsOverride: GlobalPrefsOverride = _

  override def onRender(client: BoincClient): Unit = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getGlobalPrefsOverride.map(f => {
      this.globalPrefsOverride = f

      root.appendChild(
        div( id := "global_prefs", Style.root_pane,
          div(FloatingMenu.root, BoincClientLayout.Style.in_text_icon,
            a(BoincSwarmPage.Style.button, i(`class` := "fa fa-check"), "submit".localize,
              onclick := onSubmitListener
            )
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
          label(checkbox(!f.runOnBatteries, "run_on_batteries"), "global_prefs_on_batteries".localize), br(),
          label(checkbox(!f.runIfUserActive, "run_if_active"), "global_prefs_cpu_active".localize), br(),
          label(checkbox(!f.runGPUIfUserActive, "run_gpu_if_active"), "global_prefs_gpu_active".localize), br(),

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

          label(checkbox(f.dontVerifyImages, "dont_verify_images"), "global_prefs_dont_verify_images".localize), br(),

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

          label(checkbox(f.leaveAppsInMemory, "leave_apps_in_memory"), "global_prefs_leave_apps_in_memory".localize)
        ).render
      )
    }).recover(ErrorDialogUtil.showDialog)
  }

  override val path = "global_prefs"

  private val onSubmitListener: js.Function1[Event, Unit] = (event) => {
    NProgress.start()
    boinc.setGlobalPrefsOverride(
      GlobalPrefsOverride(
        !dom.document.getElementById("run_on_batteries").asInstanceOf[HTMLInputElement].checked,
        globalPrefsOverride.batteryChargeMinPct,
        globalPrefsOverride.batteryMaxTemperature,
        !dom.document.getElementById("run_if_active").asInstanceOf[HTMLInputElement].checked,
        !dom.document.getElementById("run_gpu_if_active").asInstanceOf[HTMLInputElement].checked,
        globalPrefsOverride.idleTimeToRun,
        globalPrefsOverride.suspendCpuUsage,
        dom.document.getElementById("leave_apps_in_memory").asInstanceOf[HTMLInputElement].checked,
        dom.document.getElementById("dont_verify_images").asInstanceOf[HTMLInputElement].checked,
        dom.document.getElementById("workBufferDays").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("workBufferAdd").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("NcpuPct").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("shedPeriod").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("diskInterval").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("max_disk_usage").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("max_disk_usage_pct").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("min_disk_free").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("ram_used_busy").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("ram_used_idle").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("maxBytesUp").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("maxBytesDown").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("cpuUsageLimit").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("maxBytes").asInstanceOf[HTMLInputElement].value.toDouble,
        dom.document.getElementById("maxBytesPeriod").asInstanceOf[HTMLInputElement].value.toInt,
        globalPrefsOverride.networkWifiOnly,
        globalPrefsOverride.cpuTimes,
        globalPrefsOverride.netTimes
      )
    ).map(result => {

      if (!result) {
        NProgress.done(true)

        import scalatags.JsDom.all._
        new OkDialog("__SET_GLOBAL_OVERRIDE_PREFS", List("RESULT => false")).renderToBody().show()
      } else {
        boinc.readGlobalPrefsOverride.map(result => {
          NProgress.done(true)

          if (!result) {
            import scalatags.JsDom.all._
            new OkDialog("__READ_GLOBAL_OVERRIDE_PREFS", List("RESULT => false")).renderToBody().show()
          }
        }).recover(ErrorDialogUtil.showDialog)
      }

    }).recover(ErrorDialogUtil.showDialog)
  }

  private def checkbox(checkStatus: Boolean, cID: String): TypedTag[dom.html.Input] = {
    import scalatags.JsDom.all._
    if (checkStatus) input(`type` := "checkbox", checked, id := cID)
    else input(`type` := "checkbox", id := cID)
  }

  override def render: Elem = {<div>GLOBAL_PREFS</div>}
}
