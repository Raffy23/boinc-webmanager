package at.happywetter.boinc

import java.io.File
import java.util.concurrent.{Executors, ScheduledExecutorService}

import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.server._
import org.http4s.server.blaze.BlazeBuilder

import scala.io.StdIn

/**
  * @author Raphael
  * @version 19.07.2017
  */
object WebServer extends App  {

  private implicit val scheduler: ScheduledExecutorService =
    Executors.newScheduledThreadPool(Runtime.getRuntime.availableProcessors())

  private lazy val hostManager = new BoincManager()
  private lazy val config = AppConfig.conf
  private lazy val projects = new XMLProjectStore(config.boinc.projects.xmlSource)

  // Check environment
  if (!new File("./application.conf").exists()) {
    System.err.println("Unable to read ./application.conf!")
    System.exit(1)
  }

  // Check if XML-Source is available
  if (!new File(config.boinc.projects.xmlSource).exists()) {
    System.err.println("Unable to read Boinc XML Project definition!")
    System.exit(1)
  }

  if (!new File(config.server.webroot).exists()) {
    System.err.println("Webroot is not a valid Directory!")
    System.exit(1)
  }

  // Populate Host Manager with clients
  config.boinc.hosts.foreach { case (name, host) =>
    hostManager.add(name, new BoincClient(address = host.address, port = host.port, password = host.password))
  }

  private val authService = new AuthenticationService(config)


  private val builder =
    BlazeBuilder
    .bindHttp(config.server.port, config.server.address)
    .mountService(JsonMiddleware(authService.protectedService(BoincApiRoutes(hostManager, projects))), "/api")
    .mountService(WebResourcesRoute(config), "/")
    .mountService(authService.authService, "/auth")

  private val server = builder.run

  println(s"Server online at http://${config.server.address}:${config.server.port}/\nPress RETURN to stop...")
  StdIn.readLine()               // let it run until user presses return

  // Cleanup
  hostManager.destroy()
  scheduler.shutdownNow()
  server.shutdownNow()

}
