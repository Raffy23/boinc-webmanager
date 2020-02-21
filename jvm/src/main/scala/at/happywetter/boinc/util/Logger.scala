package at.happywetter.boinc.util

import org.slf4j
import org.slf4j.LoggerFactory

/**
  * Created by: 
  *
  * @author Raphael
  * @version 06.07.2019
  */
trait Logger {

  protected val LOG: slf4j.Logger = LoggerFactory.getILoggerFactory.getLogger(this.getClass.getCanonicalName)
  
}
