package at.happywetter.boinc.web.facade

import org.scalajs.dom.raw

import scala.scalajs.js
import scala.scalajs.js.{JSNumberOps, |}
import scala.scalajs.js.annotation.JSGlobalScope

/**
  * Created by: 
  *
  * @author Raphael
  * @version 09.07.2019
  */
object Implicits {

  @js.native
  @JSGlobalScope
  object UriEncoderProvider extends js.Object {
    def encodeURI(str: String): String = js.native
    def encodeURIComponent(str: String): String = js.native

    def decodeURI(str: String): String = js.native
    def decodeURIComponent(str: String): String = js.native
  }

  implicit class RichWindow(val window: raw.Window) extends AnyVal {
    @inline def encodeURI(str: String): String = UriEncoderProvider.encodeURI(str)
    @inline def encodeURIComponent(str: String): String = UriEncoderProvider.encodeURIComponent(str)

    @inline def decodeURI(str: String): String = UriEncoderProvider.decodeURI(str)
    @inline def decodeURIComponent(str: String): String = UriEncoderProvider.decodeURIComponent(str)
  }

  @js.native
  trait JSNumberOps extends js.Any {

    def toLocaleString(): String = js.native

    def toLocaleString(locales: Array[String]): String = js.native

    def toLocaleString(locales: js.UndefOr[Array[String]], options: js.Dictionary[Any]): String = js.native

  }

}
