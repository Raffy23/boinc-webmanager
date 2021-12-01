package at.happywetter.boinc.server

import at.happywetter.boinc.util.ResourceWalker
import at.happywetter.boinc.util.http4s.ResponseEncodingHelper
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

import scala.io.{Source, Codec => IOCodec}
import upickle.default._
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import java.security.MessageDigest

import org.http4s.headers.`Content-Length`
import org.http4s.server.middleware.GZip

/**
  * Created by: 
  *
  * @author Raphael
  * @version 29.08.2017
  */
object LanguageService extends ResponseEncodingHelper {

  private val languages: List[(String, String, String)] =
    ResourceWalker
      .listFiles("/lang")
      .filter(_.endsWith(".conf"))
      .map(_.split("\\.")(0))
      .map(language => {
        val lang = load(language)
        (language, lang("language_name"), lang("language_icon"))
      })

  private val langETags: Map[String, String] = languages.map { case (name, _, _) =>
    (name, loadETag(name))
  }.toMap

  def apply(): HttpRoutes[IO] = GZip(
    HttpRoutes.of[IO] {
      case request @ GET -> Root                                     => Ok(languages, request)
      case request @ GET -> Root / lang if  langETags.contains(lang) => OkWithEtag(load(lang), langETags(lang), request)
      case request @ GET -> Root / lang if !langETags.contains(lang) => NotFound()
    },
    isZippable = (response: Response[IO]) => response.headers.get[`Content-Length`].exists(_.length > 1024)
  )

  private def load(lang: String): Map[String, String] = {
    val path      = s"${ResourceWalker.RESOURCE_ROOT}/lang/$lang.conf"
    val bufSource = ResourceWalker.getStream(path)

    val confString: String = {
      val ins = Source.fromInputStream(bufSource)(IOCodec.UTF8)
      val config = ins.getLines().mkString("\n")
      ins.close()

      config
    }
    val hocon: TypesafeConfig = ConfigFactory.parseString(confString).resolve()

    import pureconfig._
    ConfigSource.fromConfig(hocon).loadOrThrow[Map[String,  String]]
  }

  private def loadETag(lang: String): String = {
    val path      = s"${ResourceWalker.RESOURCE_ROOT}/lang/$lang.conf"
    val bufSource = ResourceWalker.getStream(path)

    val digest = MessageDigest.getInstance("SHA-1")
    val buffer = Array.ofDim[Byte](1024)
    var nRead  = -1
    while({nRead = bufSource.read(buffer, 0, buffer.length); nRead} != -1) {
      digest.update(buffer, 0, nRead)
    }

    val hash = digest.digest()
    bufSource.close()

    hash.map("%02x" format _).mkString.take(12)
  }

}
