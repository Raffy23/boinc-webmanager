package at.happywetter.boinc.web.util

import at.happywetter.boinc.web.facade.Navigator

/**
  * Created by:
  *
  * @author Raphael
  * @version 01.11.2017
  */
object CompatibilityTester:

  def isFirefox: Boolean = Navigator.userAgent.toLowerCase().indexOf("firefox") > -1
