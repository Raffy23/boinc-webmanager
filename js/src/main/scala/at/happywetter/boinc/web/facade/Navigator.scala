package at.happywetter.boinc.web.facade

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Created by: 
  *
  * @author Raphael
  * @version 01.11.2017
  */

@js.native
@JSGlobal("navigator")
object Navigator extends js.Object:

  val userAgent: String = js.native
