package at.happywetter.boinc.web.helper

import scala.xml.{Node, Text}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 06.02.2018
  */
object XMLHelper {

  implicit def toXMLTextNode(str: String): Node = Text(str)

}
