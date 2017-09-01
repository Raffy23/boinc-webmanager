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
          canvas(
            width := "100%",
            height := "600px",
            id := "chart-area"
          ),
          table(TableTheme.table,
            thead(
              tr(
                th("table_project".localize), th("table_usage".localize)
              )
            ),
            tbody(
              usage.diskUsage.map { case (name, usage) => {
                tr(
                  td(data("project-url") := name, name), td(BoincFormater.convertSize(usage))
                )
              }}.toList
            )
          )
        ).render
      )

      usage.diskUsage.keys.foreach( url =>
        ProjectNameCache.get(url).foreach(o =>
          o.foreach(name => {
            projectNames.put(url, name)
            dom.document.querySelector("#disk_usage table td[data-project-url='"+url+"']").textContent = name
          })
        )
      )

      val context =
        dom.document.getElementById("chart-area")
          .asInstanceOf[HTMLCanvasElement]
          .getContext("2d")
          .asInstanceOf[CanvasRenderingContext2D]

      import js.JSConverters._
      new ChartJS(context, new ChartConfig {
        override val data: ChartData = new ChartData {
          override val datasets: js.Array[Dataset] = List(new Dataset {
            override val data: js.Array[js.Any] = usage.diskUsage.map { case (_, value) => value }.toJSArray.asInstanceOf[js.Array[js.Any]]
            override val label: String = "disk_usage_legend".localize
          }).toJSArray

          override val labels: js.Array[String] = usage.diskUsage.map { case (name, _) => name }.toJSArray
        }
        override val `type`: String = "pie"
        override val options: ChartOptions = new ChartOptions {
          legend.display = false
          tooltips.callbacks.label = tooltipLabel
        }
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

}
