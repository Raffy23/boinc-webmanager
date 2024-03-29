package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.boincrpc.DiskUsage
import at.happywetter.boinc.web.boincclient.BoincFormatter
import at.happywetter.boinc.web.boincclient.BoincFormatter.Implicits._
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.chartjs._
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.routes.NProgress
import at.happywetter.boinc.web.util.RichRx._
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.I18N._
import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom.raw.HTMLCanvasElement

import scala.concurrent.Future
import scala.scalajs.js
import scala.xml.Elem
import Ordering.Double.TotalOrdering
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by: 
  *
  * @author Raphael
  * @version 30.08.2017
  */
class BoincDiskLayout extends BoincClientLayout {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  override val path = "disk"

  private val data = Var(Option.empty[DiskUsage])
  private val names = Var(Map.empty[String, String])

  private def getSize(f: (DiskUsage) => Double): Rx[Option[String]] =
    data.map(opt => opt.map(x => BoincFormatter.convertSize(f(x))))

  override def render: Elem = {
    boinc.getDiskUsage.foreach(diskUsage => {
      data := Some(diskUsage)

      Future.sequence(
        diskUsage.diskUsage.keys.map(url => ProjectNameCache.get(url).map(f => (url, f)))
      ).map(names => names.map{ case (url, nameOpt) => (url, nameOpt.getOrElse(url)) })
       .map(_.toMap)
       .map(x => names := x)
       .foreach(_ => {
         buildChart(diskUsage)
         NProgress.done(true)
       })
    })

    <div>
      <h3 class={BoincClientStyle.pageHeader.htmlClass}>
        <i class="fas fa-chart-pie" aria-hidden="true"></i>
        {"disk_usage".localize}
      </h3>

      <div>
        <div style="display:inline-block;width:405px">
          <table class={TableTheme.table.htmlClass} style="width:400px">
            <tbody>
              <tr><td><b>{"disk_usage_free".localize}</b></td><td>{getSize(_.free)}</td></tr>
              <tr><td><b>{"disk_usage_allowed".localize}</b></td><td>{getSize(_.allowed)}</td></tr>
              <tr><td><b>{"disk_usage_boinc".localize}</b></td><td>{getSize(_.boinc)}</td></tr>
              <tr><td><b>{"disk_usage_total".localize}</b></td><td>{getSize(_.total)}</td></tr>
            </tbody>
          </table>
          <table class={TableTheme.table.htmlClass} style="width:400px;margin-top:20px">
            <thead>
              <tr>
                <th></th><th>{"table_project".localize}</th><th>{"table_usage".localize}</th>
              </tr>
            </thead>
            <tbody>
              {
                data.map(_.map(usage => {
                  usage.diskUsage.zip(ChartColors.stream).toList.sortBy(f => -f._1._2).map{
                    case ((name, usage), color) =>
                      <tr>
                        <td style="width:32px"><div style={"height:24px;width:24px;background-color:"+color}></div></td>
                        <td >{names.map(_.getOrElse(name, name))}</td>
                        <td>{BoincFormatter.convertSize(usage)}</td>
                      </tr>
                  }
                }))
              }
            </tbody>
          </table>
        </div>
        <div style="display:inline-block;width:calc(100% - 500px);padding-right:90px;vertical-align:top;max-height:600px">
          <canvas width="100" height="600" id="chart-area"></canvas>
        </div>
      </div>
    </div>
  }

  private def buildChart(usage: DiskUsage): Unit = {
    val context =
      dom.document.getElementById("chart-area")
        .asInstanceOf[HTMLCanvasElement]
        .getContext("2d")
        .asInstanceOf[CanvasRenderingContext2D]

    import js.JSConverters._
    new ChartJS(context, new ChartConfig {
      override val data: ChartData = new ChartData {
        override val datasets: js.Array[Dataset] = List(new Dataset {
          data = usage.diskUsage.map { case (_, value) => value }.toJSArray.asInstanceOf[js.Array[js.Any]]
          backgroundColor = ChartColors.stream.take(usage.diskUsage.size).toJSArray
          label = "disk_usage_legend".localize
        }).toJSArray

        labels = usage.diskUsage.map { case (name, _) => name }.toJSArray
      }
      override val `type`: String = "pie"
      override val options: ChartOptions = new ChartOptions {
        legend.display = false
        tooltips.callbacks.label = tooltipLabel
        maintainAspectRatio = false
      }
    })
  }

  private def getTooltipLabel(tooltipItem: TooltipItem, data: ChartData): String = {
    val idx = tooltipItem.index.intValue()
    names.map(x => x(data.labels(idx))).now
  }

  private def getTooltipValue(tooltipItem: TooltipItem, data: ChartData): String = {
    val idx = tooltipItem.index.intValue()
    this.data.map(_.map(u => u.diskUsage(data.labels(idx))).getOrElse(0D)).now.toSize
  }

  private val tooltipLabel: js.Function2[TooltipItem, ChartData, String] = (tooltipItem, data) => {
    s"${getTooltipLabel(tooltipItem, data)}: ${getTooltipValue(tooltipItem, data)}"
  }

}
