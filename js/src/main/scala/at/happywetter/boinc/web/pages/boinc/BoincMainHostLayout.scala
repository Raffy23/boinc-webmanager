package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater, ClientCacheHelper}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout.Style
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.pages.component.dialog.SimpleModalDialog
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.storage.HostInfoCache
import at.happywetter.boinc.web.storage.HostInfoCache.CacheEntry
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scalatags.JsDom
import at.happywetter.boinc.web.util.I18N._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2017
  */
class BoincMainHostLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {
  override def onRender(client: BoincClient): Unit = {
    val data = HostInfoCache.get(boincClientName)
    if (data.isDefined) {
      root.appendChild(renderView(data.get))
      NProgress.done(true)
    } else {
      import scalatags.JsDom.all._
      new SimpleModalDialog(div("loading_dialog_content".localize), h4("loading_dialog_header".localize), (_) => {}, (_) => {}).renderToBody().show()

      ClientCacheHelper.updateClientCache(boinc,(_) => {
        root.appendChild(renderView(HostInfoCache.get(boincClientName).get))
        SimpleModalDialog.remove()
        NProgress.done(true)
      })
    }
  }

  private def renderView(boincData: CacheEntry): HTMLElement = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    div( id := "host-info",
      h2(Style.pageHeader, "boinc_info_header".localize),
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
}

object BoincMainHostLayout {
  import scalacss.ProdDefaults._
  import scala.language.postfixOps

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
