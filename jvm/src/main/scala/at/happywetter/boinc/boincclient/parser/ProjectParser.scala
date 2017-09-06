package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.{Project, ProjectGuiURL}

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object ProjectParser {

  private def getText(node: NodeSeq): String = if( node.text == null ) "<empty>" else node.text

  def fromXML(node: NodeSeq): List[Project] = ( for( p <- node \ "project" ) yield fromNodeXML(p) ).toList
  def fromNodeXML(node: NodeSeq) = Project(
    getText(node \ "project_name"),
    getText(node \ "master_url"),
    getText(node \ "user_name"),
    getText(node \ "team_name"),
    getText(node \ "cross_project_id"),
    (node \ "user_total_credit").text.toDouble,
    (node \ "user_expavg_credit").text.toDouble,
    (node \ "userid").text,
    (node \ "teamid").text,
    (node \ "hostid").text,
    (node \ "host_total_credit").text.toDouble,
    (node \ "host_expavg_credit").text.toDouble,
    (node \ "dont_request_more_work").xml_==(<dont_request_more_work/>),
    (node \ "trickle_up_pending").xml_==(<trickle_up_pending/>),
    (node \ "resource_share").text.toDouble,
    (node \ "desired_disk_usage").text.toDouble,
    (node \ "duration_correction_factor").text.toDouble,
    (node \ "njobs_success").text.toInt,
    (node \ "njobs_error").text.toInt,
    readGUIUrls(node \ "gui_urls")
    )

  def readGUIUrls(node: NodeSeq): List[ProjectGuiURL] = (
    for (guiurl <- node \ "gui_url") yield ProjectGuiURL((guiurl \ "name").text
      , getText(guiurl \ "description")
      , (guiurl \ "url").text)
    ).toList

}
