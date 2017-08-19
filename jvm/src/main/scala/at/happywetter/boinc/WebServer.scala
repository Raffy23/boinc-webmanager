package at.happywetter.boinc

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

  private val hostManager = new BoincManager()
  private val config = AppConfig.conf
  private val projects = new XMLProjectStore(AppConfig.conf.boinc.projects.xmlSource)

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
  scheduler.shutdownNow()
  server.shutdownNow()
}
