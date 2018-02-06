package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.BoincRPC
import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater, ClientCacheHelper}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.BoincClientLayout.Style
import at.happywetter.boinc.web.pages.component.dialog.{OkDialog}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.storage.HostInfoCache
import at.happywetter.boinc.web.storage.HostInfoCache.CacheEntry
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.xml.Elem
import scalatags.JsDom

import at.happywetter.boinc.web.helper.XMLHelper.toXMLTextNode

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
class BoincMainHostLayout extends BoincClientLayout {
  override def onRender(client: BoincClient): Unit = {
    val data = HostInfoCache.get(boincClientName)
    if (data.isDefined) {
      buildUI(data.get, client)
      NProgress.done(true)
    } else {
      val dialog = new OkDialog("loading_dialog_content".localize, List("loading_dialog_content".localize))
      dialog.renderToBody().show()

      ClientCacheHelper.updateClientCache(boinc,(_) => {
        buildUI(HostInfoCache.get(boincClientName).get, client)
        dialog.hide()
        NProgress.done(true)
      })
    }
  }

  private def buildUI(boincData: CacheEntry, boincClient: BoincClient): Unit = {
    root.appendChild(renderView(boincData))
    boincClient.getCCState.map(state => {
      state.gpuMode match {
        case 1 => dom.document.getElementById("gm-al").asInstanceOf[HTMLInputElement].checked = true
        case 2 => dom.document.getElementById("gm-au").asInstanceOf[HTMLInputElement].checked = true
        case 3 => dom.document.getElementById("gm-n").asInstanceOf[HTMLInputElement].checked = true
      }
      state.taskMode match {
        case 1 => dom.document.getElementById("rm-al").asInstanceOf[HTMLInputElement].checked = true
        case 2 => dom.document.getElementById("rm-au").asInstanceOf[HTMLInputElement].checked = true
        case 3 => dom.document.getElementById("rm-n").asInstanceOf[HTMLInputElement].checked = true
      }
      state.networkMode match {
        case 1 => dom.document.getElementById("nm-al").asInstanceOf[HTMLInputElement].checked = true
        case 2 => dom.document.getElementById("nm-au").asInstanceOf[HTMLInputElement].checked = true
        case 3 => dom.document.getElementById("nm-n").asInstanceOf[HTMLInputElement].checked = true
      }
    })
  }

