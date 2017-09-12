package at.happywetter.boinc.web.boincclient

import at.happywetter.boinc.shared.ApplicationError

/**
  * Created by: 
  *
  * @author Raphael
  * @version 31.08.2017
  */
case class FetchResponseException(statusCode: Int, reason: ApplicationError) extends RuntimeException
