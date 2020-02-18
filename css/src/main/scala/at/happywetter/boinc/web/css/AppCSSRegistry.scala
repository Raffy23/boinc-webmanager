package at.happywetter.boinc.web.css

import at.happywetter.boinc.web.css.definitions._
import at.happywetter.boinc.web.css.definitions.components.{BasicModalStyle, ContextMenuStyle, Dialog, DropDownMenuStyle, FloatingMenu, PageLayoutStyle, TableTheme, Tooltip, TopNavigation}
import at.happywetter.boinc.web.css.definitions.pages._

/**
 * Created by: 
 *
 * @author Raphael
 * @version 03.02.2020
 */
object AppCSSRegistry {

  def registerCSSNames(): Int = {
    GlobalRegistry.registerDefinitions(
      FloatingMenu,
      PageLayoutStyle,
      TableTheme,
      TopNavigation,
      Misc,
      Tooltip,
      Dialog,
      BasicModalStyle,
      DropDownMenuStyle,
      ContextMenuStyle,
      LoginPageStyle,
      BoincClientStyle,
      BoincGlobalPrefsStyle,
      BoincMainHostStyle,
      BoincMessageStyle,
      BoincSwarmPageStyle,
      BoincProjectStyle,
      BoincStatisticsStyle,
      ProjectSwarmPageStyle,
      DashboardMenuStyle
    )(GlobalRegistry.DevMode)
  }

}
