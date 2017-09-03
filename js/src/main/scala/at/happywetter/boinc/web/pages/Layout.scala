package at.happywetter.boinc.web.pages

import at.happywetter.boinc.web.routes.Hook
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.07.2017
  */
trait Layout {

  val path: String

  def render: Option[JsDom.TypedTag[HTMLElement]] = { None }
  val staticComponent: Option[JsDom.TypedTag[HTMLElement]]

  val routerHook: Option[Hook]

  val requestedParent: Option[String] = None
  def requestParentLayout(): Option[Layout] = None

  def onRender(): Unit = {}
  def beforeRender(params: js.Dictionary[String])
}
