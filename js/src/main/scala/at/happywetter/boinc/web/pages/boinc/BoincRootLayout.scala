package at.happywetter.boinc.web.pages.boinc
import at.happywetter.boinc.web.routes.AppRouter

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.xml.Elem

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.03.2018
  */
object BoincRootLayout extends BoincClientLayout {

  var currentController: BoincClientLayout = _

  override val path: String = ""

  override def render: Elem = {
    <div>
      <!-- __ROOT_LAYOUT__ -->
      Boinc Webmanager is loading the Boinc layout ...
    </div>
  }

  override def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit = {
    currentController.before(() => {
      AppRouter.navigate(currentController.linkForHost(params("client")))
      //done()
    }, params)
  }

  override def beforeRender(params: Dictionary[String]): Unit = {}

}
