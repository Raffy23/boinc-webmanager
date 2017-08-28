package at.happywetter.boinc.web.hacks

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.typedarray.Uint8Array

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.08.2017
  */
@js.native
@JSGlobal
class TextEncoder(encoding: String) extends js.Object {

  def encode(buffer: String): Uint8Array = js.native
  def encode(buffer: String, options: Any): Uint8Array = js.native

}
