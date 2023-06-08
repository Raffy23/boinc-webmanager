package at.happywetter.boinc

import java.io.File
import java.util.concurrent.TimeUnit
import at.happywetter.boinc.shared.boincrpc.ServerSharedConfig
import cats.effect.{Async, IO}
import com.typesafe.config.{Config as TypesafeConfig, ConfigFactory}

import scala.concurrent.duration.FiniteDuration
import scala.io.Source

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.07.2017
  */
object AppConfig:
  import pureconfig._
  import pureconfig.generic.derivation.default._

  case class Config(server: Server,
                    boinc: Boinc,
                    development: Option[Boolean] = Some(false),
                    autoDiscovery: AutoDiscovery,
                    hostGroups: Map[String, List[String]],
                    hardware: Option[Hardware],
                    serviceMode: Boolean
                    /*webRPC: WebRPC*/
  )

  case class Server(address: String,
                    port: Short,
                    username: String,
                    password: String,
                    secureEndpoint: Boolean,
                    webroot: String = "",
                    secret: String,
                    ssl: SSLConfig
  )

  case class Host(address: String, port: Int, password: String)

  case class Boinc(hosts: Map[String, Host], projects: Projects, connectionPool: Int, encoding: String)

  case class Projects(xmlSource: String, customProjects: Map[String, ProjectEntry])

  case class ProjectEntry(url: String, generalArea: String, description: String, organization: String)

  case class SSLConfig(enabled: Boolean, keystore: String, password: String)

  case class AutoDiscovery(startIp: String,
                           endIp: String,
                           timeout: Int,
                           port: Int,
                           enabled: Boolean,
                           scanTimeout: Int,
                           maxScanRequests: Int = 10,
                           password: List[String]
  )

  case class Hardware(enabled: Boolean,
                      hosts: List[String],
                      binary: String,
                      params: List[String],
                      cacheTimeout: Long,
                      actions: Map[String, Seq[String]]
  )

  // case class WebRPC(parser: Parser, rules: Map[String, WebRPCRule])
  // case class Parser(default: Int = ProjectRules.UseXMLParser)
  // case class WebRPCRule(serverStatus: Int)
  lazy val typesafeConfig = ConfigFactory.parseFile(new File("./application.conf"))

  lazy val conf: Config =
    import pureconfig.module.cats._

    given ConfigReader[Host] = ConfigReader.derived
    given ConfigReader[Boinc] = ConfigReader.derived
    given ConfigReader[Projects] = ConfigReader.derived
    given ConfigReader[ProjectEntry] = ConfigReader.derived
    given ConfigReader[SSLConfig] = ConfigReader.derived
    given ConfigReader[AutoDiscovery] = ConfigReader.derived
    given ConfigReader[Hardware] = ConfigReader.derived

    ConfigSource
      .fromConfig(typesafeConfig)
      .load[Config](ConfigReader.derived) // FIXME: Why is implicit broken here?
      .fold(ex => throw new RuntimeException(ex.prettyPrint()), identity)

  val sharedConf: ServerSharedConfig =
    ServerSharedConfig(
      if conf.autoDiscovery.enabled then FiniteDuration(conf.autoDiscovery.scanTimeout, TimeUnit.MINUTES).toMillis
      else FiniteDuration(12, TimeUnit.HOURS).toMillis,
      conf.hardware.exists(_.enabled)
    )
