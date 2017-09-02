package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.{DailyStatistic, Statistics}

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.09.2017
  */
object ProjectStatsParser {

  def fromXML(node: NodeSeq) =
    Statistics(
      (node \ "statistics" \ "project_statistics").theSeq.map(projectStatNode => {
        ((projectStatNode \ "master_url").text, (projectStatNode \ "daily_statistics").theSeq.map(parseDailyEntry).toList)
      }).toMap
    )

  private def parseDailyEntry(node: NodeSeq) = DailyStatistic(
    (node \ "day").text.toDouble,
    (node \ "user_total_credit").text.toDouble,
    (node \ "user_expavg_credit").text.toDouble,
    (node \ "host_total_credit").text.toDouble,
    (node \ "host_expavg_credit").text.toDouble,
  )
}
