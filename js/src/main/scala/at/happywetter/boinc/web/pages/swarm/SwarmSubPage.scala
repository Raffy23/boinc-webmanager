package at.happywetter.boinc.web.pages.swarm

import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 13.09.2017
  */
trait SwarmSubPage {

  def header: String

  def render: Elem

}
