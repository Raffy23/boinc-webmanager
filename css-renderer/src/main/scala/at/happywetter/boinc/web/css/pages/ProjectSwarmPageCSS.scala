package at.happywetter.boinc.web.css.pages

import at.happywetter.boinc.web.css.AppCSS.CSSDefaults._

import scala.language.postfixOps

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object ProjectSwarmPageCSS extends StyleSheet.Standalone {
  import at.happywetter.boinc.web.css.definitions.pages.ProjectSwarmPageStyle._
  import dsl._
  
  topNavigationAtion.cssName -(
    color(c"#333"),
    textDecoration := "none",
    fontSize(28 px),
    paddingLeft(8 px)
  )

  lastRowSmall.cssName -(
    width(1.5 em)
  )

  floatingMenu.cssName -(
    position.absolute,
    top(80 px),
    right(20 px),
  )
  
}
