package at.happywetter.boinc.web.css

import at.happywetter.boinc.web.css.components._
import at.happywetter.boinc.web.css.pages._

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object CSSRenderer extends App {

  def render(): String = {
    import AppCSS.CSSDefaults._

    Seq(
      GlobalPageStyle,
      PageLayoutStyle,
      FloatingMenu,
      TableTheme,
      TopNavigation,
      Tooltip,
      Button,
      Misc,
      Dialog,
      BasicModalCSS,
      DropdownMenuCSS,
      ContextMenuCSS,
      LoginPageCSS,
      BoincClientCSS,
      BoincGlobalPrefsCSS,
      BoincMainHostCSS,
      BoincMessageCSS,
      BoincSwarmPageCSS,
      BoincProjectCSS,
      BoincStatisticsCSS,
      ProjectSwarmPageCSS,
      DashboardMenuCSS
    ).map(_.render[String])
     .foldLeft(new StringBuilder)(_ append _)
     .toString
  }

}
