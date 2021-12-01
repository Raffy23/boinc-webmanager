package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.boincrpc.BoincRPC.Modes
import at.happywetter.boinc.shared.boincrpc.BoincRPC.Modes.Mode
import at.happywetter.boinc.shared.boincrpc.{CCState, HostInfo}
import at.happywetter.boinc.web.boincclient.BoincFormatter.Implicits._
import at.happywetter.boinc.web.boincclient.{BoincFormatter, ClientCacheHelper}
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.util.XMLHelper.toXMLTextNode
import at.happywetter.boinc.web.css.definitions.pages.{BoincMainHostStyle => Style}
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.storage.HostInfoCache
import at.happywetter.boinc.web.util.I18N._
import mhtml.{Rx, Var}
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLInputElement

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
class BoincMainHostLayout extends BoincClientLayout {

  override val path = "boinc"

  private val clientCC: Var[CCState] = Var(CCState(0,0,0,0,0,0D,0,0,0,0D,0,0,0,0D,true,true,0))
  private val clientData: Var[HostInfoCache.CacheEntry] = Var(HostInfoCache.CacheEntry.empty())

  override def already(): Unit = onRender()

  override def onRender(): Unit = {
    val data = HostInfoCache.get(boincClientName)
    boinc.getCCState.foreach(cc => clientCC := cc)

    if (data.isDefined) {
      clientData.update(_ => data.get)
      NProgress.done(true)
    } else {
      val dialog = new OkDialog("loading_dialog_content".localize, List("loading_dialog_content".localize))
      dialog.renderToBody().show()

      ClientCacheHelper.updateClientCache(boinc, _ => {
        clientData.update(_ => HostInfoCache.get(boincClientName).get)
        dialog.hide()
        NProgress.done(true)
      })
    }
  }

