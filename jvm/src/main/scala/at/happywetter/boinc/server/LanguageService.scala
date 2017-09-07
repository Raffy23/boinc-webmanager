package at.happywetter.boinc.server

import at.happywetter.boinc.util.ResourceWalker
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

import scala.io.Source
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

  import org.http4s._
  import org.http4s.dsl._
  import prickle._

  def apply(): HttpService = HttpService {

    case GET -> Root => Ok(Pickle.intoString(languages))
    case GET -> Root / lang =>
      Try(
        Ok(
          Pickle.intoString(load(lang))
        )
      ).getOrElse(NotFound())

  }

  private def load(lang: String): Map[String, String] = {
    val path = s"${ResourceWalker.RESOURCE_ROOT}/lang/$lang.conf"
    val ins  = ResourceWalker.getStream(path)

    val confString: String = Source.fromInputStream(ins).getLines().mkString("\n")
    val hocon: TypesafeConfig = ConfigFactory.parseString(confString).resolve()

    import pureconfig._
    loadConfigOrThrow[Map[String, String]](hocon)
  }

}
