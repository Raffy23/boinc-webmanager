package at.happywetter.boinc.server

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.shared.User
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import fs2.Task
import org.http4s.util.CaseInsensitiveString
import prickle.Unpickle

import scala.util.{Random, Try}
import scala.language.implicitConversions

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.08.2017
  */
class AuthenticationService(config: Config) {

  import AuthenticationService.toDate
  import org.http4s._
  import org.http4s.dsl._

  private val algorithm = Algorithm.HMAC512(config.server.secret)
  private val jwtBuilder = JWT.create()
  private val jwtVerifyer = JWT.require(algorithm)

  def authService: HttpService = HttpService {
    case GET -> Root => Ok(AuthenticationService.nonce)

    case request @ GET -> Root / "refresh" =>
      request.headers.get(CaseInsensitiveString("X-Authorization")).map(header => {

        if (!validate(header.value)) Forbidden()
        else Ok(refreshToken(header.value))

      }).getOrElse(Forbidden())

    case request @ POST -> Root =>
      request.body
        .map(_.toChar).runLog
        .map(_.mkString)
        .map(Unpickle[User].fromString(_))
        .map(requestBody =>
          requestBody.toOption.map(user => {
            if ( config.server.username.equals(user.username)
              && user.passwordHash.equals(AuthenticationService.sha256Hash(user.nonce+config.server.password)))
                Ok(buildToken(user.username))
            else
              BadRequest("Username or Password are invalid!")
          }).getOrElse(BadRequest("Missing User POST-Data!"))
        ).unsafeRun()
  }

  def protectedService(service: HttpService): HttpService = Service.lift { req =>
    req.headers.get(CaseInsensitiveString("X-Authorization")).map(header => {

      if (validate(header.value)) {
        service(req)
      } else {
        new Response().withBody("Token has expired").withStatus(Forbidden)
      }

    }).getOrElse(new Response().withBody("No Header was given!").withStatus(Forbidden))
  }

  def validate(token: String): Boolean = Try(jwtVerifyer.build().verify(token).getExpiresAt.after(new Date())).getOrElse(false)
  def buildToken(user: String): String = jwtBuilder.withClaim("user", user).withExpiresAt(LocalDateTime.now().plusHours(1)).sign(algorithm)
  def refreshToken(token: String): String = {
    val jwtToken = jwtVerifyer.build().verify(token)
    val curUser  = jwtToken.getClaim("user")

    buildToken(curUser.asString())
  }
}

object AuthenticationService {

  val TIMEOUT: Long = 30 //Minutes

  def nonce: String = sha256Hash(Random.alphanumeric.take(64).toString()+new Date().toString)

  def sha256Hash(text: String) : String =
    String.format("%064x",
      new java.math.BigInteger(1, java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8")))
    )

  implicit def toDate(localDateTime: LocalDateTime): Date = {
    Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant)
  }

}