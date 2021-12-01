package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.boincrpc.BoincVersion

import scala.xml.NodeSeq

/**
 * Created by: 
 *
 * @author Raphael
 * @version 13.07.2020
 */
object VersionParser {

  def fromXML(node: NodeSeq): BoincVersion =
    BoincVersion(
      (node \ "major").text.toInt,
      (node \ "minor").text.toInt,
      (node \ "release").text.toInt,
    )

}