  override def render: Elem = {
    <div id="host-info">
      <h2 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fas fa-address-card" aria-hidden="true"></i>
        {"boinc_info_header".localize}
      </h2>

      <div id="boinc_cc_state">
        <h4 class={BoincClientStyle.h4WithoutLine.htmlClass}>
          <i class="fa fa-cogs" aria-hidden="true"></i>
          {"boinc_info_cc_state_header".localize}
        </h4>

        <table class={s"${TableTheme.table.htmlClass} ${TableTheme.noBorder.htmlClass}"} style="width:auto!important">
          <thead>
            <tr>
              <th></th>
              <th class={TableTheme.verticalText.htmlClass} style="width:24px">
                <div style="margin-bottom:-8px"><span>{"always".localize}</span></div>
              </th>
              <th class={TableTheme.verticalText.htmlClass} style="width:24px">
                <div style="margin-bottom:-8px"><span>{"auto".localize}</span></div>
              </th>
              <th class={TableTheme.verticalText.htmlClass} style="width:24px">
                <div style="margin-bottom:-8px"><span>{"never".localize}</span></div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td> <i class="fa fa-tasks" aria-hidden="true" style="display:inline-block;width:1em;padding-right:1em"></i> {"boinc_info_run_mode".localize} </td>
              <td> <input type="radio" name="run_mode" value="always" checked={clientCC.map(_.taskMode == 1)} onclick={action(boinc.setRun, Modes.Always)}></input> </td>
              <td> <input type="radio" name="run_mode" value="auto"   checked={clientCC.map(_.taskMode == 2)} onclick={action(boinc.setRun, Modes.Auto)}></input> </td>
              <td> <input type="radio" name="run_mode" value="never"  checked={clientCC.map(_.taskMode == 3)} onclick={action(boinc.setRun, Modes.Never)}></input> </td>
            </tr>
            <tr>
              <td> <i class="fa fa-tv" aria-hidden="true" style="display:inline-block;width:1em;padding-right:1em"></i> {"boinc_info_gpu_mode".localize} </td>
              <td> <input type="radio" name="gpu_mode" value="always" checked={clientCC.map(_.gpuMode == 1)} onclick={action(boinc.setGpu, Modes.Always)}></input> </td>
              <td> <input type="radio" name="gpu_mode" value="auto"   checked={clientCC.map(_.gpuMode == 2)} onclick={action(boinc.setGpu, Modes.Auto)}></input> </td>
              <td> <input type="radio" name="gpu_mode" value="never"  checked={clientCC.map(_.gpuMode == 3)} onclick={action(boinc.setGpu, Modes.Never)}></input> </td>
            </tr>
            <tr>
              <td> <i class="fa fa-exchange-alt" aria-hidden="true" style="display:inline-block;width:1em;padding-right:1em"></i> {"boinc_info_network_mode".localize} </td>
              <td> <input type="radio" name="network_mode" value="always" checked={clientCC.map(_.networkMode == 1)} onclick={action(boinc.setNetwork, Modes.Always)}></input> </td>
              <td> <input type="radio" name="network_mode" value="auto"   checked={clientCC.map(_.networkMode == 2)} onclick={action(boinc.setNetwork, Modes.Auto)}></input> </td>
              <td> <input type="radio" name="network_mode" value="never"  checked={clientCC.map(_.networkMode == 3)} onclick={action(boinc.setNetwork, Modes.Never)}></input> </td>
            </tr>
          </tbody>
        </table>
        <table class={s"${TableTheme.table.htmlClass} ${Style.table.htmlClass}"} style="line-height:1.4567">
          <tbody>
            <tr><td style="width:55px"><b>{"boinc_info_version".localize}</b></td><td>{clientData.map(_.boincVersion)}</td></tr>
            <tr><td><b>{"boinc_info_domain".localize}</b></td><td>{clientData.map(_.hostInfo.domainName)}</td></tr>
            <tr><td><b>{"boinc_info_os".localize}</b></td>
                <td>{clientData.map(_.hostInfo.osName)}<br/>
                  <small>{clientData.map(_.hostInfo.osVersion)}</small>
                </td>
            </tr>
            <tr><td><b>{"boinc_info_cpu".localize}</b></td>
                <td>{clientData.map(_.hostInfo.cpuVendor)}<br/>
                  <small>{clientData.map(_.hostInfo.cpuModel)}</small><br/>
                  <small><small>{clientData.map(_.hostInfo.cpuFeatures.mkString(", "))}</small></small>
                </td>
            </tr>
            <tr><td><b>{"boinc_info_#cpu".localize}</b></td><td>{clientData.map(_.hostInfo.cpus)}</td></tr>
            <tr><td><b>{"boinc_info_ip".localize}</b></td><td>{clientData.map(_.hostInfo.ipAddr)}</td></tr>
            <tr><td><b>{"boinc_info_ram".localize}</b></td>
              <td>{clientData.map(x => BoincFormatter.convertSize(x.hostInfo.memory))}</td>
            </tr>
            <tr><td><b>{"boinc_info_swap".localize}</b></td>
              <td>{clientData.map(x => BoincFormatter.convertSize(x.hostInfo.swap))}</td>
            </tr>
            <tr><td><b>{"boinc_info_disk".localize}</b></td> <td>
              <span class={BoincClientStyle.progressBar.htmlClass} >
              <progress style="width:250px" value={progressBarValue} max={clientData.map(_.hostInfo.diskTotal.toString)}></progress>
              </span>
              <br/>
              {
                clientData.map(x =>
                  "boinc_info_disk_content".localize.format(x.hostInfo.diskFree.toSize, x.hostInfo.diskTotal.toSize)
                )
              }</td></tr>
            <tr><td><b>{"boinc_info_platfrom".localize}</b></td><td>{clientData.map(_.platform)}</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  }

  private def progressBarValue: Rx[String] = {
    val total = clientData.map(_.hostInfo.diskTotal)
    val free  = clientData.map(_.hostInfo.diskFree)

    total.zip(free).map(f => (f._1 - f._2).toString)
  }

  private def action(function: (Modes.Value, Double) => Future[Boolean], mode: Mode): (Event) => Unit = (event) => {
    event.preventDefault()
    NProgress.start()

    function(mode, 0).foreach(result => {
      NProgress.done(true)
      if (result)
        event.target.asInstanceOf[HTMLInputElement].checked = true
    })
  }
}
