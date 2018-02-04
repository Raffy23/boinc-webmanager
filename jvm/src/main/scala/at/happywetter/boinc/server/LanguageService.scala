package at.happywetter.boinc.server

import at.happywetter.boinc.util.ResourceWalker
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

import scala.io.{Codec, Source}
import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
object LanguageService {

  val languages: List[(String, String, String)] =
    ResourceWalker
      .listFiles("/lang")
      .filter(_.endsWith(".conf"))
      .map(_.split("\\.")(0))
      .map(language => {
        val lang = load(language)
        (language, lang("language_name"), lang("language_icon"))
      })

  import cats.effect._
  import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._
  import org.http4s.circe._
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

  def apply(): HttpService[IO] = HttpService[IO] {

    case GET -> Root => Ok(languages.asJson)
    case GET -> Root / lang =>
      Try(
        Ok(
          load(lang).asJson
        )
      ).getOrElse(NotFound())

  }

  private def load(lang: String): Map[String, String] = {
    val path = s"${ResourceWalker.RESOURCE_ROOT}/lang/$lang.conf"
    val ins  = ResourceWalker.getStream(path)

    val confString: String = Source.fromInputStream(ins)(Codec.UTF8).getLines().mkString("\n")
    val hocon: TypesafeConfig = ConfigFactory.parseString(confString).resolve()

    import pureconfig._
    loadConfigOrThrow[Map[String, String]](hocon)
  }

}
