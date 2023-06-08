package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.boincrpc.Workunit

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object WorkunitParser:

  def fromXML(node: NodeSeq): Workunit =
    Workunit(
      (node \ "name").text,
      (node \ "app_name").text,
      (node \ "rsc_fpops_est").text.toDouble,
      (node \ "rsc_fpops_bound").text.toDouble,
      (node \ "rsc_memory_bound").text.toDouble,
      (node \ "rsc_disk_bound").text.toDouble
      // TODO: command_line, file_ref, version_num
    )
