package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.{BoincClient, BoincFormater, FetchResponseException}
import at.happywetter.boinc.web.chartjs._
import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom.raw.HTMLCanvasElement

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.js

/**
  * Created by: 
  *
  * @author Raphael
  * @version 30.08.2017
  */
class BoincDiskLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {

  private val projectNames: mutable.Map[String, String] = new mutable.HashMap[String, String]()

  override def onRender(client: BoincClient): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getDiskUsage.map(usage => {

      root.appendChild(
        div(id := "disk_usage",
          h3(BoincClientLayout.Style.pageHeader, "disk_usage".localize),
          div(
            div( style := "display:inline-block;width:405px",
            table(TableTheme.table, style := "width:400px",
              tbody(
                tr(td(b("disk_usage_free".localize)), td(BoincFormater.convertSize(usage.free))),
                tr(td(b("disk_usage_allowed".localize)), td(BoincFormater.convertSize(usage.allowed))),
                tr(td(b("disk_usage_boinc".localize)), td(BoincFormater.convertSize(usage.boinc))),
                tr(td(b("disk_usage_total".localize)), td(BoincFormater.convertSize(usage.total)))
              )
            ),

              table(TableTheme.table, style := "margin-top:20px; width:400px",
                thead(
                  tr(
                    th(), th("table_project".localize), th("table_usage".localize)
                  )
                ),
                tbody(
                  usage.diskUsage.zip(ChartColors.stream).toList.sortBy(f => -f._1._2).map { case ((name, usage), color) => {
                    tr(
                      td(style:="width:32px", div(style := "height:24px;width:24px;background-color:"+color)),
                      td(data("project-url") := name, name), td(BoincFormater.convertSize(usage))
                    )
                  }}.toList
                )
              )

            ),
            div( style := "display:inline-block;width:calc(100% - 495px);padding-right:90px;vertical-align: top;",
            canvas(
              width := "100%",
              height := "600px",
              id := "chart-area"
            ))
          )
        ).render
      )

      Future.sequence(
        usage.diskUsage.keys.map(url => ProjectNameCache.get(url).map(f => (url, f)))
      ).map(names => names.foreach { case (url, nameOpt) =>
        val name = nameOpt.getOrElse(url)

        projectNames.put(url, name)
        dom.document.querySelector("#disk_usage table td[data-project-url='"+url+"']").textContent = name
      }).foreach(_ => {
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
          }
        })
      })

    }).recover {
      case _: FetchResponseException =>
        new OkDialog("dialog_error_header".localize, List("server_connection_loss".localize))
          .renderToBody().show()
    }
  }

  private val tooltipLabel: js.Function2[TooltipItem, ChartData, String] = (tooltipItem, data) => {
    s"${projectNames(data.labels(tooltipItem.index.intValue()))}: ${BoincFormater.convertSize(data.datasets(tooltipItem.datasetIndex.intValue()).data(tooltipItem.index.intValue()).asInstanceOf[Double])}"
  }
  override val path = "disk"
}
