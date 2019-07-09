package at.happywetter.boinc.server

import at.happywetter.boinc.util.ResourceWalker
import at.happywetter.boinc.util.http4s.MsgPackRequRespHelper
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

import scala.io.{Source, Codec => IOCodec}
import scala.util.Try
import upickle.default._
import cats.effect._
import org.http4s._, org.http4s.dsl.io._

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
object LanguageService extends MsgPackRequRespHelper {

  val languages: List[(String, String, String)] =
    ResourceWalker
      .listFiles("/lang")
      .filter(_.endsWith(".conf"))
      .map(_.split("\\.")(0))
      .map(language => {
        val lang = load(language)
        (language, lang("language_name"), lang("language_icon"))
      })

  def apply(): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case request @ GET -> Root => Ok(languages, request)
    case request @ GET -> Root / lang => Try(Ok(load(lang), request)).getOrElse(NotFound())

  }

  private def load(lang: String): Map[String, String] = {
    val path = s"${ResourceWalker.RESOURCE_ROOT}/lang/$lang.conf"
    val ins  = ResourceWalker.getStream(path)

    val confString: String = Source.fromInputStream(ins)(IOCodec.UTF8).getLines().mkString("\n")
    val hocon: TypesafeConfig = ConfigFactory.parseString(confString).resolve()

    import pureconfig._
    loadConfigOrThrow[Map[String, String]](hocon)
  }

}
