package at.happywetter.boinc.web.model

import at.happywetter.boinc.shared.boincrpc.{Message, Project, Result}
import at.happywetter.boinc.shared.rpc.HostDetails
import at.happywetter.boinc.web.boincclient.BoincClient
import at.happywetter.boinc.web.extensions.HardwareStatusClient
import at.happywetter.boinc.web.model.SensorHardwareTableModel
import at.happywetter.boinc.web.model.SensorHardwareTableModel.SensorHardwareTableRow
import at.happywetter.boinc.web.pages.component.DataTable

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.09.2017
  */
object DataModelConverter:
  import scala.language.implicitConversions

  implicit def resultConverter(data: List[Result])(implicit boinc: BoincClient): List[WuDataTableModel.WuTableRow] =
    data.map(WuDataTableModel.convert)

  implicit def projectConverter(data: List[Project])(implicit
    boinc: BoincClient,
    table: DataTable[ProjectDataTableModel.ProjectTableRow]
  ): List[ProjectDataTableModel.ProjectTableRow] =
    data.map(ProjectDataTableModel.convert)

  implicit def messagesConvert(data: List[Message]): List[MessageTableModel.MessageTableRow] =
    data.map(MessageTableModel.convert)
