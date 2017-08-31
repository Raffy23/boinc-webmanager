package at.happywetter.boinc.web.helper

import at.happywetter.boinc.shared.User
import at.happywetter.boinc.web.boincclient.FetchResponseException
import at.happywetter.boinc.web.hacks.TextEncoder
import org.scalajs.dom
import org.scalajs.dom.experimental.{Fetch, HttpMethod, RequestInit}
import prickle.Pickle

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray.{ArrayBuffer, DataView}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.08.2017
  */
object AuthClient {

  def validate(username: String, password: String): Future[Boolean] = {
    var n = ""

    Fetch.fetch("/auth", RequestInit(method = HttpMethod.GET, headers = FetchHelper.header))
      .toFuture
      .flatMap(response => response.text().toFuture)
      .flatMap(nonce => {n = nonce; hashPassword(password, nonce) } )
      .flatMap(pwHash => requestToken(User(username, pwHash, n)))
      .map(token => {
        if (token != null && token.nonEmpty) {
          FetchHelper.setToken(token)
          true
        } else {
          false
        }
      }).recover {
        case _: FetchResponseException => false
      }
  }

  private def requestToken(user: User): Future[String] = {
    Fetch.fetch("/auth", RequestInit(method = HttpMethod.POST, headers = FetchHelper.header, body = Pickle.intoString(user)))
      .toFuture
      .map(response => if (response.status == 200) response else throw FetchResponseException(response.status))
      .flatMap(response => response.text().toFuture)
  }

  private def hashPassword(password: String, nonce: String): Future[String] = {
    dom.crypto.crypto.subtle
      .digest(dom.crypto.HashAlgorithm.`SHA-256`, new TextEncoder("utf-8").encode(nonce + password).buffer)
      .toFuture
      .map((buffer) => {
        val hex = StringBuilder.newBuilder
        val view = new DataView(buffer.asInstanceOf[ArrayBuffer])
        for (i <- 0 until view.byteLength by 2) {
          hex.append(view.getUint16(i).toHexString.reverse.padTo(4, '0').reverse)
        }

        hex.toString()
      })
  }

  def hasToken: Boolean = {
    val token = FetchHelper.header.get("X-Authorization")
    token != null && token.toOption.nonEmpty
  }

  def tryLogin: Future[Boolean] = {
    if (!hasToken) {
      val username = dom.window.sessionStorage.getItem("username")
      val password = dom.window.sessionStorage.getItem("password")

      validate(username, password)
    } else {
      Future { true }
    }
  }
}
