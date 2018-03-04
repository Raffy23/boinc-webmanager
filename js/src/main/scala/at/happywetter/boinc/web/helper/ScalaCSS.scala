package at.happywetter.boinc.web.helper

import scalacss.internal.StyleA
import scala.language.implicitConversions

/**
  * Created by: 
  *
  * @author Raphael
  * @version 04.03.2018
  */
object ScalaCSS {

  implicit def styleAToStringConverter(styleA: StyleA): String = styleA.htmlClass

}
