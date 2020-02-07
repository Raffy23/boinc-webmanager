package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.boincrpc._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object BoincStateParser {
  private def toBool(nodeSeq: NodeSeq): Boolean = nodeSeq.text.toInt==1

  def fromXML(node: NodeSeq): BoincState = {
    val hostInfo = HostInfoParser.fromXML(node \ "host_info")
    val boincVersion =
      (node \ "core_client_major_version").text + "." +
      (node \ "core_client_minor_version").text + "." +
      (node \ "core_client_release").text

    val apps      = new mutable.HashMap[String,App]()
    val projects  = new ListBuffer[Project]
    val workunits = new ListBuffer[Workunit]
    val results   = new ListBuffer[Result]

    val netStats = NetStats(
      (node \ "net_stats" \ "bwup").text.toDouble,
      (node \ "net_stats" \ "avg_up").text.toDouble,
      (node \ "net_stats" \ "avg_time_up").text.toDouble,
      (node \ "net_stats" \ "bwdown").text.toDouble,
      (node \ "net_stats" \ "avg_down").text.toDouble,
      (node \ "net_stats" \ "avg_time_down").text.toDouble
    )

    val timeStats = TimeStats(
      (node \ "time_stats" \ "on_frac").toScalaDouble,
      (node \ "time_stats" \ "connected_frac").toScalaDouble,
      (node \ "time_stats" \ "cpu_and_network_available_frac").toScalaDouble,
      (node \ "time_stats" \ "active_frac").toScalaDouble,
      (node \ "time_stats" \ "gpu_active_frac").toScalaDouble,
      (node \ "time_stats" \ "client_start_time").toScalaDouble,
      (node \ "time_stats" \ "total_start_time").toScalaDouble,
      (node \ "time_stats" \ "total_duration").toScalaDouble,
      (node \ "time_stats" \ "total_active_duration").toScalaDouble,
      (node \ "time_stats" \ "total_gpu_active_duration").toScalaDouble,
      (node \ "time_stats" \ "now").toScalaDouble,
      (node \ "time_stats" \ "previous_uptime").toScalaDouble,
      (node \ "time_stats" \ "session_active_duration").toScalaDouble,
      (node \ "time_stats" \ "session_gpu_active_duration").toScalaDouble,
    )

    var curProject: Project = null
    val curApp: mutable.Queue[NodeSeq] = new mutable.Queue[NodeSeq]()

    for( n <- node.head.child ) {
      n.label match {
        case "project" =>
          curProject = ProjectParser.fromNodeXML(n);
          projects += curProject

        case "app" =>
          curApp.enqueue(n)

        case "app_version" =>
          if( !apps.contains( (n \ "app_name").text) ) {
            if (curApp.isEmpty)
              throw new RuntimeException("Unable get get app_version for " + (n \ "app_name").text)

            val app = curApp.dequeue()
            apps.put((app \ "name").text
              , App((app \ "name").text, (app \ "user_friendly_name").text, toBool(app \ "non_cpu_intensive")
                , AppVersion((n \ "version_num").text.toInt
                  , (n \ "platform").text
                  , (n \ "flops").text.toDouble
                  , (n \ "avg_ncpus").text.toDouble
                  , (n \ "max_ncpus").text.toDouble)
                , curProject.url
              )
            )
          }

        case "workunit" =>
          workunits += WorkunitParser.fromXML(n)

        case "result" =>
          results += ResultParser.fromXML(n)

        case _ => /* Nothing to do ... */
        //case tag => println(tag + " => " + n) //DEBUGING CODE

        // Maybe implement following tags in near future:
        // executing_as_daemon, global_preferences?
        // Some flags may be present: have_ati, have_nv ...
      }
    }

    BoincState(
      hostInfo,
      projects.toList,
      apps.toMap,
      workunits.toList,
      boincVersion,
      (node \  "platform_name").text,
      results.toList,
      netStats,
      timeStats
    )
  }

}
