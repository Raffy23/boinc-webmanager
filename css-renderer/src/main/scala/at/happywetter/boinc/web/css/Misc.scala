package at.happywetter.boinc.web.css

import scalacss.internal.mutable.StyleSheet
import AppCSS.CSSDefaults._
import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object Misc extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.Misc._
  import dsl._

  centeredText.cssName - (
    textAlign.center.important
  )
