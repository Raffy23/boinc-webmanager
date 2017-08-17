package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.{BoincClient, ClientCacheHelper}
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout.Style
import at.happywetter.boinc.web.pages.component.{BoincPageLayout, ModalDialog}
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.storage.HostInfoCache
import at.happywetter.boinc.web.storage.HostInfoCache.CacheEntry
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scalatags.JsDom

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
      new ModalDialog(div("Please wait ..."), h4("Loading"),(_) => {}, (_) => {}).renderToBody().show()

      ClientCacheHelper.updateClientCache(boinc,(_) => {
        root.appendChild(renderView(HostInfoCache.get(boincClientName).get))
        ModalDialog.remove()
        NProgress.done(true)
      })
    }
  }

  private def renderView(boincData: CacheEntry): HTMLElement = {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    div( id := "host-info",
      h2(Style.pageHeader, "Hostinfo: "),
      table(TableTheme.table, style:="line-height: 1.4567", BoincMainHostLayout.Style.table,
       tbody(
         tr(td(style:="width:55px;",b("Boinc Version")), td(boincData.boincVersion)),
         tr(td(b("Domain Name")), td(boincData.hostInfo.domainName)),
         tr(td(b("Betriebssystem")), td(boincData.hostInfo.osName,br(),small(boincData.hostInfo.osVersion))),
         tr(td(b("Prozessor")), td(boincData.hostInfo.cpuVendor,br(),small(boincData.hostInfo.cpuModel),br(),small(small(boincData.hostInfo.cpuFeatures.mkString(", "))))),
         tr(td(b("# CPU Kerne")), td(boincData.hostInfo.cpus)),
         tr(td(b("IP-Adresse (Lokal)")), td(boincData.hostInfo.ipAddr)),
         tr(td(b("RAM")), td(prettyPrintSize(boincData.hostInfo.memory))),
         tr(td(b("Swap")), td(prettyPrintSize(boincData.hostInfo.swap))),
         tr(td(b("Speicherplatz")), td(Style.progressBar, JsDom.tags2.progress(style := "width:250px;", value := boincData.hostInfo.diskTotal-boincData.hostInfo.diskFree, max := boincData.hostInfo.diskTotal),br(),
           prettyPrintSize(boincData.hostInfo.diskFree), " frei von ", prettyPrintSize(boincData.hostInfo.diskTotal))),
         tr(td(b("Platform (BOINC)")), td(boincData.platform)),
       )
      )
    ).render
  }

  private val labels = List("Bytes","KB","MB","GB","TB","PB")
  private def prettyPrintSize(size: Double): String = {
    var value = size
    var cnt   = 0

    while ((value/1024) >= 1) {
      value = value / 1024
      cnt = cnt + 1
    }

    s"${value.formatted("%.1f")} ${labels(cnt)}"
  }
}

object BoincMainHostLayout {
  import scalacss.DevDefaults._
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
