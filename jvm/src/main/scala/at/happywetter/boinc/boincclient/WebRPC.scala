package at.happywetter.boinc.boincclient

import at.happywetter.boinc.boincclient.webrpc.ServerStatusParser
import at.happywetter.boinc.shared.webrpc._
import cats.effect.IO
import scalaj.http.{Http, HttpOptions, HttpRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scala.xml.{NodeSeq, XML}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 26.08.2017
  */
object WebRPC {
  //Official WebRPC Documentation:
  //    https://boinc.berkeley.edu/trac/wiki/WebRpc

  //TODO: Make matchable classes
  object ErrorCodes {
    val Generic: Int                 = -1
    val InvalidXML: Int              = -112
    val ItemNotFoundInDB: Int        = -136
    val NameNotUnique: Int           = -137
    val DatabaseError: Int           = -138 // Same as ProjectDown error
    val ItemNotFound: Int            = -161 // Smae as Item not Found in Database
    val ProjectDown: Int             = -183
    val InvalidEmail: Int            = -205
    val WrongPassword: Int           = -207
    val EmailNotUnique: Int          = -207 // Same as -137
    val AccountCreationDisabled: Int = -208
    val AttachedFailed: Int          = -209
  }

  //TODO:
  //def getProjectConfig(url: String): Future[WebRPCProjectConfig] = Future {
  //  Http(url+"/get_project_config.php").option(HttpOptions.followRedirects(true)).asString.body.toProjectConfig
  //}

  //TODO: Use blocker for http fetching stuff ...
  //TODO:
  def getServerStatus(url: String)(/*implicit config: AppConfig.WebRPC*/): IO[ServerStatus] = IO {
    val request =
      Http(s"$url/server_status.php?xml=1")
        .header("Accept", "application/xml")
        .timeout(connTimeoutMs = 30000, readTimeoutMs = 60000)
        .option(HttpOptions.followRedirects(true))

    //config.rules.getOrElse(url, WebRPCRule(config.parser.default)).serverStatus match {
    //  case ProjectRules.UseXMLParserForEmbeddedXML => ServerStatusParser.fromHtmlXML(request.asXML)
    //  case ProjectRules.UseXMLParser               => ServerStatusParser.fromXML(request.asXML)
    //  case ProjectRules.UseHtmlParser              => null
    //}

    ServerStatusParser.fromXML(request.asXML)
  }

  def lookupAccount(url: String, email: String, password: Option[String] = None): IO[(Boolean, Option[String])] = IO.async { callback =>
    var request = Http(s"$url/lookup_account.php")
      .param("email_addr", email)
      .option(HttpOptions.followRedirects(true))

    if (password.isDefined)
      request = request.param("passwd_hash", BoincCryptoHelper.md5(password.get+email.toLowerCase()))

    //TODO: Give a Error Code to UI
    callback(
      Try {
        val response = XML.loadString(request.asString.body)
        ((response \ "success").xml_==(<success/>), (response \ "authenticator").headOption.map(a => a.text))
      }.recover{
        case _: Exception =>
          (false, Some("err_unable_to_read_webrpc_response"))
      }.toEither
    )
  }

  private implicit class XMLHttpResponse(httpRequest: HttpRequest) {
    def asXML: NodeSeq = XML.loadString(httpRequest.asString.body)
  }

}
