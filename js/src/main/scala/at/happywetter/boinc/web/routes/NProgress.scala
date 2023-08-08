package at.happywetter.boinc.web.routes

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("nprogress/nprogress.css", JSImport.Namespace)
object NProgressCSS extends js.Object

/**
  * Created by:
  *
  * @author Raphael
  * @version 24.07.2017
  */
@js.native
@JSImport("nprogress/nprogress.js", JSImport.Namespace)
object NProgress extends js.Object {

  def start(): Unit = js.native

  // def stop(): Unit = js.native

  def remove(): Unit = js.native

  def set(value: Double): Unit = js.native

  def inc(value: Double): Unit = js.native

  def done(force: Boolean): Unit = js.native

  def configure(c: js.Dynamic): Unit = js.native

  val status: Double = js.native
}
