package at.happywetter.boinc.boincclient

import at.happywetter.boinc.boincclient.webrpc.ServerStatusParser
import at.happywetter.boinc.shared.webrpc.*
import cats.effect.IO
import org.http4s.{Headers, Request, Uri}
import org.http4s.ember.client.EmberClientBuilder
//import scalaj.http.{Http, HttpOptions, HttpRequest}

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
object WebRPC:
  // Official WebRPC Documentation:
  //    https://boinc.berkeley.edu/trac/wiki/WebRpc

  // TODO: Make matchable classes
  object ErrorCodes:
    val Generic: Int = -1
    val InvalidXML: Int = -112
    val ItemNotFoundInDB: Int = -136
    val NameNotUnique: Int = -137
    val DatabaseError: Int = -138 // Same as ProjectDown error
    val ItemNotFound: Int = -161 // Smae as Item not Found in Database
    val ProjectDown: Int = -183
    val InvalidEmail: Int = -205
    val WrongPassword: Int = -207
    val EmailNotUnique: Int = -207 // Same as -137
    val AccountCreationDisabled: Int = -208
    val AttachedFailed: Int = -209

  // TODO:
  // def getProjectConfig(url: String): Future[WebRPCProjectConfig] = Future {
  //  Http(url+"/get_project_config.php").option(HttpOptions.followRedirects(true)).asString.body.toProjectConfig
  // }

  def getServerStatus(url: String)( /*implicit config: AppConfig.WebRPC*/ ): IO[ServerStatus] =
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        client
          .expect[String](
            Request(
              uri = Uri.unsafeFromString(s"$url/server_status.php?xml=1"),
              headers = Headers(
                ("Accept", "application/xml")
              )
            )
          )
          .map { content =>
            ServerStatusParser.fromXML(
              XML.loadString(content)
            )
          }
      }

  def lookupAccount(url: String, email: String, password: Option[String] = None): IO[(Boolean, Option[String])] =
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        client
          .expect[String](
            Request(
              uri = Uri
                .unsafeFromString(s"$url/lookup_account.php")
                .withQueryParam("email_addr", email)
                .withQueryParam("passwd_hash",
                                BoincCryptoHelper.md5(password.get + email.toLowerCase())
                ), // TODO: only if password.isDefined !
              headers = Headers(
                ("Accept", "application/xml")
              )
            )
          )
          .map(XML.loadString)
          .map { response =>
            // TODO: Give a Error Code to UI
            ((response \ "success").xml_==(<success/>), (response \ "authenticator").headOption.map(a => a.text))
          }

      }
      .handleError: (_: Throwable) =>
        (false, Some("err_unable_to_read_webrpc_response"))

  /*
  private implicit class XMLHttpResponse(httpRequest: HttpRequest) {
    def asXML: NodeSeq = XML.loadString(httpRequest.asString.body)
  }
   */
