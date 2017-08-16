package at.happywetter.boinc.web.css

import at.happywetter.boinc.web.pages.boinc.BoincMainHostLayout
import at.happywetter.boinc.web.pages.component.{ContextMenu, DashboardMenu, ModalDialog, Tooltip}
import at.happywetter.boinc.web.pages.{BoincClientLayout, LoginPage, PageLayout}

import scalacss.internal.mutable.GlobalRegistry

/**
  * Created by: 
  *
  * @author Raphael
  * @version 23.07.2017
  */
object AppCSS {
  import scalacss.DevDefaults._

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
      ModalDialog.Style,
      BoincMainHostLayout.Style
    )

    GlobalRegistry.addToDocumentOnRegistration()
  }

}
