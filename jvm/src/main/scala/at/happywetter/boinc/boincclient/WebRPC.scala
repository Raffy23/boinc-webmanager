package at.happywetter.boinc.boincclient

import scala.concurrent.Future
import scala.xml.XML
import scalaj.http.{Http, HttpOptions}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.08.2017
  */
object WebRPC {


  def lookupAccount(url: String, email: String, password: Option[String] = None): Future [(Boolean, Option[String])] = Future {
    var request = Http(url+"/lookup_account.php").param("email_addr", email).option(HttpOptions.followRedirects(true))
    if (password.isDefined)
      request = request.param("passwd_hash", BoincCryptoHelper.md5(password.get+email.toLowerCase()))

    println(request)
    println(request.asString.body)
    val response = XML.loadString(request.asString.body)
    ((response \ "success").xml_==(<success/>), (response \ "authenticator").headOption.map(a => a.text))
  }

}
