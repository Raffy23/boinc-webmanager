package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.boincrpc.Task

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object TaskParser:

  def fromXML(node: NodeSeq): Task =
    Task(
      (node \ "active_task_state").text.toInt,
      (node \ "app_version_num").text.toInt,
      (node \ "slot").text.toInt,
      (node \ "pid").text.toInt,
      (node \ "scheduler_state").text.toInt,
      (node \ "checkpoint_cpu_time").text.toDouble,
      (node \ "fraction_done").text.toDouble,
      (node \ "current_cpu_time").text.toDouble,
      (node \ "elapsed_time").text.toDouble,
      (node \ "swap_size").text.toDouble,
      (node \ "working_set_size").text.toDouble,
      if (node \ "progress_rate").isEmpty then 0.0d else (node \ "progress_rate").text.toDouble
    )
