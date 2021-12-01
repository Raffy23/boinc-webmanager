package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.boincclient.parser.BoincParserUtils._
import at.happywetter.boinc.shared.boincrpc.Result

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object ResultParser {

  def fromXML(node: NodeSeq): Result = {
    Result((node \ "name").text,
      (node \ "wu_name").text,
      (node \ "platform").text,
      (node \ "version_num").text,
      (node \ "plan_class").text,
      (node \ "project_url").text,
      (node \ "state").text.toInt,
      (node \ "suspended_via_gui").xml_==(<suspended_via_gui/>),
      if((node \ "active_task" ).text.nonEmpty) Some((node \ "active_task").toTask)
      else None,
      (node \ "estimated_cpu_time_remaining").text.toDouble,
      (node \ "report_deadline").text.toDouble,
      (node \ "final_cpu_time").tryToDouble,
      (node \ "final_elapsed_time").tryToDouble,
      (node \ "exit_status").tryToInt,
      (node \ "resources").optionalText
    )
  }

}