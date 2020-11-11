package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.boincrpc.GlobalPrefsOverride
import at.happywetter.boinc.web.boincclient.BoincFormater.Implicits._
import at.happywetter.boinc.web.css.definitions.components.FloatingMenu
import at.happywetter.boinc.web.css.definitions.pages.{BoincClientStyle, BoincSwarmPageStyle, BoincGlobalPrefsStyle => Style}
import at.happywetter.boinc.web.util.RichRx._
import at.happywetter.boinc.web.util.XMLHelper._
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLInputElement

import scala.language.postfixOps
import scala.xml.{Elem, Node}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.08.2017
  */
class BoincGlobalPrefsLayout extends BoincClientLayout {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  override val path = "global_prefs"

  private var globalPrefsOverride = Var(
    GlobalPrefsOverride(false,0D,0D,false,false,0D,0D,false,false,0D,0D,0D,0D,0D,0D,0D,0D, 0D,0D,0D,0D,0D,0D,0,false,(-1D,-1D),(-1D,-1D),List(),List())
  )

  override def already(): Unit = onRender()

  override def render: Elem = {
    @inline def v[T](x: (GlobalPrefsOverride) => T): Rx[String] = globalPrefsOverride.map(x).map(_.toString)
    @inline def b[T](x: (GlobalPrefsOverride) => Boolean): Rx[Boolean] = globalPrefsOverride.map(x)

    <div id="global_prefs" class={Style.rootPane.htmlClass}>
      <div class={Seq(FloatingMenu.root.htmlClass, BoincClientStyle.inTextIcon).mkString(" ")}>
        <a class={BoincSwarmPageStyle.button.htmlClass} onclick={jsOnSubmitListener}>
          <i class="fa fa-check" aria-hidden="true"></i>
          {"submit".localize}
        </a>
      </div>

      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fa fa-cogs" aria-hidden="true"></i>
        {"global_prefs".localize}
      </h2>

      <h4 class={BoincClientStyle.h4WithoutLine.htmlClass}>{"global_prefs_computing".localize}</h4>
      <label for="NcpuPct">{"global_prefs_cpu_cores".localize}</label>
      <input class={Style.input.htmlClass} id="NcpuPct" value={v(_.maxNCpuPct)}></input>
      <br/>
      <label for="cpuUsageLimit">{"global_prefs_cpu_usage".localize}</label>
      <input class={Style.input.htmlClass} id="cpuUsageLimit" value={v(_.cpuUsageLimit)}></input>
      <br/>
      <label for="shedPeriod">{"global_prefs_sheduling_period".localize}</label>
      <input class={Style.input.htmlClass} id="shedPeriod" value={v(_.cpuSchedulingPeriodMinutes)}></input>
      <br/>

      <h4 class={Seq(BoincClientStyle.h4, Style.h4Padding).map(_.htmlClass).mkString(" ")}>
        {"global_prefs_pause".localize}
      </h4>
      <label>
        <input type="checkbox" checked={b(!_.runOnBatteries)} id="run_on_batteries"></input>
        {"global_prefs_on_batteries".localize}
      </label>
      <br/>
      <label>
        <input type="checkbox" checked={b(!_.runIfUserActive)} id="run_if_active"></input>
        {"global_prefs_cpu_active".localize}
      </label>
      <br/>
      <label>
        <input type="checkbox" checked={b(!_.runGPUIfUserActive)} id="run_gpu_if_active"></input>
        {"global_prefs_gpu_active".localize}
      </label>
      <br/>

      <h4 class={Seq(BoincClientStyle.h4, Style.h4Padding).map(_.htmlClass).mkString(" ")}>
        {"global_prefs_save_time".localize}
      </h4>
      <label for="workBufferDays">{"global_prefs_workbuffer_days".localize}</label>
      <input class={Style.input.htmlClass} id="workBufferDays" value={v(_.workBufferMinDays)}></input>
      <br/>
      <label for="workBufferAdd">{"global_prefs_add_buffer_days".localize}</label>
      <input class={Style.input.htmlClass} id="workBufferAdd" value={v(_.workBufferAdditionalDays)}></input>
      <br/>
      <label for="diskInterval">{"global_prefs_disk_interval".localize}</label>
      <input class={Style.input.htmlClass} id="diskInterval" value={v(_.diskInterval)}></input>
      <br/>

      <h4 class={Seq(BoincClientStyle.h4, Style.h4Padding).map(_.htmlClass).mkString(" ")}>
        {"global_prefs_network".localize}
      </h4>
      <label for="maxBytesDown">{"global_prefs_max_bytes_down".localize}</label>
      <input class={Style.input.htmlClass} id="maxBytesDown" value={v(_.maxBytesSecDownload.toSpeedValue(1))}></input>
      <br/>
      <label for="maxBytesUp">{"global_prefs_max_bytes_up".localize}</label>
      <input class={Style.input.htmlClass} id="maxBytesUp" value={v(_.maxBytesSecUpload.toSpeedValue(1))}></input>
      <br/>
      <label for="maxBytes">{"global_prefs_max_bytes".localize}</label>
      <input class={Style.input.htmlClass} id="maxBytes" value={v(_.dailyXFerLimitMB)}></input>
      <br/>
      <label for="maxBytesPeriod">{"global_prefs_max_bytes_period".localize}</label>
      <input class={Style.input.htmlClass} id="maxBytesPeriod" value={v(_.dailyXFerPeriodDays)}></input>
      <br/>
      <label>
        <input type="checkbox" checked={b(!_.runGPUIfUserActive)} id="run_gpu_if_active"></input>
        {"global_prefs_gpu_active".localize}
      </label>
      <br/>
      <label>
        <input type="checkbox" checked={b(_.dontVerifyImages)} id="dont_verify_images"></input>
        {"global_prefs_dont_verify_images".localize}
      </label>
      <br/>

      <h4 class={Seq(BoincClientStyle.h4, Style.h4Padding).map(_.htmlClass).mkString(" ")}>
        {"global_prefs_disk".localize}
      </h4>
      <label for="min_disk_free">{"global_prefs_min_disk_free".localize}</label>
      <input class={Style.input.htmlClass} id="min_disk_free" value={v(_.diskMinFreeGB)}></input>
      <br/>
      <label for="max_disk_usage">{"global_prefs_max_disk_usage".localize}</label>
      <input class={Style.input.htmlClass} id="max_disk_usage" value={v(_.diskMaxUsedGB)}></input>
      <br/>
      <label for="max_disk_usage_pct">{"global_prefs_max_disk_usage_pct".localize}</label>
      <input class={Style.input.htmlClass} id="max_disk_usage_pct" value={v(_.diskMaxUsedPct)}></input>
      <br/>

      <h4 class={Seq(BoincClientStyle.h4, Style.h4Padding).map(_.htmlClass).mkString(" ")}>
        {"global_prefs_memory".localize}
      </h4>
      <label for="ram_used_busy">{"global_prefs_ram_used_busy".localize}</label>
      <input class={Style.input.htmlClass} id="ram_used_busy" value={v(_.ramUsedBusyPct)}></input>
      <br/>
      <label for="ram_used_idle">{"global_prefs_ram_used_idle".localize}</label>
      <input class={Style.input.htmlClass} id="ram_used_idle" value={v(_.ramUsedIdlePct)}></input>
      <br/>
      <label for="max_disk_usage_pct">
        <input type="checkbox" id="leave_apps_in_memory" value={b(_.leaveAppsInMemory)}></input>
        {"global_prefs_leave_apps_in_memory".localize}
      </label>
      <br/>

    </div>
  }

