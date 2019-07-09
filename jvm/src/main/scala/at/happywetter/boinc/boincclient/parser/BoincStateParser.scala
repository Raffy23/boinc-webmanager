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
    val boincVersion = (node \ "core_client_major_version").text + "." +
      (node \ "core_client_minor_version").text + "." +
      (node \ "core_client_release").text

    val apps: mutable.Map[String,App] = new mutable.HashMap[String,App]()
    val projects: ListBuffer[Project] = new ListBuffer[Project]
    val workunits: ListBuffer[Workunit] = new ListBuffer[Workunit]
    val results: ListBuffer[Result] = new ListBuffer[Result]

    val netStats = NetStats(
      (node \ "net_stats" \ "bwup").text.toDouble,
      (node \ "net_stats" \ "avg_up").text.toDouble,
      (node \ "net_stats" \ "avg_time_up").text.toDouble,
      (node \ "net_stats" \ "bwdown").text.toDouble,
      (node \ "net_stats" \ "avg_down").text.toDouble,
      (node \ "net_stats" \ "avg_time_down").text.toDouble
    )

    var curProject: Project = null
    val curApp: mutable.Queue[NodeSeq] = new mutable.Queue[NodeSeq]()

    for( n <- node.head.child ) {
      n.label match {
        case "project" => curProject = ProjectParser.fromNodeXML(n); projects += curProject
        case "app" => curApp.enqueue(n)
        case "app_version" =>
          if( !apps.contains( (n \ "app_name").text) ) {
            if (curApp.isEmpty) throw new RuntimeException("Unable get get app_version for " + (n \ "app_name").text)

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
        case "workunit" => workunits += WorkunitParser.fromXML(n)
        case "result" => results += ResultParser.fromXML(n)
        case _ => /* Nothing to do ... */
        //case tag => println(tag + " => " + n) //DEBUGING CODE

        // Maybe implement following tags in near future:
        // executing_as_daemon, global_preferences, time_stats ?
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
      netStats
    )
  }

}
