package at.happywetter.boinc.web.model

import at.happywetter.boinc.web.pages.component.DataTable.TableRow

/**
 * Created by: 
 *
 * @author Raphael
 * @version 10.07.2020
 */
trait TableModel[A, T <: TableRow]:

  val header: List[(String, Boolean)]

  def convert(data: List[A]): List[T]