  private def renderView(boincData: CacheEntry): HTMLElement = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    div( id := "host-info", Style.in_text_icon,
      h2(Style.pageHeader, i(`class` := "fa fa-id-card-o"), "boinc_info_header".localize),
      div( id := "boinc_cc_state",
        h4(Style.h4_without_line, i(`class` := "fa fa-cogs"), "boinc_info_cc_state_header".localize),
        table(TableTheme.table,  TableTheme.no_border, style := "width:auto!important",
          thead(
            tr(
              th(),
              th(TableTheme.vertical_table_text, div(span("always".localize), style := "margin-bottom:-8px"), style := "width:24px"),
              th(TableTheme.vertical_table_text, div(span("auto".localize), style := "margin-bottom:-8px"), style := "width:24px"),
              th(TableTheme.vertical_table_text, div(span("never".localize), style := "margin-bottom:-8px"), style := "width:24px"),
            )
          ),
          tbody(
            tr(
              td(i(`class` := "fa fa-tasks"), "boinc_info_run_mode".localize),
              td(input(`type` := "radio", name := "run_mode", value := "always", id:="rm-al",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  boinc.setRun(BoincRPC.Modes.Always).foreach(result => {
                    NProgress.done(true)
                    if (result)
                      event.target.asInstanceOf[HTMLInputElement].checked = true
                  })
                }}
              )),
              td(input(`type` := "radio", name := "run_mode", value := "auto", id:="rm-au",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  boinc.setRun(BoincRPC.Modes.Auto).foreach(result => {
                    NProgress.done(true)
                    if (result)
                      event.target.asInstanceOf[HTMLInputElement].checked = true
                  })
                }})),
              td(input(`type` := "radio", name := "run_mode", value := "never", id:="rm-n",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  boinc.setRun(BoincRPC.Modes.Never).foreach(result => {
                    NProgress.done(true)
                    if (result)
                      event.target.asInstanceOf[HTMLInputElement].checked = true
                  })
                }}))
            ),
            tr(td(i(`class` := "fa fa-television"), "boinc_info_gpu_mode".localize),
              td(input(`type` := "radio", name := "gpu_mode", value := "always", id:="gm-al",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  boinc.setGpu(BoincRPC.Modes.Always).foreach(result => {
                    NProgress.done(true)
                    if (result)
                      event.target.asInstanceOf[HTMLInputElement].checked = true
                  })
                }})),
              td(input(`type` := "radio", name := "gpu_mode", value := "auto", id:="gm-au",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  boinc.setGpu(BoincRPC.Modes.Auto).foreach(result => {
                    NProgress.done(true)
                    if (result)
                      event.target.asInstanceOf[HTMLInputElement].checked = true
                  })
                }})),
              td(input(`type` := "radio", name := "gpu_mode", value := "never", id:="gm-n",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  boinc.setGpu(BoincRPC.Modes.Never).foreach(result => {
                    NProgress.done(true)
                    if (result)
                      event.target.asInstanceOf[HTMLInputElement].checked = true
                  })
                }}))
            ),
            tr(td(i(`class` := "fa fa-exchange"), "boinc_info_network_mode".localize),
              td(input(`type` := "radio", name := "network_mode", value := "always", id:="nm-al",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  boinc.setNetwork(BoincRPC.Modes.Always).foreach(result => {
                    NProgress.done(true)
                    if (result)
                      event.target.asInstanceOf[HTMLInputElement].checked = true
                  })
                }})),
              td(input(`type` := "radio", name := "network_mode", value := "auto", id:="nm-au",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  boinc.setNetwork(BoincRPC.Modes.Auto).foreach(result => {
                    NProgress.done(true)
                    if (result)
                      event.target.asInstanceOf[HTMLInputElement].checked = true
                  })
                }})),
              td(input(`type` := "radio", name := "network_mode", value := "never", id:="nm-n",
                onclick := { (event: Event) => {
                  event.preventDefault()
                  NProgress.start()

                  boinc.setNetwork(BoincRPC.Modes.Never).foreach(result => {
                    NProgress.done(true)
                    if (result)
                      event.target.asInstanceOf[HTMLInputElement].checked = true
                  })
                }}))
            )
          )
        )
      ),
      table(TableTheme.table, style:="line-height: 1.4567", BoincMainHostLayout.Style.table,
       tbody(
         tr(td(style:="width:55px;",b("boinc_info_version".localize)), td(boincData.boincVersion)),
         tr(td(b("boinc_info_domain".localize)), td(boincData.hostInfo.domainName)),
         tr(td(b("boinc_info_os".localize)), td(boincData.hostInfo.osName,br(),small(boincData.hostInfo.osVersion))),
         tr(td(b("boinc_info_cpu".localize)), td(boincData.hostInfo.cpuVendor,br(),small(boincData.hostInfo.cpuModel),br(),small(small(boincData.hostInfo.cpuFeatures.mkString(", "))))),
         tr(td(b("boinc_info_#cpu".localize)), td(boincData.hostInfo.cpus)),
         tr(td(b("boinc_info_ip".localize)), td(boincData.hostInfo.ipAddr)),
         tr(td(b("boinc_info_ram".localize)), td(BoincFormater.convertSize(boincData.hostInfo.memory))),
         tr(td(b("boinc_info_swap".localize)), td(BoincFormater.convertSize(boincData.hostInfo.swap))),
         tr(td(b("boinc_info_disk".localize)), td(Style.progressBar, JsDom.tags2.progress(style := "width:250px;", value := boincData.hostInfo.diskTotal-boincData.hostInfo.diskFree, max := boincData.hostInfo.diskTotal),br(),
           "boinc_info_disk_content".localize.format(BoincFormater.convertSize(boincData.hostInfo.diskFree), BoincFormater.convertSize(boincData.hostInfo.diskTotal))
         )),
         tr(td(b("boinc_info_platfrom".localize)), td(boincData.platform)),
       )
      )
    ).render
  }

  override val path = "boinc"

  override def render: Elem = {<div>HOST_MAIN</div>}
}

object BoincMainHostLayout {
  import scala.language.postfixOps
  import scalacss.ProdDefaults._

  object Style extends StyleSheet.Inline {
    import dsl._

    val table = style(
      unsafeChild("tbody>tr>td:first-child")(
        width(200 px)
      ),

      unsafeChild("tbody>tr>td")(
        padding(8 px).important
      )
    )
  }
}
