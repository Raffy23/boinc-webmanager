package at.happywetter.boinc.web.routes

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSImport}

/**
  * For Navigo Version 5.3.1
  *
  * Created by:
  * @author Raphael
  * @version 24.07.2017
  */

@js.native
@JSGlobal//@JSImport("navigo", JSImport.Namespace)
object Navigo extends js.Object {

  val PARAMETER_REGEXP: String = js.native
  val WILDCARD_REGEXP: String = js.native
  val REPLACE_VARIABLE_REGEXP: String = js.native
  val REPLACE_WILDCARD: String = js.native
  val FOLLOWED_BY_SLASH_REGEXP: String = js.native
  var MATCH_REGEXP_FLAGS: String = js.native

}

@js.native
@JSGlobal//@JSImport("navigo", JSImport.Namespace)
class Navigo(root: String = null, useHash: Boolean = false, hash: String = "#") extends js.Object {

  //Doesn't work but is in Docs
  //def on(genericHook: GenericHook): Navigo = js.native

  //Is in docs, but not tested
  //def on(routes: Map[String, RouteCallback]): Navigo = js.native

  def on(route: String, callback: js.Function0[Unit]): Navigo = js.native
  def on(route: String, callback: js.Function1[js.Dictionary[String], Unit]): Navigo = js.native
  def on(route: String, callback: js.Function2[js.Dictionary[String], String, Unit]): Navigo = js.native
  def on(route: String, callback: js.Function0[Unit], hook: Hook): Navigo = js.native
  def on(route: String, callback: js.Function1[js.Dictionary[String], Unit], hook: Hook): Navigo = js.native
  def on(route: String, callback: js.Function2[js.Dictionary[String], String, Unit], hook: Hook): Navigo = js.native

  def on(rootCallback: js.Function0[Unit]): Navigo = js.native

  def notFound(callback: js.Function1[String, Unit]): Unit = js.native

  def navigate(route: String): Navigo = js.native

  def navigate(route: String, absolute: Boolean): Navigo = js.native

  def pause(boolean: Boolean = true): Unit = js.native

  def resume(): Unit = js.native

  def disableIfAPINotAvailable(): Unit = js.native

  def lastRouteResolved(): Unit = js.native

  def destroy(): Unit = js.native

  def resolve(currentURL: js.UndefOr[String]): Unit = js.native

  def link(path: String): String = js.native

  def updatePageLinks(): Unit = js.native

  def resolve(): Boolean = js.native

}

abstract class Hook extends js.Object {

  def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit

  def after(params: js.Dictionary[String]): Unit

  def leave(params: js.Dictionary[String]): Unit

  def already(params: js.Dictionary[String]): Unit

}

abstract class GenericHook extends js.Object {

  def before(done: js.Function0[Unit], params: String): Unit

  def after(params: String): Unit

}

@js.native
trait ResolvedRoute extends js.Object {

  val url: String = js.native

  val query: String = js.native

}