package at.happywetter.boinc

import java.io.File
import java.util.concurrent.TimeUnit

import at.happywetter.boinc.boincclient.webrpc.ProjectRules
import at.happywetter.boinc.shared.webrpc.ServerSharedConfig
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

import scala.concurrent.duration.FiniteDuration
import scala.io.Source

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.07.2017
  */
object AppConfig {

  case class Config(server: Server,
                    boinc: Boinc,
                    development: Option[Boolean] = Some(false),
                    autoDiscovery: AutoDiscovery,
                    hostGroups: Map[String, List[String]],
                    hardware: Option[Hardware],
                    serviceMode: Boolean,
                    /*webRPC: WebRPC*/)

  case class Server(address: String,
                    port: Short,
                    username: String,
                    password: String,
                    secureEndpoint: Boolean,
                    webroot: String = "",
                    secret: String,
                    ssl: SSLConfig)

  case class Host(address: String, port: Short, password: String)

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
                           password: List[String])

  case class Hardware(enabled: Boolean,
                      hosts: List[String],
                      binary: String,
                      params: List[String],
                      cacheTimeout: Long
  )

  //case class WebRPC(parser: Parser, rules: Map[String, WebRPCRule])
  //case class Parser(default: Int = ProjectRules.UseXMLParser)
  //case class WebRPCRule(serverStatus: Int)

  val typesafeConfig: TypesafeConfig = ConfigFactory.parseFile(new File("./application.conf"))

  val conf: Config = {
    import pureconfig._
    import pureconfig.generic.auto._
    ConfigSource.fromConfig(typesafeConfig).loadOrThrow[Config]
  }

  lazy val sharedConf: ServerSharedConfig = ServerSharedConfig(
    if (conf.autoDiscovery.enabled) FiniteDuration(conf.autoDiscovery.scanTimeout, TimeUnit.MINUTES).toMillis
    else FiniteDuration(12, TimeUnit.HOURS).toMillis,
    conf.hardware.exists(_.enabled)
  )

}
