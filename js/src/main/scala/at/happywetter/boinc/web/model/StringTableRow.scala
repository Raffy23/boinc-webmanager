package at.happywetter.boinc.web.model

import at.happywetter.boinc.web.pages.component
import at.happywetter.boinc.web.pages.component.DataTable

import mhtml.Var

/**
 * Created by 
 *
 * @author Raphael Ludwig
 * @version 09.02.20
 */
class StringTableRow(entries: List[String]) extends DataTable.TableRow:

  override val columns: List[DataTable.TableColumn] =
    entries.map(s => new component.DataTable.StringColumn(Var(s)))
