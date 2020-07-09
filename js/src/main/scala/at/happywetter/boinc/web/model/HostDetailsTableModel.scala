package at.happywetter.boinc.web.model

import at.happywetter.boinc.shared.rpc.HostDetails
import at.happywetter.boinc.web.pages.component.DataTable
import at.happywetter.boinc.web.pages.component.DataTable.StringColumn
import mhtml.Var

/**
 * Created by: 
 *
 * @author Raphael
 * @version 09.07.2020
 */
object HostDetailsTableModel {

  class HostDetailsTableRow(details: HostDetails) extends DataTable.TableRow {
    override val columns: List[DataTable.TableColumn] = List(
      new StringColumn(Var(details.name)),
      new StringColumn(Var(details.address)),
      new StringColumn(Var(details.port.toString)),
      new StringColumn(Var(details.addedBy)),
      new StringColumn(Var("CHANGE_ENTRY_BTN"))
    )
  }

  def convert(data: List[HostDetails]): List[HostDetailsTableRow] =
    data.map(new HostDetailsTableRow(_))

}
