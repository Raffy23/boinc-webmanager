package at.happywetter.boinc.web.css

import at.happywetter.boinc.web.pages.boinc.{BoincMainHostLayout, BoincStatisticsLayout}
import at.happywetter.boinc.web.pages.component._
import at.happywetter.boinc.web.pages.component.dialog.BasicModalDialog
import at.happywetter.boinc.web.pages.{BoincClientLayout, LoginPage, PageLayout}

import scalacss.internal.mutable.GlobalRegistry

/**
  * Created by: 
  *
  * @author Raphael
  * @version 23.07.2017
  */
object AppCSS {
  import scalacss.ProdDefaults._

  def load(): Unit = {
    GlobalRegistry.register(
      GlobalPageStyle,
      LoginPage.Style,
      PageLayout.Style,
      DashboardMenu.Style,
      TableTheme,
      BoincClientLayout.Style,
      TopNavigation,
      ContextMenu.Style,
      Tooltip.Style,
      BasicModalDialog.Style,
      BoincMainHostLayout.Style,
      DropdownMenu.Style,
      BoincStatisticsLayout.Style
    )

    GlobalRegistry.addToDocumentOnRegistration()
  }

}
