package at.happywetter.boinc.web.css

import scala.language.postfixOps

import AppCSS.CSSDefaults._
import scalacss.internal.mutable.StyleSheet

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
