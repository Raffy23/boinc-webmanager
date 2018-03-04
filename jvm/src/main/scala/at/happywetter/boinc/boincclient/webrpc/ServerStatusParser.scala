package at.happywetter.boinc.boincclient.webrpc

import at.happywetter.boinc.shared.webrpc.{Daemon, DatabaseFileStates, ServerStatus, ServerStatusApp}

import scala.language.implicitConversions
import scala.xml.NodeSeq
import at.happywetter.boinc.boincclient.parser._
/**
  * Created by: 
  *
  * @author Raphael
  * @version 04.03.2018
  */
object ServerStatusParser {

  def fromXML(node: NodeSeq) = ServerStatus(
      (node \ "daemon_status" \ "daemon").map(d => Daemon(d \ "host", d \ "command", d \ "status")),
      DatabaseFileStates(
        node \ "database_file_states" \ "results_ready_to_send",
        node \ "database_file_states" \ "results_in_progress",
        node \ "database_file_states" \ "workunits_waiting_for_validation",
        node \ "database_file_states" \ "workunits_waiting_for_assimilation",
        node \ "database_file_states" \ "workunits_waiting_for_deletion",
        node \ "database_file_states" \ "results_waiting_for_deletion",
        node \ "database_file_states" \ "transitioner_backlog_hours",
        node \ "database_file_states" \ "users_with_recent_credit",
        node \ "database_file_states" \ "users_with_credit",
        node \ "database_file_states" \ "users_registered_in_past_24_hours",
        node \ "database_file_states" \ "hosts_with_recent_credit",
        node \ "database_file_states" \ "hosts_with_credit",
        node \ "database_file_states" \ "hosts_registered_in_past_24_hours",
        node \ "database_file_states" \ "current_floating_point_speed"
      ),
      (node \ "database_file_states" \ "tasks_by_app" \ "app").map(a => ServerStatusApp(
        a \ "id", a \ "name", a \ "unsent", a \ "in_progress", a \ "avg_runtime", a \ "min_runtime",
        a \ "max_runtime", a \ "users"
      ))
    )

  private implicit def convertNodeSeqToString(node: NodeSeq): String = node.text
  private implicit def convertNodeSeqToInteger(node: NodeSeq): Int = node.tryToInt
  private implicit def convertNodeSeqToDouble(node: NodeSeq): Double = node.tryToDouble

}
