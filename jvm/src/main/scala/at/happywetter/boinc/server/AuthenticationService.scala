package at.happywetter.boinc.server

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.shared.User
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.http4s.util.CaseInsensitiveString
import prickle.Unpickle

import scala.util.Random
import scalaz.concurrent.Task
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
    case request @ POST -> Root =>
      request.body
        .map(b => Unpickle[User].fromString(b.decodeUtf8.right.get))
        .map(requestBody =>
          requestBody.toOption.map(user => {
            if ( config.server.username.equals(user.username)
              && user.passwordHash.equals(AuthenticationService.sha256Hash(user.nonce+config.server.password)))
              try {
                Ok(jwtBuilder.withClaim("user", user.username).withExpiresAt(LocalDateTime.now().plusHours(1)).sign(algorithm))
              } catch {
                case ex: Exception =>
                  ex.printStackTrace()
                  InternalServerError()
              }
            else
              BadRequest("Username or Password are invalid!")
          }).getOrElse(BadRequest("Missing User POST-Data!"))
        )
        .runLast.unsafePerformSync.get
  }

  def protectedService(service: HttpService): HttpService = Service.lift { req =>
    Task {
      req.headers.get(CaseInsensitiveString("X-Authorization")).map(header => {
        try {
          val token = jwtVerifyer.build().verify(header.value)

          if (token.getExpiresAt.before(new Date())) new Response().withBody("Token has expired").withStatus(Forbidden).unsafePerformSync
          else service(req).unsafePerformSync
        } catch {
          case a: Exception =>
            a.printStackTrace()
            new Response().withStatus(InternalServerError)
        }
      }).getOrElse(new Response().withBody("No Header was given!").withStatus(Forbidden).unsafePerformSync)
    }
  }
}

object AuthenticationService {

  val TIMEOUT: Long = 30 //Minutes

  def nonce: String = sha256Hash(Random.alphanumeric.take(64).toString())

  def sha256Hash(text: String) : String =
    String.format("%064x",
      new java.math.BigInteger(1, java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8")))
    )

  implicit def toDate(localDateTime: LocalDateTime): Date = {
    Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant)
  }

}