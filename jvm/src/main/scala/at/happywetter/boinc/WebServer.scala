package at.happywetter.boinc

import at.happywetter.boinc.extensions.linux.HWStatusService
import at.happywetter.boinc.server._
import at.happywetter.boinc.util.IOAppTimer.scheduler
import at.happywetter.boinc.util.http4s.CustomBlazeServerBuilder._
import at.happywetter.boinc.util.{BoincHostFinder, ConfigurationChecker, IOAppTimer, Logger}
import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.server.middleware.GZip

import scala.io.StdIn

/**
  * @author Raphael
  * @version 19.07.2017
  */
object WebServer extends IOApp with Logger {

  private lazy val config      = AppConfig.conf
  private lazy val hostManager = new BoincManager(config.boinc.connectionPool, config.boinc.encoding)

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
  private def routes(xmlProjectStore: XMLProjectStore) = Router(
    "/"              -> WebResourcesRoute(config),
    "/swagger"       -> SwaggerRoutes(),
    "/api"           -> authService.protectedService(BoincApiRoutes(hostManager, xmlProjectStore)),
    "/api/webrpc"    -> authService.protectedService(WebRPCRoutes()),                               // <--- TODO: Document in Swagger
    "/api/hardware"  -> authService.protectedService(hw),                                           // <--- TODO: Document in Swagger
    "/ws"            -> WebsocketRoutes(authService, hostManager),
    "/auth"          -> authService.authService,
    "/language"      -> GZip(LanguageService())
  )

  override def run(args: List[String]): IO[ExitCode] = {
    LOG.info("Boinc-Webmanager: current Version: " + BuildInfo.version)
    LOG.info(s"Using scheduler with ${IOAppTimer.cores} cores as pool size")

    // TODO: Use resource management ...
    (for {
      database  <- Database()
      xmlPStore <- XMLProjectStore(database, config)
      webserver <-  BlazeServerBuilder[IO](IOAppTimer.defaultExecutionContext)
                      .enableHttp2(false) // Can't use web sockets if http2 is enabled (0.21.0-M2)
                      .withOptionalSSL(config)
                      .bindHttp(config.server.port, config.server.address)
                      .withHttpApp(routes(xmlPStore).orNotFound)
                      .resource

      // This is kinda ugly ...
      autoDiscovery <- new BoincHostFinder(config, hostManager).beginSearch()
    } yield (database, webserver, autoDiscovery))
      .use(_ =>
        if (config.serviceMode)
          IO {
            println("Running in service mode, waiting for signal ...")
          }.flatMap(_ => IO.never)
        else
          IO.async[Unit] { resolve =>
            println("Press ENTER to exit the server ...")
            StdIn.readLine()

            scheduler.shutdownNow()
            hostManager.destroy()
            resolve(Right(()))
          }
      ).as(ExitCode.Success)
  }
}
