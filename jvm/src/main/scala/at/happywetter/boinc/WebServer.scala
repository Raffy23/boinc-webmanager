package at.happywetter.boinc

import at.happywetter.boinc.extensions.linux.HWStatusService
import at.happywetter.boinc.server._
import at.happywetter.boinc.util.IOAppTimer.scheduler
import at.happywetter.boinc.util.{BoincHostFinder, ConfigurationChecker, IOAppTimer, Logger}
import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.SSLKeyStoreSupport.StoreInfo
import org.http4s.server.blaze._
import org.http4s.server.middleware.GZip

import scala.io.StdIn

/**
  * @author Raphael
  * @version 19.07.2017
  */
object WebServer extends IOApp with Logger {

  private lazy val config = AppConfig.conf
  private lazy val projects = new XMLProjectStore(config.boinc.projects.xmlSource)
  private lazy val hostManager = new BoincManager(config.boinc.connectionPool, config.boinc.encoding)
  private val autoDiscovery = new BoincHostFinder(config, hostManager)

  ConfigurationChecker.checkConfiguration(config)

  // Populate Host Manager with clients
  config.boinc.hosts.foreach(hostManager.add)
  config.hostGroups.foreach{ case (group, hosts) => hostManager.addGroup(group, hosts)}

  // Create services
  private val authService = new AuthenticationService(config)
  private val hw   = {
    if (config.hardware.isDefined && config.hardware.get.enabled) {
      val hwStatusService = new HWStatusService(
        config.hardware.get.binary,
        config.hardware.get.params,
        config.hardware.get.cacheTimeout
      )

      HardwareAPIRoutes(config.hardware.get.hosts.toSet, hwStatusService)
    } else {
      HttpRoutes.of[IO] {
        case GET -> Root => NotFound()
      }
    }
  }

  // Create top level routes
  private val routes = Router(
    "/"              -> WebResourcesRoute(config),
    "/api"           -> authService.protectedService(BoincApiRoutes(hostManager, projects)),
    "/api/webrpc"    -> authService.protectedService(WebRPCRoutes(config.webRPC)),
    "/api/hardware"  -> hw,
    "/api/ws"        -> WebsocketRoutes(authService, hostManager),
    "/auth"          -> authService.authService,
    "/language"      -> GZip(LanguageService())
  )

  override def run(args: List[String]): IO[ExitCode] = {
    LOG.info("Boinc-Webmanager: current Version: " + BuildInfo.version)
    LOG.info(s"Using scheduler with ${IOAppTimer.cores} cores as pool size")

    import cats.implicits._
    BlazeServerBuilder[IO]
      .enableHttp2(false) // Can't use web sockets if http2 is enabled (0.21.0-M2)
      .withSSL(StoreInfo(config.server.ssl.keystore, config.server.ssl.password), config.server.ssl.password)
      .bindHttp(config.server.port, config.server.address)
      .withHttpApp(routes.orNotFound)
      .resource
      .use(_ => IO {
        projects.importFrom(config)
        autoDiscovery.beginSearch()

        println("Press ENTER to exit the server ...")
        StdIn.readLine()

        scheduler.shutdownNow()
        hostManager.destroy()
      })
      .as(ExitCode.Success)
  }
}
