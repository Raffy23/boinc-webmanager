package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.BoincClientLayout
import at.happywetter.boinc.web.pages.component.DataTable.TableRow
import org.scalajs.dom
import org.scalajs.dom.raw.{Element, Event, HTMLElement}
import rx._

import scala.scalajs.js
import scalacss.internal.StyleA

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.09.2017
  */
object DataTable {
  import scalatags.JsDom.all._

  abstract class TableColumn(val content: Rx[scalatags.JsDom.Modifier], val datasource: TableRow) extends Ordered[TableColumn] {}
  class StringColumn(val source: Rx[String])(implicit ctx: Ctx.Owner) extends TableColumn(content = Rx { source() }, null) {
    override def compare(that: TableColumn): Int = source.now.compare(that.asInstanceOf[StringColumn].source.now)
  }
  class DoubleColumn(val source: Rx[Double])(implicit ctx: Ctx.Owner) extends TableColumn(content = Rx { source() }, null) {
    override def compare(that: TableColumn): Int = source.now.compare(that.asInstanceOf[DoubleColumn].source.now)
  }

  abstract class TableRow(implicit ctx: Ctx.Owner) {
    lazy val htmlRow: HTMLElement = {
      val root = dom.document.createElement("tr").asInstanceOf[HTMLElement]

      root.addEventListener("contetmenu", contextMenuHandler)
      transform(columns).foreach(root.appendChild)

      root
    }

    val columns: List[TableColumn]

    val contextMenuHandler: js.Function1[Event,Unit] = (_) => {}

    private def transform(raw: List[TableColumn]): List[Element] = {
      raw.map(data => {
        val column = dom.document.createElement("td")
        data.content.now.applyTo(column)

        data.content.foreach(colData => {
          column.innerHTML = ""
          colData.applyTo(column)
        })

        column
      })
    }
  }
}

class DataTable[T <: TableRow](headers: List[(String, Boolean)],val tableData: List[T], tableStyle: Option[StyleA] = None) {

  val reactiveData: Var[List[T]] = Var(tableData)
  private val tBody = dom.document.createElement("tbody")

  private val internalRX = Rx.unsafe {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    val root = table(TableTheme.table, tableStyle,
      thead(
        tr(
          headers.zipWithIndex.map { case ((header, sortable), idx) =>
            if (sortable)
              th(BoincClientLayout.Style.in_text_icon, style := "cursor:pointer",
                i(`class` := "fa fa-sort", data("sort-icon") := ""),
                header, onclick := tableSortFunction(idx))
            else th(header)
          }
        )
      )
    ).render

    reactiveData.foreach(data => {
      tBody.innerHTML = ""
      data.foreach(x => tBody.appendChild(x.htmlRow))
    })

    reactiveData.now.foreach(x => tBody.appendChild(x.htmlRow))
    root.appendChild(tBody)

    root
  }

  lazy val component: HTMLElement = internalRX.now

  private def tableSortFunction(idx: Int): js.Function1[Event,Unit] = (event) => {
    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList

    var target = event.target.asInstanceOf[HTMLElement]
    while (target.nodeName != "TH") {
      target = target.parentNode.asInstanceOf[HTMLElement]
    }

    val icon = target.querySelector("i[data-sort-icon]")
    val sortState = icon.getAttribute("class")

    val allIcons = target.parentNode.asInstanceOf[HTMLElement].querySelectorAll("i[data-sort-icon]")
    allIcons.forEach((node, _, _ ) => node.asInstanceOf[HTMLElement].setAttribute("class","fa fa-sort"))

    if (sortState == "fa fa-sort" || sortState == "fa fa-sort-desc") {
      reactiveData() = reactiveData.now.sortBy(_.columns(idx))
      icon.setAttribute("class","fa fa-sort-asc")
    } else {
      reactiveData() = reactiveData.now.sortBy(_.columns(idx)).reverse
      icon.setAttribute("class","fa fa-sort-desc")
    }
  }

  def dispose(): Unit = {
    internalRX.kill()
  }

}

