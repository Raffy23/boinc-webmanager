package at.happywetter.boinc.web.helper

import at.happywetter.boinc.shared.Result
import at.happywetter.boinc.web.boincclient.BoincClient

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.09.2017
  */
object DataModelConverter {
  import scala.language.implicitConversions

  implicit def resultConverter(data: List[Result])(implicit boinc: BoincClient): List[WuDataTableModel.WuTableRow] = data.map(WuDataTableModel.convert)

}
