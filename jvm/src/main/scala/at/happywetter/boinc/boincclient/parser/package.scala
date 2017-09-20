package at.happywetter.boinc.boincclient

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.09.2017
  */
package object parser {

  implicit class NodeSeqParserHelper(node: NodeSeq) {

    def toScalaBoolean: Boolean = if (node.text.nonEmpty) node.text.toInt==1 else false
    def toScalaDouble: Double = if(node.text.nonEmpty) node.text.toDouble else 0D

    def tryToDouble: Double =
      if (node == null || node.text == null || node.text.isEmpty) 0D
      else node.text.toDouble

    def tryToInt: Int =
      if (node == null || node.text == null || node.text.isEmpty) 0
      else node.text.toInt

  }


}
