package at.happywetter.boinc.web.css.components

import scala.language.postfixOps

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object Dialog extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.components.Dialog._

  import dsl._

  header.cssName - (
    marginTop(15 px)
  )
