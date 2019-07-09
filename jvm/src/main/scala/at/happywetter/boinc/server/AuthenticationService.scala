package at.happywetter.boinc.server

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.shared.webrpc.{ApplicationError, User}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.http4s.util.CaseInsensitiveString

import scala.util.{Failure, Random, Success, Try}
import scala.language.implicitConversions
import at.happywetter.boinc.shared.parser._
import upickle.default.{writeBinary}
import at.happywetter.boinc.util.http4s.RichMsgPackRequest.RichMsgPacKResponse

/**
  * Created by: 
  *
  * @author Raphael
  * @version 18.08.2017
  */
class AuthenticationService(config: Config) {

  import AuthenticationService.toDate
  import cats.effect._
  import org.http4s._, org.http4s.dsl.io._

  private val algorithm = Algorithm.HMAC512(config.server.secret)
  private val jwtBuilder = JWT.create()
  private val jwtVerifyer = JWT.require(algorithm)

  def authService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root => Ok(writeBinary(AuthenticationService.nonce))

    case request @ GET -> Root / "refresh" =>
      request.headers.get(CaseInsensitiveString("X-Authorization")).map(header => {
        validate(header.value) match {
          case Success(true) => Ok(writeBinary(refreshToken(header.value)))
          case _ => new Response[IO](status = Unauthorized)
            .withBody(writeBinary(ApplicationError("error_invalid_token")))
        }

      }).getOrElse(
        new Response[IO](status = Unauthorized)
          .withBody(writeBinary(ApplicationError("error_no_token")))
      )

    case request @ POST -> Root =>
      println(s"POST: ${writeBinary(User("admin", AuthenticationService.sha256Hash("password"), "b4775713f235c5173b166820dfa04208923fb38096b28f7a6b0a697f088377e6")).mkString("")}")
      request.decodeJson[User]{ user =>
        if (config.server.username.equals(user.username)
          && user.passwordHash.equals(AuthenticationService.sha256Hash(user.nonce + config.server.password)))
          Ok(writeBinary(buildToken(user.username)))
        else
          BadRequest(writeBinary(ApplicationError("error_invalid_credentials")))
      }

  }

  private def denyService(errorText: String): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case _ =>
      new Response[IO]()
        .withBody(writeBinary(ApplicationError(errorText)))
        .map(_.withStatus(Unauthorized))
  }

  // TODO: Update to http4s 0.20.0-M4
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