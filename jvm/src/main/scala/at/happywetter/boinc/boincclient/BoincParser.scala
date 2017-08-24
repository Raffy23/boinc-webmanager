package at.happywetter.boinc.boincclient

import at.happywetter.boinc.shared._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.xml.{Node, NodeSeq}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.07.2016
  */
object ResultParser {
  def fromXML(node: NodeSeq): Result = {
    Result((node \ "name").text
      ,(node \ "wu_name").text
      ,(node \ "platform").text
      ,(node \ "version_num").text
      ,(node \ "plan_class").text
      ,(node \ "project_url").text
      ,(node \ "state").text.toInt
      ,(node \ "suspended_via_gui").xml_==(<suspended_via_gui/>)
      ,if((node \ "active_task" ).text.nonEmpty) Some(TaskParser.fromXML(node \ "active_task"))
      else None
      ,(node \ "estimated_cpu_time_remaining").text.toDouble
      ,(node \ "report_deadline").text.toDouble
    )
  }
}

object TaskParser {
  private def toBool(nodeSeq: NodeSeq): Boolean = nodeSeq.text.toInt==1

  def fromXML(node: NodeSeq): Task = {
    Task((node \ "active_task_state").text.toInt
      ,(node \ "app_version_num").text.toInt
      ,(node \ "slot").text.toInt
      ,(node \ "pid").text.toInt
      ,(node \ "scheduler_state").text.toInt
      ,(node \ "checkpoint_cpu_time").text.toDouble
      ,(node \ "fraction_done").text.toDouble
      ,(node \ "current_cpu_time").text.toDouble
      ,(node \ "elapsed_time").text.toDouble
      ,(node \ "swap_size").text.toDouble
      ,(node \ "working_set_size").text.toDouble
      ,if((node \ "progress_rate").isEmpty) 0.0D else (node \ "progress_rate").text.toDouble)
  }
}

object HostInfoParser {
  private def toList(nodeSeq: NodeSeq): List[String] = nodeSeq.text.split(" ").toList
  private def getCoproc(nodeSeq: NodeSeq): List[CoProcessor] = Array[CoProcessor]().toList

  def fromXML(node: NodeSeq): HostInfo = {
    HostInfo((node \ "domain_name").text
             ,(node \ "ip_addr").text
             ,(node \ "host_cpid").text
             ,(node \ "p_ncpus").text.toInt
             ,(node \ "p_vendor").text
             ,(node \ "p_model").text
             ,toList(node \ "p_features")
             ,(node \ "p_fpops").text.toDouble
            ,(node \ "p_iops").text.toDouble
            ,(node \ "p_membw").text.toDouble
            ,(node \ "m_nbytes").text.toDouble
            ,(node \ "m_cache").text.toDouble
            ,(node \ "m_swap").text.toDouble
            ,(node \ "d_total").text.toDouble
            ,(node \ "d_free").text.toDouble
            ,(node \ "os_name").text
            ,(node \ "os_version").text
            ,getCoproc(node \ "coprocs")
            ,(node \ "virtualbox_version").toList.headOption.map(n => n.text))
  }

}

object DiskUsageParser {
  def fromXML(node: NodeSeq): DiskUsage = {
    val data: mutable.HashMap[String,Double] = new mutable.HashMap[String,Double]

    (node \ "project") foreach { case(child) => data += ((child \ "master_url").text -> (child \ "disk_usage").text.toDouble) }

    DiskUsage(data.toMap
            ,(node \ "d_total").text.toDouble
            ,(node \ "d_free").text.toDouble
            ,(node \ "d_boinc").text.toDouble
            ,(node \ "d_allowed").text.toDouble)
  }
}

object ProjectParser {

  private def getText(node: NodeSeq): String = if( node.text == null ) "<empty>" else node.text

