package at.happywetter.boinc

import at.happywetter.boinc.extensions.linux.HWStatusService
import at.happywetter.boinc.server._
import at.happywetter.boinc.util.http4s.CustomBlazeServerBuilder._
import at.happywetter.boinc.util.{BoincHostFinder, ConfigurationChecker, JobManager, Logger}
import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware.GZip


/**
  * @author Raphael
  * @version 19.07.2017
  */
object WebServer extends IOApp with Logger {

  private lazy val config      = AppConfig.conf
  ConfigurationChecker.checkConfiguration(config)

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
  // Seems kinda broken in 1.0.0-M3, can't access /api/webrpc or /api/hardware so they are outside /api for now ...
  private def routes(hostManager: BoincManager, xmlProjectStore: XMLProjectStore, db: Database, jobManager: JobManager) = Router(
    "/"              -> GZip(WebResourcesRoute(config)),
    "/swagger"       -> SwaggerRoutes(),
    "/api"           -> authService.protectedService(BoincApiRoutes(hostManager, xmlProjectStore, db)),
    "/webrpc"        -> authService.protectedService(WebRPCRoutes()),                               // <--- TODO: Document in Swagger
    "/hardware"      -> authService.protectedService(hw),                                           // <--- TODO: Document in Swagger
    "/ws"            -> WebsocketRoutes(authService, hostManager),
    "/auth"          -> authService.authService,
    "/language"      -> LanguageService(),
    "/jobs"          -> JobManagerRoutes(db, jobManager)
  )

  override def run(args: List[String]): IO[ExitCode] = {
    LOG.info("Boinc-Webmanager: current Version: " + BuildInfo.version)
    LOG.info(s"Using scheduler with ${Runtime.getRuntime.availableProcessors} cores as pool size")

    (for {
      database    <- Database()
      xmlPStore   <- XMLProjectStore(database, config)
      hostManager <- BoincManager(config, database)
      jobManager  <- JobManager(hostManager, database)

      // TODO: for Linux with systemd privileged socket can be inherited,
      //       how to convince Blaze to use it?
      webserver   <- BlazeServerBuilder[IO]
                        .enableHttp2(false) // Can't use web sockets if http2 is enabled (since 0.21.0-M2)
                        .withOptionalSSL(config)
                        .bindHttp(config.server.port, config.server.address)
                        .withHttpApp(routes(hostManager, xmlPStore, database, jobManager).orNotFound)
                        .resource

      autoDiscovery <- BoincHostFinder(config, hostManager, database)
    } yield (database, webserver, autoDiscovery))
      .use(_ =>
        if (config.serviceMode)
          serviceMode
        else
          interactive
      ).as(ExitCode.Success)
  }

  private val serviceMode = IO.println("Running in service mode, waiting for signal ...") *> IO.never
  private val interactive = {
    IO.println("Press ENTER to exit the server ...") *>
    IO.readLine
  }

}
