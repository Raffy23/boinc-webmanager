package at.happywetter.boinc.server

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.shared.{ApplicationError, User}
import cats.data.OptionT
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.http4s.util.CaseInsensitiveString

import scala.util.{Failure, Random, Success, Try}
import scala.language.implicitConversions

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.08.2017
  */
class AuthenticationService(config: Config) {

  import AuthenticationService.toDate
  import cats.effect._
  import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._
  import org.http4s.circe._
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

  private val algorithm = Algorithm.HMAC512(config.server.secret)
  private val jwtBuilder = JWT.create()
  private val jwtVerifyer = JWT.require(algorithm)

  def authService: HttpService[IO] = HttpService[IO] {
    case GET -> Root => Ok(AuthenticationService.nonce)

    case request @ GET -> Root / "refresh" =>
      request.headers.get(CaseInsensitiveString("X-Authorization")).map(header => {
        validate(header.value) match {
          case Success(true) => Ok(refreshToken(header.value))
          case _ => new Response[IO]()
            .withBody(ApplicationError("error_invalid_token").asJson)
            .map(_.withStatus(Unauthorized))  // .withStatus(Unauthorized) [Compiler Error?]
        }

      }).getOrElse(
        new Response[IO]()
          .withBody(ApplicationError("error_no_token").asJson)
          .map(_.withStatus(Unauthorized))  // .withStatus(Unauthorized) [Compiler Error?]
      )

    case request @ POST -> Root =>
      request.decode[String] { body =>
        decode[User](body).toOption.map(user => {
          if (config.server.username.equals(user.username)
            && user.passwordHash.equals(AuthenticationService.sha256Hash(user.nonce + config.server.password)))
            Ok(buildToken(user.username))
          else
            BadRequest(ApplicationError("error_invalid_credentials").asJson)
        }).getOrElse(BadRequest(ApplicationError("error_invalid_request").asJson))
      }
  }

  private def denyService(errorText: String): HttpService[IO] = HttpService[IO] {
    case _ =>
      new Response[IO]()
        .withBody(ApplicationError(errorText).asJson)
        .map(_.withStatus(Unauthorized))
  }

  def protectedService(service: HttpService[IO]): HttpService[IO] = Service.lift { req: Request[IO] =>
    req.headers.get(CaseInsensitiveString("X-Authorization")).map(header => {
      validate(header.value) match {
        case Success(true) => service(req)
        case Success(false) => denyService("error_invalid_token")(req)
        case Failure(_) => denyService("error_invalid_token")(req)
      }
    }).getOrElse(denyService("error_no_token")(req))
  }

  def validate(token: String): Try[Boolean] = Try(jwtVerifyer.build().verify(token).getExpiresAt.after(new Date()))
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