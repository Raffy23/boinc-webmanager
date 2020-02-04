package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.css.CSSIdentifier
import at.happywetter.boinc.web.css.definitions.components.TableTheme
import at.happywetter.boinc.web.css.definitions.pages.BoincClientStyle
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.XMLHelper.toXMLTextNode
import at.happywetter.boinc.web.pages.boinc.BoincClientLayout
import at.happywetter.boinc.web.pages.component.DataTable.TableRow
import mhtml.{Rx, Var}
import org.scalajs.dom.raw.{Event, HTMLElement}

import scala.xml.{Elem, Node, Text}
import at.happywetter.boinc.web.helper.ScalaCSS._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.09.2017
  */
object DataTable {

  abstract class TableColumn(val content: Rx[Node], val datasource: TableRow) extends Ordered[TableColumn] {}
  class StringColumn(val source: Rx[String]) extends TableColumn(content = source.map(Text), null) {
    override def compare(that: TableColumn): Int = source.now.compare(that.asInstanceOf[StringColumn].source.now)
  }
  class DoubleColumn(val source: Rx[Double]) extends TableColumn(content = source.map(d => d.toString), null) {
    override def compare(that: TableColumn): Int = source.now.compare(that.asInstanceOf[DoubleColumn].source.now)
  }

  abstract class TableRow {

    val columns: List[TableColumn]
    val contextMenuHandler: (Event) => Unit = (_) => {}

    lazy val htmlRow: Elem = {
      <tr oncontextmenu={contextMenuHandler}>
        {columns.map(column => <td>{column.content}</td>)}
      </tr>
    }
  }
}

class DataTable[T <: TableRow](headers: List[(String, Boolean)],
                               val tableData: List[T] = List.empty,
                               tableStyle: List[CSSIdentifier] = List(TableTheme.table, TableTheme.lastRowSmall)) {

  val reactiveData: Var[List[T]] = Var(tableData)

  lazy val component: Elem = {
    <table class={tableStyle.map(_.htmlClass).mkString(" ")}>
      <thead>
        <tr>
          {
            headers.zipWithIndex.map { case ((header, sortable), idx) =>
              if (sortable)
                <th class={BoincClientStyle.inTextIcon.htmlClass}
                    style="cursor:pointer" onclick={tableSortFunction(idx)}>
                  <i class="fa fa-sort" data-sort-icon="icon" aria-hidden="true"></i>
                  {header}
                </th>
              else
                <th>{header}</th>
            }
          }
        </tr>
      </thead>
      <tbody>
        {reactiveData.map(_.map(_.htmlRow))}
      </tbody>
    </table>
  }

  private def tableSortFunction(idx: Int): (Event) => Unit = (event) => {
    import at.happywetter.boinc.web.hacks.NodeListConverter.convNodeList

    var target = event.target.asInstanceOf[HTMLElement]
    while (target.nodeName != "TH") {
      target = target.parentNode.asInstanceOf[HTMLElement]
    }

    val icon = target.querySelector("i[data-sort-icon]")
    val sortState = icon.getAttribute("class")

    val allIcons = target.parentNode.asInstanceOf[HTMLElement].querySelectorAll("i[data-sort-icon]")
    allIcons.forEach((node, _, _ ) => node.asInstanceOf[HTMLElement].setAttribute("class","fa fa-sort"))

    if (sortState == "fa fa-sort" || sortState == "fa fa-sort-up") {
      reactiveData.update(_.sortBy(_.columns(idx)))
      icon.setAttribute("class","fa fa-sort-down")
    } else {
      reactiveData.update(_.sortBy(_.columns(idx)).reverse)
      icon.setAttribute("class","fa fa-sort-up")
    }
  }
}

