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
    <div>__ROOT_LAYOUT__</div>
  }

  override def before(done: js.Function0[Unit], params: js.Dictionary[String]): Unit = {
    println(currentController)

    currentController.before(() => {AppRouter.navigate(currentController.link); done()}, params)
  }

  override def beforeRender(params: Dictionary[String]): Unit = {}

}