  override def onRender(): Unit = {
    boinc.getGlobalPrefsOverride
      .map(f => this.globalPrefsOverride := f)
      .recover(ErrorDialogUtil.showDialog)
      .foreach(_ => NProgress.done(true))
  }

  private val jsOnSubmitListener: (Event) => Unit = (event) => {
    NProgress.start()

    val globalPrefsOverride = this.globalPrefsOverride.now

    boinc.setGlobalPrefsOverride(
      GlobalPrefsOverride(
        !getHTMLInputElement("run_on_batteries").checked,
        globalPrefsOverride.batteryChargeMinPct,
        globalPrefsOverride.batteryMaxTemperature,
        !getHTMLInputElement("run_if_active").checked,
        !getHTMLInputElement("run_gpu_if_active").checked,
        globalPrefsOverride.idleTimeToRun,
        globalPrefsOverride.suspendCpuUsage,
        getHTMLInputElement("leave_apps_in_memory").checked,
        getHTMLInputElement("dont_verify_images").checked,
        getHTMLInputElement("workBufferDays").value.toDouble,
        getHTMLInputElement("workBufferAdd").value.toDouble,
        getHTMLInputElement("NcpuPct").value.toDouble,
        getHTMLInputElement("shedPeriod").value.toDouble,
        getHTMLInputElement("diskInterval").value.toDouble,
        getHTMLInputElement("max_disk_usage").value.toDouble,
        getHTMLInputElement("max_disk_usage_pct").value.toDouble,
        getHTMLInputElement("min_disk_free").value.toDouble,
        getHTMLInputElement("ram_used_busy").value.toDouble,
        getHTMLInputElement("ram_used_idle").value.toDouble,
        getHTMLInputElement("maxBytesUp").value.toDouble.fromSpeedValue(1),
        getHTMLInputElement("maxBytesDown").value.toDouble.fromSpeedValue(1),
        getHTMLInputElement("cpuUsageLimit").value.toDouble,
        getHTMLInputElement("maxBytes").value.toDouble,
        getHTMLInputElement("maxBytesPeriod").value.toInt,
        globalPrefsOverride.networkWifiOnly,
        globalPrefsOverride.cpuTime,
        globalPrefsOverride.netTime,
        globalPrefsOverride.cpuTimes,
        globalPrefsOverride.netTimes
      )
    ).map(result => {

      if (!result) {
        NProgress.done(true)

        new OkDialog("__SET_GLOBAL_OVERRIDE_PREFS", List("RESULT => false")).renderToBody().show()
      } else {
        boinc.readGlobalPrefsOverride.map(result => {
          NProgress.done(true)

          if (!result) {
            new OkDialog("__READ_GLOBAL_OVERRIDE_PREFS", List("RESULT => false")).renderToBody().show()
          }
        }).recover(ErrorDialogUtil.showDialog)
      }

    }).recover(ErrorDialogUtil.showDialog)
  }

  private def getHTMLInputElement(elementID: String): HTMLInputElement =
    dom.document.getElementById(elementID).asInstanceOf[HTMLInputElement]
  
  private def checkbox(checkStatus: Boolean, cID: String): Node =
    <input type="checkbox" checked={if(checkStatus) Some("checked") else None} id={cID}/>

}
