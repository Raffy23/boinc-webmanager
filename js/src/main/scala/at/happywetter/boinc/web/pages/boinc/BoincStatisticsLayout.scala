package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.shared.{DailyStatistic, Statistics}
import at.happywetter.boinc.web.boincclient.{BoincClient, FetchResponseException}
import at.happywetter.boinc.web.chartjs._
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.boinc.BoincStatisticsLayout.Style
import at.happywetter.boinc.web.pages.component.BoincPageLayout
import at.happywetter.boinc.web.pages.component.dialog.OkDialog
import at.happywetter.boinc.web.storage.ProjectNameCache
import at.happywetter.boinc.web.util.ErrorDialogUtil
import at.happywetter.boinc.web.util.I18N._
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLCanvasElement, HTMLInputElement}
import org.scalajs.dom.{CanvasRenderingContext2D, Event}

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.JSConverters._
import scala.xml.Elem
import scalacss.internal.mutable.StyleSheet
import scalacss.ProdDefaults._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.09.2017
  */
object BoincStatisticsLayout {
  object Style extends StyleSheet.Inline {
    import dsl._

    import scala.language.postfixOps

    val button = style(
      textDecoration := "none",
      outline.`0`,
      width(100 %%),
      border.`0`,
      padding(14 px),
      color(c"#333"),
      cursor.pointer,

      borderTop :=! "1px #AAA solid",
      borderRight :=! "1px #AAA solid",

      &.hover(
        backgroundColor(c"#c3daee")
      )
    )

    val active = style(
      backgroundColor(c"#c3daee")
    )
  }
}

class BoincStatisticsLayout(params: js.Dictionary[String]) extends BoincPageLayout(_params = params) {

  private val DAY = 24*60*60

  private sealed trait State
  private case object USER_TOTAL extends State
  private case object USER_AVG extends State
  private case object HOST_TOTAL extends State
  private case object HOST_AVG extends State


  private var statData: Statistics = _
  private val projectNameCache = new mutable.HashMap[String, String]()
  private val projectUrlCache = new mutable.HashMap[String, String]()
  private val projectColors = new mutable.HashMap[String, String]()
  private var chart: ChartJS = _
  private var currentDataSet: State = HOST_AVG


  override def onRender(client: BoincClient): Unit = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    client.getStatistics.map(stats => {
      this.statData = stats


      ProjectNameCache.getAll(stats.stats.keys.toList).foreach( projectNameData => {
        projectNameData.foreach{ case (url, nameOpt) => {
          projectNameCache += ((url, nameOpt.getOrElse(url)))
          projectUrlCache += ((nameOpt.getOrElse(url), url))
        }}
        projectNameData.map(_._1).zip(ChartColors.stream).foreach(projectColors.+=)

        root.appendChild(
          div( id := "boinc_statistics",
            h3(BoincClientLayout.Style.pageHeader, i(`class` := "fa fa-area-chart"), "statistics_header".localize),
            div( style := "display:inline-block;width:400px",
              div(style := "padding-bottom:14px;border-bottom:1px #AAA solid;",
                b("boinc_statistics_projects".localize)
              ),
              ul(
                stats.stats.map{ case (project, data) =>
                  li(
                    label( style := (if(data.size <= 2) "color:gray" else ""),
                      input(`type` := "checkbox", value := project, onclick := checkBoxOnClickFunction),
                      projectNameCache(project)
                    )
                  )
                }.toList
              )
            ),
            div( style := "display:inline-block;width:calc(100% - 490px); vertical-align: top;",
              div( style := "padding-bottom:14px;text-align:right;border-bottom:1px #AAA solid;",
                b("table_credits".localize, style := "float:left"),
                a("user_total_credit".localize, Style.button, style := "border-left: 1px #AAA solid",
                  onclick := { (event: Event) => { event.preventDefault(); renderChartData(USER_TOTAL)} } ,
                  href := "#user_total", id := "user_total"),
                a("user_avg_credit".localize, Style.button,
                  onclick := { (event: Event) => { event.preventDefault(); renderChartData(USER_AVG)} },
                  href := "#user_avg", id := "user_avg"),
                a("host_total_credit".localize, Style.button,
                  onclick := { (event: Event) => { event.preventDefault(); renderChartData(HOST_TOTAL)} },
                  href := "#host_total", id := "host_total"),
                a("host_avg_credit".localize, Style.button, Style.active, 
                  onclick := { (event: Event) => { event.preventDefault(); renderChartData(HOST_AVG)} },
                  href := "#host_avg", id := "host_avg"),
              ),
              canvas(
                width := "100%",
                height := "600px",
                id := "chart-area",
                style := "margin-top:12px"
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
          }
          override val `type`: String = "line"
          override val options: ChartOptions = new ChartOptions {
            tooltips.display = false
          }
        })

        toggleActiveBtnClass(currentDataSet)
      })
    }).recover(ErrorDialogUtil.showDialog)
  }



