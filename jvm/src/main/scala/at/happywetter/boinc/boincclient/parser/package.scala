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

    def existsNode: Boolean = node != null

    def optionalText: Option[String] =
      if (node == null || node.text == null) None
      else Some(node.text)

    def toOptionDouble: Option[Double] =
      if (node == null || node.text == null || node.text.isEmpty) None
      else Some(node.text.toDouble)

    def tryToDouble: Double =
      if (node == null || node.text == null || node.text.isEmpty) 0D
      else node.text.toDouble

    def tryToInt: Int =
      if (node == null || node.text == null || node.text.isEmpty) 0
      else node.text.toInt

    def tryToLong: Long =
      if (node == null || node.text == null || node.text.isEmpty) 0L
      else java.lang.Long.parseLong(node.text)

  }


}
