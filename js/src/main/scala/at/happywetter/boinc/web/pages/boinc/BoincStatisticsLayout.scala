package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.Statistics
import at.happywetter.boinc.web.boincclient.BoincClient
import at.happywetter.boinc.web.chartjs._
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.storage.ProjectNameCache

import scala.scalajs.js
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.{CanvasRenderingContext2D, Event}
import org.scalajs.dom.raw.{HTMLCanvasElement, HTMLInputElement}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.scalajs.js.UndefOr

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.09.2017
  */
class BoincStatisticsLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {

  private var statData: Statistics = _
  private val projectNameCache = new mutable.HashMap[String, String]()
  private val useDataForChart = new  mutable.HashMap[String, Int]()
  private var chart: ChartJS = _


  override def onRender(client: BoincClient): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getStatistics.map(stats => {
      this.statData = stats


      ProjectNameCache.getAll(stats.stats.keys.toList).foreach( projectNameData => {
        projectNameData.foreach{ case (url, nameOpt) => projectNameCache += ((url, nameOpt.getOrElse(url))) }

        root.appendChild(
          div( id := "boinc_statistics",
            h3(BoincClientLayout.Style.pageHeader, "statistics_header".localize),
            div( style := "display:inline-block;width:400px",
              ul(
                stats.stats.map{ case (project, _) =>
                  li(
                    label(
                      input(`type` := "checkbox", value := project, onclick := {
                        (event: Event) => {
                          val project = event.target.asInstanceOf[HTMLInputElement].value

                          if (event.target.asInstanceOf[HTMLInputElement].checked) {
                            import scala.scalajs.js.JSConverters._
                            val chartData = new Dataset {
                              override val data: js.Array[js.Any] = statData.stats(project).map(_.hostAvg).toJSArray.asInstanceOf[js.Array[js.Any]]
                              override val label: String = projectNameCache(project)
                            }

                            useDataForChart += ((project, this.chart.data.datasets.push(chartData)))
                          } else {
                            this.chart.data.datasets.jsSlice(useDataForChart(project), 1)
                            useDataForChart.remove(project)
                          }

                          this.chart.update()
                        }
                      }),
                      projectNameCache(project)
                    )
                  )
                }.toList
              )
            ),
            div( style := "display:inline-block;width:calc(100% - 490px); vertical-align: top;",
              canvas(
                width := "100%",
                height := "600px",
                id := "chart-area"
              )
            )
          ).render
        )

        val context =
          dom.document.getElementById("chart-area")
            .asInstanceOf[HTMLCanvasElement]
            .getContext("2d")
            .asInstanceOf[CanvasRenderingContext2D]

        this.chart = new ChartJS(context, new ChartConfig {
          override val data: ChartData = new ChartData {
            override val datasets: js.Array[Dataset] = new js.Array(0)
            override val labels: js.Array[String] = new js.Array(0)
          }
          override val `type`: String = "line"
          override val options: ChartOptions = new ChartOptions {}
        })


      })
    }).recover {
      case e: Exception => e.printStackTrace()
    }
  }

}
