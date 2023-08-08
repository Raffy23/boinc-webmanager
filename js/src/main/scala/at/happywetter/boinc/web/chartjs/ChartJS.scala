package at.happywetter.boinc.web.chartjs

import org.scalajs.dom.CanvasRenderingContext2D
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSImport}
import scala.scalajs.js.|

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.09.2017
  */
@js.native
@JSGlobal("Chart") //@JSImport("chart.js", JSImport.Namespace)
object ChartJS extends js.Object:

  val defaults: GlobalChartOptions = js.native

@js.native
trait GlobalChartOptions extends js.Object:

  val bar: js.Any = js.native
  val buuble: js.Any = js.native
  val doughnut: js.Any = js.native
  val global: ChartOptions = js.native
  val line: js.Any = js.native
  val pie: js.Any = js.native
  val radar: js.Any = js.native
  val scale: js.Any = js.native
  val scatter: js.Any = js.native

@js.native
@JSGlobal("Chart") //@JSImport("chart.js", JSImport.Namespace)
class ChartJS(ctx: CanvasRenderingContext2D, config: ChartConfig) extends js.Object:

  val data: ChartData = js.native

  def update(duration: js.UndefOr[Double] = js.undefined, `lazy`: js.UndefOr[Boolean] = js.undefined): Unit = js.native

  def clear(): Unit = js.native

  def stop(): Unit = js.native

  def reset(): Unit = js.native

  def destroy(): Unit = js.native

  def toBase64Image(): String = js.native

  def generateLegend(): String = js.native

abstract class ChartData extends js.Object:
  var labels: js.Array[String] = new js.Array(0)
  val datasets: js.Array[Dataset]

abstract class Dataset extends js.Object:
  var label: String = ""
  var data: js.Array[js.Any] = js.Array(0)
  var backgroundColor: js.UndefOr[String | js.Array[String]] = js.undefined
  var borderColor: js.UndefOr[String] = js.undefined
  var borderWidth: Double = 1d
  var fill: String = "origin"

abstract class ChartOptions extends js.Object:

  trait Legend extends js.Object:
    var display: Boolean

  // http://www.chartjs.org/docs/latest/configuration/tooltip.html
  trait Tooltips extends js.Object:
    var display: Boolean
    val callbacks: TooltipCallbacks

  // @ScalaJSDefined
  trait TooltipCallbacks extends js.Object:
    var label: js.Function2[TooltipItem, ChartData, String]

  val legend: Legend = new Legend:
    var display: Boolean = true

  val tooltips: Tooltips = new Tooltips:
    var display: Boolean = true
    val callbacks = new TooltipCallbacks:
      var label: js.Function2[TooltipItem, ChartData, String] = ChartJS.defaults.global.tooltips.callbacks.label

  var maintainAspectRatio: Boolean = true

abstract class ChartConfig extends js.Object:

  val `type`: String
  val data: ChartData
  val options: ChartOptions

@js.native
trait TooltipItem extends js.Object {

  val xLabel: String
  val yLabel: String
  val datasetIndex: Number
  val index: Number
  val x: Number
  val y: Number

}