  private def getMinMax(dataset: List[DailyStatistic] = List()): (Double, Double) = {
    val globalDataset = chart.data.datasets.toList.map(p => projectUrlCache(p.label)).flatMap(statData.stats(_))
    val data = globalDataset ++ dataset
    if (data.isEmpty)
      return (new Date().getTime(), new Date().getTime())

    val min = data.minBy(_.day)
    val max = data.maxBy(_.day)

    (min.day, max.day)
  }

  private def transformDataset(dataset: List[DailyStatistic], min: Double, max: Double): Seq[(Double, DailyStatistic)] = {
    var lastEntry = dataset.minBy(_.day)
    (min to max by DAY).map(time => {
      lastEntry = dataset.find(_.day == time).getOrElse(lastEntry)
      (time, lastEntry)
    })
  }

  private def getData(s: DailyStatistic): Double = currentDataSet match {
    case USER_AVG => s.userAvg
    case USER_TOTAL => s.userTotal
    case HOST_AVG => s.hostAvg
    case HOST_TOTAL => s.hostTotal
  }

  private def reRenderOldDataset(dataset: Dataset, min: Double, max: Double): Unit = {
    dataset.data = transformDataset(statData.stats(projectUrlCache(dataset.label)), min, max)
      .map{ case (_, data) => getData(data) }.toJSArray.asInstanceOf[js.Array[js.Any]]
  }

  def handleProject(project: String, state: Boolean): Unit = {
    if (state) {
      val minMax = getMinMax(statData.stats(project))
      val tData = transformDataset(statData.stats(project), minMax._1, minMax._2)

      chart.data.labels = tData.map{ case (time, _) => new Date(time*1000).toLocaleDateString() }.toJSArray

      val chartData = new Dataset {
        data = tData.map{ case (_, data) => getData(data) }.toJSArray.asInstanceOf[js.Array[js.Any]]
        label = projectNameCache(project)
        borderColor = List(projectColors(project)).toJSArray
        backgroundColor = List(projectColors(project)).toJSArray

        fill = false.toString
        borderWidth = 3D
      }

      this.chart.data.datasets.foreach(d => reRenderOldDataset(d, minMax._1, minMax._2))
      this.chart.data.datasets.push(chartData)
    } else {
      this.chart.data.datasets
        .filter(_.label == projectNameCache(project))
        .map(chart.data.datasets.indexOf)
        .map(chart.data.datasets.remove)

      val minMax = getMinMax()
      this.chart.data.datasets.foreach(d => reRenderOldDataset(d, minMax._1, minMax._2))
    }

    this.chart.update()
  }

  private val checkBoxOnClickFunction: js.Function1[Event, Unit] = (event) => {
    val project = event.target.asInstanceOf[HTMLInputElement].value
    val state   = event.target.asInstanceOf[HTMLInputElement].checked

    this.handleProject(project, state)
  }

  private def renderChartData(newState: State): Unit = {
    toggleActiveBtnClass(newState)
    currentDataSet = newState

    val minMax = getMinMax()
    this.chart.data.datasets.foreach(d => reRenderOldDataset(d, minMax._1, minMax._2))
    this.chart.update()
  }

  private def toggleActiveBtnClass(newState: State): Unit = {
    currentDataSet match {
      case USER_TOTAL => dom.document.getElementById("user_total").classList.remove(Style.active.htmlClass)
      case USER_AVG => dom.document.getElementById("user_avg").classList.remove(Style.active.htmlClass)
      case HOST_TOTAL => dom.document.getElementById("host_total").classList.remove(Style.active.htmlClass)
      case HOST_AVG => dom.document.getElementById("host_avg").classList.remove(Style.active.htmlClass)
    }

    newState match {
      case USER_TOTAL => dom.document.getElementById("user_total").classList.add(Style.active.htmlClass)
      case USER_AVG => dom.document.getElementById("user_avg").classList.add(Style.active.htmlClass)
      case HOST_TOTAL => dom.document.getElementById("host_total").classList.add(Style.active.htmlClass)
      case HOST_AVG => dom.document.getElementById("host_avg").classList.add(Style.active.htmlClass)
    }
  }

  override val path = "statistics"

  override def render: Elem = {<div>STAISTICS</div>}
}
