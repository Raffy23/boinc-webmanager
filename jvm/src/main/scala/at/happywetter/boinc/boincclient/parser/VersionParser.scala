package at.happywetter.boinc.boincclient.parser

import scala.xml.NodeSeq

import at.happywetter.boinc.shared.boincrpc.BoincVersion

/**
 * Created by: 
 *
 * @author Raphael
 * @version 13.07.2020
 */
object VersionParser:

  def fromXML(node: NodeSeq): BoincVersion =
    BoincVersion(
      (node \ "major").text.toInt,
      (node \ "minor").text.toInt,
      (node \ "release").text.toInt
    )
