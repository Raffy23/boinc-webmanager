package at.happywetter.boinc.boincclient.webrpc

import at.happywetter.boinc.boincclient.parser._
import at.happywetter.boinc.shared.webrpc.{Daemon, DatabaseFileStates, ServerStatus, ServerStatusApp}

import scala.language.{implicitConversions, postfixOps}
import scala.xml.{Node, NodeSeq}
/**
  * Created by: 
  *
  * @author Raphael
  * @version 04.03.2018
  */
object ServerStatusParser {

  def fromHtmlXML(node: NodeSeq): ServerStatus = {
    println(node)
    ServerStatusParser.fromXML(node \ "html" \ "body")
  }

  def fromXML(node: NodeSeq) = ServerStatus(
      (node \ "daemon_status" \ "daemon").map(d => Daemon(d \ "host", d \ "command", (d \ "status") or d)),
      DatabaseFileStates(
        (node \ "database_file_states" \ "results_ready_to_send") or findTotal,
        (node \ "database_file_states" \ "results_in_progress") or findTotal,
        (node \ "database_file_states" \ "workunits_waiting_for_validation") or findTotal,
        (node \ "database_file_states" \ "workunits_waiting_for_assimilation") or findTotal,
        (node \ "database_file_states" \ "workunits_waiting_for_deletion") or findTotal,
        (node \ "database_file_states" \ "results_waiting_for_deletion") or findTotal,
        (node \ "database_file_states" \ "transitioner_backlog_hours") or findTotal,
        (node \ "database_file_states" \ "users_with_recent_credit") or findTotal,
        (node \ "database_file_states" \ "users_with_credit") or findTotal,
        (node \ "database_file_states" \ "users_registered_in_past_24_hours") or findTotal,
        (node \ "database_file_states" \ "hosts_with_recent_credit") or findTotal,
        (node \ "database_file_states" \ "hosts_with_credit") or findTotal,
        (node \ "database_file_states" \ "hosts_registered_in_past_24_hours") or findTotal,
        (node \ "database_file_states" \ "current_floating_point_speed") or findTotal
      ),

      if (node \ "database_file_states" \ "tasks_by_app" exists (_.label == "tasks_by_app")) {
        (node \ "database_file_states" \ "tasks_by_app" \ "app").map(a =>
          ServerStatusApp(
            a \ "id",
            a \ "name",
            a \ "unsent",
            a \ "in_progress",
            a \ "avg_runtime",
            a \ "min_runtime",
            a \ "max_runtime",
            a \ "users"
          )
        )
      } else {
        (node \ "database_file_states" \ "results_in_progress").filter(_ \@ "app" nonEmpty).map(progress =>
          ServerStatusApp(
            (progress \@ "appid").toInt,
            progress \@ "app",
            node \ "database_file_states" \ "results_ready_to_send" find (_ \@ "appid" == progress \@ "appid") map (_.text.toInt) getOrElse 0,
            progress,
            0D,
            0D,
            0D,
            node \ "database_file_states" \ "users_with_recent_credit" find (_ \@ "appid" == progress \@ "appid") map (_.text.toInt) getOrElse 0
          )
        )
      }
    )

  private implicit class OrDSL(val node: NodeSeq) extends AnyVal {
    def or(other: NodeSeq): String = if(node.text.isEmpty) other.text else node.text
    def or(f: NodeSeq => Boolean): NodeSeq = if (node.theSeq.size > 1) node.filter(f) else node
  }
  
  private val findTotal: NodeSeq => Boolean = node => node \@ "app" == "total"

  private implicit def convertNodeSeqToString(node: NodeSeq): String = node.text
  private implicit def convertNodeSeqToInteger(node: NodeSeq): Int = node.tryToInt
  private implicit def convertNodeSeqToDouble(node: NodeSeq): Double = node.tryToDouble

}
