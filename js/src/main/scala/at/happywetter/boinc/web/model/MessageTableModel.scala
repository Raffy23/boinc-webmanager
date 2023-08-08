package at.happywetter.boinc.web.model

import scala.xml.Elem

import at.happywetter.boinc.shared.boincrpc.Message
import at.happywetter.boinc.web.boincclient.BoincFormatter.Implicits._
import at.happywetter.boinc.web.css.definitions.pages.{BoincMessageStyle => Style}
import at.happywetter.boinc.web.pages.component.DataTable
import at.happywetter.boinc.web.pages.component.DataTable.{StringColumn, TableColumn, TableRow}
import at.happywetter.boinc.web.util.RichRx._
import at.happywetter.boinc.web.util.XMLHelper._

import mhtml.{Rx, Var}

/**
 * Created by 
 *
 * @author Raphael Ludwig
 * @version 14.02.20
 */
object MessageTableModel:

  private class DateColumn(val date: Rx[Double]) extends TableColumn(content = date.map(_.toDate.toXML), null):
    override def compare(that: TableColumn): Int = date.now.compare(that.asInstanceOf[DateColumn].date.now)

  def convert(message: Message): MessageTableRow = new MessageTableRow(Var(message))

  class MessageTableRow(val message: Var[Message]) extends TableRow:
    override val columns: List[DataTable.TableColumn] = List(
      new StringColumn(message.map(_.project)),
      new DateColumn(message.map(_.time.toDouble)),
      new StringColumn(message.map(_.msg))
    )

    override lazy val htmlRow: Elem =
      <tr class={Style.tableRow.htmlClass} data-prio={message.map(_.priority.toString)}>
        {columns.map(column => <td>{column.content}</td>)}
      </tr>
