package at.happywetter.boinc.web.util

import scala.xml.{Node, Text}

/**
  * Created by:
  *
  * @author Raphael
  * @version 06.02.2018
  */
object XMLHelper:

  import scala.language.implicitConversions

  implicit def toXMLTextNode(str: String): Node = Text(str)

  implicit class RichTextXMLNode(private val str: String) extends AnyVal:
    def toXML: Node = Text(str)
