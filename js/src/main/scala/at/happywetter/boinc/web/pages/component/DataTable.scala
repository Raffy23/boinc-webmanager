package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.css.CSSIdentifier
import at.happywetter.boinc.web.css.definitions.Misc
import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle, Dialog, TableTheme}
import at.happywetter.boinc.web.css.definitions.pages.{BoincClientStyle, BoincProjectStyle}
import at.happywetter.boinc.web.helper.RichRx._
import at.happywetter.boinc.web.helper.XMLHelper.toXMLTextNode
import at.happywetter.boinc.web.pages.component.DataTable.TableRow
import at.happywetter.boinc.web.routes.AppRouter
import at.happywetter.boinc.web.util.I18N
import mhtml.{Rx, Var}
import org.scalajs.dom.raw.{Event, HTMLElement, HTMLInputElement}
import at.happywetter.boinc.web.util.I18N._

import scala.util.Random
import scala.xml.{Elem, Node, Text}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.09.2017
  */
object DataTable {

  abstract class TableColumn(val content: Rx[Node], val datasource: TableRow, val dataEntry: Option[String] = None) extends Ordered[TableColumn] {}

  class LinkColumn(val source: Rx[(String, String)]) extends TableColumn(
    source.map( data =>
      <a href={data._2} onclick={AppRouter.openExternal} class={BoincProjectStyle.link.htmlClass}>
        {data._1}
      </a>
    ), null
  ) {
    override def compare(that: TableColumn): Int = source.now._1.compareTo(that.asInstanceOf[LinkColumn].source.now._1)
  }

  class StringColumn(val source: Rx[String]) extends TableColumn(content = source.map(Text), null) {
    override def compare(that: TableColumn): Int = source.now.compare(that.asInstanceOf[StringColumn].source.now)
  }

  class DoubleColumn(val source: Rx[Double]) extends TableColumn(content = source.map("%.4f".format(_)), null, dataEntry = Some("number")) {
    override def compare(that: TableColumn): Int = source.now.compare(that.asInstanceOf[DoubleColumn].source.now)
  }

  val PageSizes: List[Int] = List(10, 20, 30, 40, 50, 100, 200)
  val DefaultPageSize = 30

  abstract class TableRow {

    val columns: List[TableColumn]
    val contextMenuHandler: (Event) => Unit = (_) => {}

    lazy val htmlRow: Elem = {
      <tr oncontextmenu={contextMenuHandler}>
        {columns.map(column => <td data-type={column.dataEntry}>{column.content}</td>)}
      </tr>
    }
  }
}

class DataTable[T <: TableRow](headers: List[(String, Boolean)],
                               val tableData: List[T] = List.empty,
                               tableStyle: List[CSSIdentifier] = List(TableTheme.table, TableTheme.lastRowSmall),
                               paged: Boolean = false) {

  val reactiveData: Var[List[T]] = Var(tableData)
  val currentPage: Var[Int] = Var(1)
  val curPageSize: Var[Int] = Var(DataTable.DefaultPageSize)

  lazy val component: Elem = {
    <div class={TableTheme.container.htmlClass}>
      {
        if(paged) {
          Some(
            <div>
              {
                "table_page_size_select".localizeTags(
                  <select onchange={onPageSizeChange}>
                    {
                    DataTable.PageSizes.map(s =>
                      <option value={s.toString} selected={if(s == curPageSize.now) Some("") else None}>{s.toString}</option>
                    )
                    }
                  </select>
                )
              }
            </div>
          )
        } else None
      }
      <table class={tableStyle.map(_.htmlClass).mkString(" ")}>
        <thead>
          <tr>
            {
              headers.zipWithIndex.map { case ((header, sortable), idx) =>
                if (sortable)
                  <th class={TableTheme.sortable.htmlClass} onclick={tableSortFunction(idx)}>
                    {header}
                    <i class="fa fa-sort" data-sort-icon="icon" aria-hidden="true"></i>
                  </th>
                else
                  <th>{header}</th>
              }
            }
          </tr>
        </thead>
        <tbody>
          {
          if(paged) {
            currentPage.zip(curPageSize).zip(reactiveData).map { case ((page, size), data) =>
              val start = (page-1) * size
              data.slice(start, start + size).map(_.htmlRow)
            }
          } else {
            reactiveData.map(_.map(_.htmlRow))
          }
          }
        </tbody>
      </table>
      {
        if(paged) {
          val pages = reactiveData.zip(curPageSize).map { case (data, size) => (data.length / size)+1 }

          Some(
            <div>
              <a onclick={prevPage}>
                <i class="fas fa-angle-left"></i>
                {"table_prev_page".localize}
              </a>
              {
              currentPage.zip(pages).map { case (page, pages) =>
                <input type="number" value={page.toString} onchange={onPageChange} min="1" max={(pages+1).toString}></input>
              }
              }
              / { pages }
              <a onclick={nextPage}>
                {"table_next_page".localize}
                <i class="fas fa-angle-right"></i>
              </a>
            </div>
          )
        } else None
      }
    </div>
  }

  private val onPageSizeChange: Event => Unit = { event =>
    curPageSize.update(_ => event.target.asInstanceOf[HTMLInputElement].value.toInt)
    currentPage.update { old =>
      if(reactiveData.now.length / curPageSize.now >= old) old
      else (reactiveData.now.length / curPageSize.now)+1
    }
  }

  private val onPageChange: Event => Unit = { event =>
    val value = event.target.asInstanceOf[HTMLInputElement].value.toInt
    if(value >= 1 && value <= reactiveData.now.length / curPageSize.now)
      currentPage := value
  }

  private val nextPage: Event => Unit = { _ =>
    currentPage.update { old =>
      if(reactiveData.now.length / curPageSize.now >= old) old + 1
      else old
    }
  }

  private def prevPage: Event => Unit = { _ =>
    currentPage.update { old =>
      if(old > 1) old - 1
      else old
    }
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

