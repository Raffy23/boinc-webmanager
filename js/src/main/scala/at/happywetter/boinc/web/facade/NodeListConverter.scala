package at.happywetter.boinc.web.facade

import org.scalajs.dom.{Node, NodeList}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.07.2017
  */
object NodeListConverter {

  import scala.language.implicitConversions
  implicit def convNodeList(nodeList: NodeList): MDNNodeList = nodeList.asInstanceOf[MDNNodeList]

}

@JSGlobal
@js.native
class MDNNodeList extends NodeList {

  def values(): Iterator[Node] = js.native

  def forEach(func: js.Function3[Node, Int, NodeList, Unit]): Unit = js.native

}
