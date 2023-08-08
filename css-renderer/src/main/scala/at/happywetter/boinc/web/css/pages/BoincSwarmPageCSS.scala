package at.happywetter.boinc.web.css.pages

import scala.language.postfixOps

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._
import at.happywetter.boinc.web.css.components.Button

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object BoincSwarmPageCSS extends StyleSheet.Standalone:
  import at.happywetter.boinc.web.css.definitions.pages.BoincSwarmPageStyle._

  import dsl._

  checkbox.cssName - (
    marginRight(10 px)
  )

  masterCheckbox.cssName - (
    textDecoration := "none",
    color(c"#333"),
    float.left,
    marginLeft(5 px)
  )

  button.cssName - (
    Button.normal
  )
