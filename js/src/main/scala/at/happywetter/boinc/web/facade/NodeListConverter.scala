package at.happywetter.boinc.web.facade

import org.scalajs.dom.{Node, NodeList}
import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.07.2017
  */

object NodeListConverter:

  given convNodeList[A <: Node]: Conversion[NodeList[A], MDNNodeList[A]] with
    def apply(nodeList: NodeList[A]): MDNNodeList[A] = nodeList.asInstanceOf[MDNNodeList[A]]

@JSGlobal
@js.native
class MDNNodeList[+A <: Node] extends NodeList[A]:

  def values(): Iterator[A] = js.native

  def forEach(func: js.Function3[Node, Int, NodeList[A], Unit]): Unit = js.native
