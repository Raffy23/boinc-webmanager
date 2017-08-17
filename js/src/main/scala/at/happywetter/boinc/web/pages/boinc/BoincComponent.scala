package at.happywetter.boinc.web.pages.boinc

import at.happywetter.boinc.web.boincclient.BoincClient
import org.scalajs.dom

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.08.2017
  */
trait BoincComponent {

  def onRender(client: BoincClient)

}
