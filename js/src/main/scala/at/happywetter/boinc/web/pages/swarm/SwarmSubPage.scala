package at.happywetter.boinc.web.pages.swarm

import org.scalajs.dom.raw.HTMLElement

import scalatags.JsDom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 13.09.2017
  */
trait SwarmSubPage {

  def header: String

  def render: JsDom.TypedTag[HTMLElement]

}
