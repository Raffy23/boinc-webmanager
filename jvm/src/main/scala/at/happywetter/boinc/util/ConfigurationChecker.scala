package at.happywetter.boinc.util

import java.io.File

import at.happywetter.boinc.AppConfig.Config

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
object ConfigurationChecker extends Logger {

  def checkConfiguration(config: Config): Unit = {
    if (config.development.getOrElse(false)) {
      LOG.info("WebServer was launched with development options!")
      LOG.info("All resources will be served from: " + config.server.webroot)
    }

    // Check environment
    if (!new File("./application.conf").exists()) {
      System.err.println("Unable to read ./application.conf!")
      System.exit(1)
    }

    // Check if XML-Source is available
    if (!new File(config.boinc.projects.xmlSource).exists()) {
      System.err.println("Unable to read Boinc XML Project definition!")
      System.exit(1)
    }

    if (config.development.getOrElse(false) && !new File(config.server.webroot).exists()) {
      System.err.println("Webroot is not a valid Directory!")
      System.exit(1)
    }

    if (config.hardware.isDefined && config.hardware.get.enabled
      && !new File(config.hardware.get.binary).exists()) {
      System.err.println("Binary for Hardware Data fetching is missing!")
      System.exit(1)
    }
  }

}
