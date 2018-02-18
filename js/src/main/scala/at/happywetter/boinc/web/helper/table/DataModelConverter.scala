package at.happywetter.boinc.web.helper.table

import at.happywetter.boinc.shared.{Project, Result}
import at.happywetter.boinc.web.boincclient.BoincClient
import at.happywetter.boinc.web.extensions.HardwareStatusClient
import at.happywetter.boinc.web.helper.table.HardwareTableModel.HardwareTableRow

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.09.2017
  */
object DataModelConverter {
  import scala.language.implicitConversions

  implicit def resultConverter
  (data: List[Result])(implicit boinc: BoincClient): List[WuDataTableModel.WuTableRow] =
    data.map(WuDataTableModel.convert)

  implicit def projectConverter
  (data: List[Project])(implicit boinc: BoincClient): List[ProjectDataTableModel.ProjectTableRow] =
    data.map(ProjectDataTableModel.convert)

  implicit def hwClientConverter(data: List[HardwareStatusClient]): List[HardwareTableRow] =
    HardwareTableModel.convert(data)

}