  def fromXML(node: NodeSeq): List[Project] = ( for( p <- node \ "project" ) yield fromNodeXML(p) ).toList
  def fromNodeXML(node: NodeSeq): Project = {
    Project(getText(node \ "project_name")
      , getText(node \ "master_url")
      , getText(node \ "user_name")
      , getText(node \ "team_name")
      , getText(node \ "cross_project_id")
      , (node \ "user_total_credit").text.toDouble
      , (node \ "user_expavg_credit").text.toDouble
      , (node \ "userid").text
      , (node \ "teamid").text
      , (node \ "hostid").text
      , (node \ "host_total_credit").text.toDouble
      , (node \ "host_expavg_credit").text.toDouble
      , (node \ "dont_request_more_work").xml_==(<dont_request_more_work/>)
      , (node \ "trickle_up_pending").xml_==(<trickle_up_pending/>)
      , readGUIUrls(node \ "gui_urls")
    )
  }

  def readGUIUrls(node: NodeSeq): List[ProjectGuiURL] = (
    for( guiurl <- node \ "gui_url") yield ProjectGuiURL((guiurl \ "name").text
                                                        ,getText(guiurl \ "description")
                                                        ,(guiurl \ "url").text)
  ).toList

}

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

object WorkunitParser {

  def fromXML(node: NodeSeq): Workunit = {
    Workunit((node \ "name").text
            ,(node \ "app_name").text
            ,(node \ "rsc_fpops_est").text.toDouble
            ,(node \ "rsc_fpops_bound").text.toDouble
            ,(node \ "rsc_memory_bound").text.toDouble
            ,(node \ "rsc_disk_bound").text.toDouble)
  }

}

object FileTransferParser {


  def fromXML(node: NodeSeq): List[FileTransfer] = {
    ( for( ft <- node \ "file_transfer" ) yield fromNode(ft) ).toList
  }

  private def fromNode(node: NodeSeq): FileTransfer = {
    FileTransfer(
       (node \ "project_url").text
      ,(node \ "project_name").text
      ,(node \ "name").text
      ,(node \ "nbytes").text.toDouble
      ,(node \ "status").text.toInt
      ,fromPxferNode(node \ "persistent_file_xfer")
      ,fromXFerNode(node \ "file_xfer")
    )
  }

  private def fromPxferNode(node: NodeSeq): PersistentFileXfer = {
    PersistentFileXfer(
       (node \ "num_retries").text.toInt
      ,(node \ "first_request_time").text.toDouble
      ,(node \ "next_request_time").text.toDouble
      ,(node \ "time_so_far").text.toDouble
      ,(node \ "last_bytes_xferred").text.toDouble
      ,(node \ "is_upload").text.toInt==1
    )
  }

  private def fromXFerNode(node: NodeSeq): FileXfer = {
    if( node.isEmpty ) FileXfer(0D,0D,"")
    else FileXfer(
       (node \ "bytes_xferred").text.toDouble
      ,(node \ "xfer_speed").text.toDouble
      ,(node \ "url").text
    )
  }

}

object CCStateParser {

  def fromXML(node: NodeSeq): CCState = CCState(
      (node \ "network_status").text.toInt,
      (node \ "ams_password_error").text.toInt,
      (node \ "task_suspend_reason").text.toInt,
      (node \ "task_mode").text.toInt,
      (node \ "task_mode_perm").text.toInt,
      (node \ "task_mode_delay").text.toInt,
      (node \ "gpu_suspend_reason").text.toInt,
      (node \ "gpu_mode").text.toInt,
      (node \ "gpu_mode_perm").text.toInt,
      (node \ "gpu_mode_delay").text.toInt,
      (node \ "network_suspend_reason").text.toInt,
      (node \ "network_mode").text.toInt,
      (node \ "network_mode_perm").text.toInt,
      (node \ "network_mode_delay").text.toDouble,
      (node \ "disallow_attach").text.toInt==1,
      (node \ "simple_gui_only").text.toInt==1,
      (node \ "max_event_log_lines").text.toInt
    )

}