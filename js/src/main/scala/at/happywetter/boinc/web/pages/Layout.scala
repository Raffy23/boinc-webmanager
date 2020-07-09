package at.happywetter.boinc.web.pages

import at.happywetter.boinc.web.boincclient.ClientManager
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.util.{AuthClient, DashboardMenuBuilder, ErrorDialogUtil}

import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
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
    AuthClient.validateAction(authenticatedHook(done))
  }

  def after(): Unit = {}
  def leave(): Unit = {}
  def already(): Unit = {}

  def onRender(): Unit = {}
  def beforeRender(params: js.Dictionary[String]): Unit

  private def authenticatedHook(done: js.Function0[Unit]): () => Unit = () => {
    PageLayout.clearNav()
    PageLayout.showMenu()

    ClientManager
      .readClients()
      .map(DashboardMenuBuilder.renderClients)
      .recover(ErrorDialogUtil.showDialog)

    done()
  }

}