package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.CCState

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object CCStateParser {

  def fromXML(node: NodeSeq) = CCState(
      (node \ "network_status").text.toInt,
      (node \ "ams_password_error").text.toInt,
      (node \ "task_suspend_reason").text.toInt,
      (node \ "task_mode").text.toInt,
      (node \ "task_mode_perm").text.toInt,
      (node \ "task_mode_delay").text.toDouble,
      (node \ "gpu_suspend_reason").text.toInt,
      (node \ "gpu_mode").text.toInt,
      (node \ "gpu_mode_perm").text.toInt,
      (node \ "gpu_mode_delay").text.toDouble,
      (node \ "network_suspend_reason").text.toInt,
      (node \ "network_mode").text.toInt,
      (node \ "network_mode_perm").text.toInt,
      (node \ "network_mode_delay").text.toDouble,
      (node \ "disallow_attach").text.toInt == 1,
      (node \ "simple_gui_only").text.toInt == 1,
      (node \ "max_event_log_lines").tryToInt
    )

}
