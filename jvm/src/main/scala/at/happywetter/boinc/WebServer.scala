package at.happywetter.boinc

import java.util.concurrent.{Executors, ScheduledExecutorService}

import at.happywetter.boinc.extensions.linux.HWStatusService
import at.happywetter.boinc.server._
import at.happywetter.boinc.util.{BoincHostSettingsResolver, ConfigurationChecker}
import org.http4s.HttpService
import org.http4s.server.SSLKeyStoreSupport.StoreInfo
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.{GZip, HSTS}

import scala.io.StdIn

/**
  * @author Raphael
  * @version 19.07.2017
  */
object WebServer extends App  {
  println("Current Version: " + BuildInfo.version)

  private implicit val scheduler: ScheduledExecutorService =
    Executors.newScheduledThreadPool(Runtime.getRuntime.availableProcessors())

  private lazy val config = AppConfig.conf
  private lazy val projects = new XMLProjectStore(config.boinc.projects.xmlSource)
  private lazy val hostManager = new BoincManager(config.boinc.connectionPool, config.boinc.encoding)
  private val autoDiscovery = new BoincHostSettingsResolver(config, hostManager)

  ConfigurationChecker.checkConfiguration(config)

  // Populate Host Manager with clients
  config.boinc.hosts.foreach(hostManager.add)
  config.hostGroups.foreach{ case (group, hosts) => hostManager.addGroup(group, hosts)}

  autoDiscovery.beginSearch()

  private val authService = new AuthenticationService(config)
  projects.importFrom(config)

  private var builder =
    BlazeBuilder
      .enableHttp2(true) // Doesn't work properly in 0.17
      .withSSL(StoreInfo(config.server.ssl.keystore, config.server.ssl.password), config.server.ssl.password)
      .bindHttp(config.server.port, config.server.address)
      .mountService(service(authService.protectedService(BoincApiRoutes(hostManager, projects))), "/api")
      .mountService(HSTS(WebResourcesRoute(config)), "/")
      .mountService(HSTS(authService.authService), "/auth")
      .mountService(service(LanguageService()), "/language")

  // Only enable /hardware routing if enabled in config
  if (config.hardware.isDefined && config.hardware.get.enabled) {
    val hwStatusService = new HWStatusService(
      config.hardware.get.binary,
      config.hardware.get.params,
      config.hardware.get.cacheTimeout)

    builder = builder
      .mountService(
        service(HardwareAPIRoutes(config.hardware.get.hosts.toSet, hwStatusService)),
        "/api/hardware"
      )
  }

  private val server = builder.run
  println(s"Server online and listening at https://${config.server.address}:${config.server.port}")

  if (config.serviceMode) {
    println("Server was started in service mode")
    println("Send SIGTERM or SIGHUP to terminate the server ...")

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        println("Stopping Server ...")
        cleanUp()
      }
    })

  } else {
    println("Server was started in interactive mode")
    println("Press RETURN to stop...")

    StdIn.readLine()               // let it run until user presses return
    cleanUp()
  }

  private def service(service: HttpService): HttpService = GZip(HSTS(JsonMiddleware(service)))

  private def cleanUp(): Unit = {
    hostManager.destroy()
    scheduler.shutdownNow()
    server.shutdownNow()
  }
}
