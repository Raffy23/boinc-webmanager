package at.happywetter.boinc.web.pages.component

import at.happywetter.boinc.web.pages.BoincClientLayout

import scala.scalajs.js
import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.08.2017
  */
abstract class BoincPageLayout(_params: js.Dictionary[String])
  extends BoincClientLayout(clientName = _params.get("client").get) {

  override def render: Elem = {<span>__BOINC_CLIENT_LAYOUT__</span>}
}
