package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.DiskUsage

import scala.collection.mutable
import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object DiskUsageParser {
  def fromXML(node: NodeSeq): DiskUsage = {
    val data: mutable.HashMap[String,Double] = new mutable.HashMap[String,Double]

    (node \ "project") foreach {
      case(child) => data += ((child \ "master_url").text -> (child \ "disk_usage").text.toDouble)
    }

    DiskUsage(data.toMap
      ,(node \ "d_total").text.toDouble
      ,(node \ "d_free").text.toDouble
      ,(node \ "d_boinc").text.toDouble
      ,(node \ "d_allowed").text.toDouble)
  }
}
