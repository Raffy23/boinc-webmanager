package at.happywetter.boinc.web.pages

import at.happywetter.boinc.web.routes.NProgress

import scala.scalajs.js
import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.07.2017
  */
trait Layout {

  val path: String

  def link: String = "/view/" + path

  def render: Elem

  def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit = {
    NProgress.start()
    done()
  }

  def after(): Unit = {}
  def leave(): Unit = {}
  def already(): Unit = {}

  def onRender(): Unit = {}
  def beforeRender(params: js.Dictionary[String]): Unit

}