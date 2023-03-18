package at.happywetter.boinc

import at.happywetter.boinc.extensions.linux.HWStatusService
import at.happywetter.boinc.server._
import at.happywetter.boinc.util.http4s.CustomEmberServerBuilder._
import at.happywetter.boinc.util.{BoincHostFinder, ConfigurationChecker, JobManager}
import cats.effect._
import cats.effect.std.Semaphore
import cats.effect.unsafe.IORuntime
import com.comcast.ip4s.{Host, Port}
import fs2.io.net.SocketGroup
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware.GZip
import org.http4s.server.websocket.WebSocketBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.nio.channels.ServerSocketChannel


/**
  * @author Raphael
  * @version 19.07.2017
  */
object WebServer extends IOApp {

  // Lazy config such that it can be accessed everywhere in
  // WebServer without getting passed around, but initialized
  // later when needed
  private lazy val config      = AppConfig.conf

  // Create top level routes
  // Seems kinda broken in 1.0.0-M3, can't access /api/webrpc or /api/hardware so they are outside /api for now ...
  private def routes(webSocketBuilder: WebSocketBuilder[IO], hostManager: BoincManager, xmlProjectStore: XMLProjectStore, db: Database, jobManager: JobManager) = {
    val authService = new AuthenticationService(config)
    val hw = {
      if (config.hardware.isDefined && config.hardware.get.enabled) {
        HardwareAPIRoutes(
          config.hardware.get.hosts.toSet,
          config.hardware.map(hardware =>
            new HWStatusService(
              hardware.binary,
              hardware.params,
              hardware.cacheTimeout,
              hardware.actions
            )
          ).get
        )
      } else {
        HttpRoutes.of[IO] {
          case GET -> Root => NotFound()
        }
      }
    }

    Router(
      "/"              -> GZip(WebResourcesRoute(config)),
      "/swagger"       -> SwaggerRoutes(),
      "/api"           -> authService.protectedService(BoincApiRoutes(hostManager, xmlProjectStore, db)),
      "/webrpc"        -> authService.protectedService(WebRPCRoutes()),                               // <--- TODO: Document in Swagger
      "/hardware"      -> authService.protectedService(hw),                                           // <--- TODO: Document in Swagger
      "/ws"            -> WebsocketRoutes(webSocketBuilder, authService, hostManager),
      "/auth"          -> authService.authService,
      "/language"      -> LanguageService(),
      "/jobs"          -> JobManagerRoutes(db, jobManager)
    ).orNotFound
  }

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      _  <- Resource.eval(for {
        logger      <- Slf4jLogger.fromClass[IO](getClass)
        _           <- logger.info(s"Current Boinc-Webmanager version: ${BuildInfo.version}")
        _           <- ConfigurationChecker.checkConfiguration(config, logger)
      } yield ())

      database    <- Database().onFinalize(IO.println("DONE Database"))
      xmlPStore   <- XMLProjectStore(database, config).onFinalize(IO.println("DONE XMLProjectStore"))
      hostManager <- BoincManager(config, database).onFinalize(IO.println("DONE BoincManager")) // <-- problematic
      jobManager  <- JobManager(hostManager, database).onFinalize(IO.println("DONE JobManager"))

      // TODO: for Linux with systemd privileged socket can be inherited,
      //       how to convince Blaze to use it?
      webserver   <- EmberServerBuilder
                        .default[IO]
                        .withOptionalSSL(config)
                        .withHostOption(Host.fromString(config.server.address))
                        .withPort(Port.fromInt(config.server.port).get)
                        .withHttpWebSocketApp(wsBuilder => routes(wsBuilder, hostManager, xmlPStore, database, jobManager))
                        .build
                        .onFinalize(IO.println("DONE EmberServerBuilder"))

      autoDiscovery <- BoincHostFinder(config, hostManager, database).onFinalize(IO.println("DONE BoincHostFinder"))
    } yield (()))
      .use(_ =>
        if (config.serviceMode)
          serviceMode
        else
          interactive
      ).as(ExitCode.Success)
  }

  private val serviceMode = IO.println("Running in service mode, waiting for signal ...") *> IO.never
  private val interactive = for {
    _     <- IO.println("Press ENTER to exit the server ...")
    fiber <- Spawn[IO].start(IO.readLine)
    _     <- fiber.join
    _     <- IO.println("Shutting down server ...")
  } yield ()

}
