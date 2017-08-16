package at.happywetter.boinc

import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

import scala.io.Source

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.07.2017
  */
object AppConfig {

  case class Config(server: Server, boinc: Boinc)
  case class Server(address: String, port: Short, username: String, password: String, webroot: String)
  case class Host(address: String, port: Short, password: String)
  case class Boinc(hosts: Map[String, Host], projects: Projects)
  case class Projects(xmlSource: String, customProjects: Map[String, ProjectEntry])
  case class ProjectEntry(url: String, generalArea: String)

  val conf: Config = {
    val confString: String = Source.fromFile("./application.conf").getLines().mkString("\n")
    val hocon: TypesafeConfig = ConfigFactory.parseString(confString).resolve()

    import pureconfig._
    loadConfigOrThrow[Config](hocon)
  }

}
