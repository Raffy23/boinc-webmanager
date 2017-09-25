package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.css.TableTheme
import at.happywetter.boinc.web.pages.component.DataTable.TableRow
import org.scalajs.dom
import org.scalajs.dom.raw.{Element, HTMLElement}
import rx._

import scalacss.internal.StyleA
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.09.2017
  */
object DataTable {

  abstract class TableRow(implicit ctx: Ctx.Owner) {
    lazy val htmlRow: HTMLElement = {
      val root = dom.document.createElement("tr").asInstanceOf[HTMLElement]
      transform(columns).foreach(root.appendChild)

      root
    }

    val columns: List[Rx[JsDom.TypedTag[HTMLElement]]]

    private def transform(raw: List[Rx[JsDom.TypedTag[HTMLElement]]]): List[Element] = {
      raw.map(data => {
        val column = dom.document.createElement("td")
        column.appendChild(data.now.render)

        data.foreach( colData => {
          column.innerHTML = ""
          column.appendChild(colData.render)
        })

        column
      })
    }
  }

}

class DataTable[T <: TableRow](headers: List[String],val tableData: List[T], tableStyle: Option[StyleA] = None) {

  val reactiveData: Var[List[T]] = Var(tableData)
  private val tBody = dom.document.createElement("tbody")

  lazy val component: HTMLElement = Rx.unsafe {
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    val root = table(TableTheme.table, tableStyle,
      thead(
        tr(
          headers.map { x => th(x) }
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
  }.now

}

