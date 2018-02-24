package at.happywetter.boinc

import java.util.concurrent.{Executors, ScheduledExecutorService}

import at.happywetter.boinc.extensions.linux.HWStatusService
import at.happywetter.boinc.server._
import at.happywetter.boinc.util.{BoincHostSettingsResolver, ConfigurationChecker}
import org.http4s.HttpService
import org.http4s.server.SSLKeyStoreSupport.StoreInfo
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.{GZip, HSTS}
import cats.effect._

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
    BlazeBuilder[IO]
      .enableHttp2(true) // h2spec check fails in 0.18
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

  private val server = builder.start.unsafeRunSync()
  println(s"Server online at https://${config.server.address}:${config.server.port}/")

  if (!config.serviceMode) {
    println("Press RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
  } else {
    println("Server was started in service mode, send SIG_TERM to shut the JVM down")
  }

  // Cleanup
  hostManager.destroy()
  scheduler.shutdownNow()
  server.shutdownNow()


  private def service(service: HttpService[IO]): HttpService[IO] = GZip(HSTS(JsonMiddleware(service)))

}
