package at.happywetter.boinc.boincclient

import scala.xml.{Node, NodeSeq}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.09.2017
  */
package object parser:

  implicit class NodeSeqParserHelper(val node: NodeSeq) extends AnyVal:

    def toScalaBoolean: Boolean = if node.text.nonEmpty then node.text.toInt == 1 else false
    def toScalaDouble: Double = if node.text.nonEmpty then node.text.toDouble else 0d

    def existsNode: Boolean = node != null

    def optionalText: Option[String] =
      if node == null || node.text == null || node.text.isEmpty then Option.empty
      else Some(node.text)

    def toOptionDouble: Option[Double] =
      if node == null || node.text == null || node.text.isEmpty then Option.empty
      else Some(node.text.toDouble)

    def tryToDouble: Double =
      if node == null || node.text == null || node.text.isEmpty then 0d
      else node.text.toDouble

    def tryToInt: Int =
      if node == null || node.text == null || node.text.isEmpty then 0
      else node.text.toInt

    def tryToLong: Long =
      if node == null || node.text == null || node.text.isEmpty then 0L
      else java.lang.Long.parseLong(node.text)

    def toIntOption: Option[Int] =
      if node == null || node.text == null || node.text.isEmpty then Option.empty
      else Some(node.text.toInt)

    def toOption: Option[NodeSeq] =
      if node == null then Option.empty else Some(node)
