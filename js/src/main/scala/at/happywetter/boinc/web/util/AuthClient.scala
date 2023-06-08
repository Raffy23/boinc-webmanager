package at.happywetter.boinc.web.util

import at.happywetter.boinc.shared.webrpc.User
import at.happywetter.boinc.web.boincclient.FetchResponseException
import at.happywetter.boinc.web.facade.TextEncoder
import at.happywetter.boinc.web.pages.LoginPage
import at.happywetter.boinc.web.routes.{AppRouter, NProgress}
import at.happywetter.boinc.web.util.I18N._
import at.happywetter.boinc.shared.parser._
import org.scalajs.dom
import org.scalajs.dom.HTMLInputElement

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.typedarray.{ArrayBuffer, DataView}
import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by:
  *
  * @author Raphael
  * @version 19.08.2017
  */
object AuthClient:
  type AuthToken = String

  val isSecureEndpoint: Boolean = dom.window.location.protocol == "https:"

  private val TOKEN_VALID_TIME = 20 * 60 * 1000
  private var refreshTimeoutHandler: Int = -1

  def validate(username: String, password: String): Future[Boolean] =
    var n = ""

    FetchHelper
      .get[String]("/auth")
      .flatMap(nonce => { n = nonce; hashPassword(password, nonce) })
      .flatMap(pwHash => requestToken(User(username, pwHash, n)))
      .map(token => {
        if (token != null && token.nonEmpty) {
          saveToken(token)
          true
        } else {
          false
        }
      })
      .recover:
        case e: FetchResponseException =>
          dom.console.error(e.localize)
          false

        case e: Exception =>
          e.printStackTrace()
          false

  def validateSavedCredentials(): Future[Boolean] =
    this.validate(
      dom.window.sessionStorage.getItem("username"),
      dom.window.sessionStorage.getItem("password")
    )

  def validateAction(done: js.Function0[Unit]): Boolean =
    if (!FetchHelper.hasToken)
      NProgress.done(true)
      AppRouter.navigate(LoginPage.link)
      false
    else
      enableTokenRefresh()
      done()
      true

  private def requestToken(user: User): Future[AuthToken] =
    if (user == null || user.username == null || user.passwordHash == null || user.nonce == null)
      return Future.failed(new RuntimeException("User case class is not valid! (" + user + ")"))
    FetchHelper.post[User, AuthToken]("/auth", user)

  private def hashPassword(password: String, nonce: String): Future[String] =
    if (isSecureEndpoint)
      dom.crypto.subtle
        .digest(dom.HashAlgorithm.`SHA-256`, new TextEncoder("utf-8").encode(nonce + password).buffer)
        .toFuture
        .map(buffer => {
          val hex = new StringBuilder()
          val view = new DataView(buffer.asInstanceOf[ArrayBuffer])
          for (i <- 0 until view.byteLength by 2) {
            hex.append(view.getUint16(i).toHexString.reverse.padTo(4, '0').reverse)
          }

          hex.toString()
        })
    else
      Future { password }

  def refreshToken(): Future[AuthToken] =
    FetchHelper
      .get[AuthToken]("/auth/refresh")
      .map(token => {
        saveToken(token)
        token
      })

  def enableTokenRefresh(): Unit =
    if (refreshTimeoutHandler == -1)
      refreshTimeoutHandler = dom.window.setInterval(() => { refreshToken() }, TOKEN_VALID_TIME)

  def loadFromLocalStorage(): Boolean =
    val tokenDate = dom.window.localStorage.getItem("auth/time")
    val token = dom.window.localStorage.getItem("auth/token")
    if (tokenDate == null || token == null) return false

    if (tokenDate.toDouble + TOKEN_VALID_TIME < new Date().getTime())
      dom.window.localStorage.removeItem("auth/time")
      dom.window.localStorage.removeItem("auth/token")

      return false

    FetchHelper.setToken(token)
    true

  private def saveToken(token: String): Unit =
    dom.window.localStorage.setItem("auth/token", token)
    dom.window.localStorage.setItem("auth/time", new Date().getTime().toString)

    FetchHelper.setToken(token)
    enableTokenRefresh()
