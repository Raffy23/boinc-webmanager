package at.happywetter.boinc.web.helper

import org.scalajs.dom.experimental.Headers

/**
  * Created by: 
  *
  * @author Raphael
  * @version 23.07.2017
  */
object FetchHelper {

  val header = new Headers()
  header.append("Content-Type", "application/json")

  def setToken(token: String): Unit = {
    header.set("X-Authorization", token)
  }

}
