package at.happywetter.boinc.web.chartjs

import org.scalajs.dom.CanvasRenderingContext2D

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSGlobal, ScalaJSDefined}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.09.2017
  */
@js.native
@JSGlobal("Chart")
object ChartJS extends js.Object {

  val defaults: ChartOptions = js.native

}

@js.native
@JSGlobal("Chart")
class ChartJS(ctx: CanvasRenderingContext2D, config: ChartConfig) extends js.Object {

  val data: ChartData = js.native

  def update(duration: js.UndefOr[Double] = js.undefined, `lazy`: js.UndefOr[Boolean] = js.undefined): Unit = js.native

  def clear(): Unit = js.native

  def stop(): Unit = js.native

  def reset(): Unit = js.native

  def destroy(): Unit = js.native


  def toBase64Image(): String = js.native

  def generateLegend(): String = js.native

}

@ScalaJSDefined
abstract class ChartData extends js.Object {
  val labels: js.Array[String]
  val datasets: js.Array[Dataset]

}

@ScalaJSDefined
abstract class Dataset extends js.Object {
  val label: String
  val data: js.Array[js.Any]
  val backgroundColor: js.UndefOr[js.Array[String]] = js.undefined
  val borderColor: js.UndefOr[js.Array[String]] = js.undefined
  var borderWidth: Double = 1D
}

@ScalaJSDefined
abstract class ChartOptions extends js.Object {

  @ScalaJSDefined
  trait Legend extends js.Object {
    var display: Boolean
  }

  @ScalaJSDefined
  //http://www.chartjs.org/docs/latest/configuration/tooltip.html
  trait Tooltips extends js.Object {
    var display: Boolean
    val callbacks: TooltipCallbacks
  }

  @ScalaJSDefined
  trait TooltipCallbacks extends js.Object {
    var label: js.UndefOr[js.Function2[TooltipItem, ChartData, String]]
  }

  val legend: Legend = new Legend {
    override var display: Boolean = true
  }

  val tooltips: Tooltips = new Tooltips {
    override var display: Boolean = true
    override val callbacks = new TooltipCallbacks {
      override var label: UndefOr[js.Function2[TooltipItem, ChartData, String]] = js.undefined
    }
  }

}

@ScalaJSDefined
abstract class ChartConfig extends js.Object {

  val `type`: String
  val data: ChartData
  val options: ChartOptions

}

@js.native
trait TooltipItem extends js.Object {

  val xLabel: String
  val yLabel: String
  val datasetIndex: Number
  val index: Number
  val x: Number
  val y: Number

}