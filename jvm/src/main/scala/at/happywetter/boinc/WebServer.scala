package at.happywetter.boinc

import at.happywetter.boinc.extensions.linux.HWStatusService
import at.happywetter.boinc.server._
import at.happywetter.boinc.util.http4s.CustomEmberServerBuilder._
import at.happywetter.boinc.util.{BoincHostFinder, ConfigurationChecker, JobManager}
import cats.effect._
import com.comcast.ip4s.{Host, Port}
import io.opentelemetry.api.GlobalOpenTelemetry
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware.GZip
import org.http4s.server.websocket.WebSocketBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.otel4s.java.OtelJava
import org.typelevel.otel4s.trace.Tracer
import scala.concurrent.duration.Duration
import util.http4s.Otel4sMiddelware._

/**
  * @author Raphael
  * @version 19.07.2017
  */
object WebServer extends IOApp:

  // Create top level routes
  // Seems kinda broken in 1.0.0-M3, can't access /api/webrpc or /api/hardware so they are outside /api for now ...
  private def routes(config: AppConfig.Config,
                     webSocketBuilder: WebSocketBuilder[IO],
                     hostManager: BoincManager,
                     xmlProjectStore: XMLProjectStore,
                     db: Database,
                     jobManager: JobManager
  )(implicit T: Tracer[IO]) =
    val authService = new AuthenticationService(config)
    val hw =
      if config.hardware.isDefined && config.hardware.get.enabled then
        HardwareAPIRoutes(
          config.hardware.get.hosts.toSet,
          config.hardware
            .map(hardware =>
              new HWStatusService(
                hardware.binary,
                hardware.params,
                hardware.cacheTimeout,
                hardware.actions
              )
            )
            .get
        )
      else
        HttpRoutes.of[IO]:
          case GET -> Root => NotFound()

    Router(
      "/" -> GZip(WebResourcesRoute(config)),
      "/swagger" -> SwaggerRoutes(),
      "/api" -> authService.protectedService(BoincApiRoutes(hostManager, xmlProjectStore, db)),
      "/webrpc" -> authService.protectedService(WebRPCRoutes()), // <--- TODO: Document in Swagger
      "/hardware" -> authService.protectedService(hw), // <--- TODO: Document in Swagger
      "/ws" -> WebsocketRoutes(webSocketBuilder, authService, hostManager),
      "/auth" -> authService.authService,
      "/language" -> LanguageService(),
      "/jobs" -> JobManagerRoutes(db, jobManager)
    ).orNotFound.traced

  private def tracerResource: Resource[IO, Tracer[IO]] =
    Resource
      .eval(IO(GlobalOpenTelemetry.get))
      .evalMap(OtelJava.forAsync[IO])
      .evalMap(_.tracerProvider.get("at.happywetter.boinc.WebServer"))

  override def run(args: List[String]): IO[ExitCode] = tracerResource.use { implicit tracer: Tracer[IO] =>
    (for {
      config <- Resource.eval(for {
        logger <- Slf4jLogger.fromClass[IO](getClass)
        _ <- logger.info(s"Current Boinc-Webmanager version: ${BuildInfo.version}")
        config <- IO.blocking(AppConfig.conf)
        _ <- ConfigurationChecker.checkConfiguration(config, logger)
      } yield config)

      database <- Database().onFinalize(IO.println("DONE Database"))
      xmlPStore <- XMLProjectStore(database, config).onFinalize(IO.println("DONE XMLProjectStore"))
      hostManager <- BoincManager(config, database).onFinalize(IO.println("DONE BoincManager")) // <-- problematic
      jobManager <- JobManager(hostManager, database).onFinalize(IO.println("DONE JobManager"))

      // TODO: for Linux with systemd privileged socket can be inherited,
      //       how to convince Blaze to use it?
      webserver <- EmberServerBuilder
        .default[IO]
        .withShutdownTimeout(Duration.fromNanos(0))
        .withOptionalSSL(config)
        .withHostOption(Host.fromString(config.server.address))
        .withPort(Port.fromInt(config.server.port).get)
        .withHttpWebSocketApp(wsBuilder => routes(config, wsBuilder, hostManager, xmlPStore, database, jobManager))
        .build
        .onFinalize(IO.println("DONE EmberServerBuilder"))

      autoDiscovery <- BoincHostFinder(config, hostManager, database).onFinalize(IO.println("DONE BoincHostFinder"))
    } yield config)
      .use(config =>
        if config.serviceMode then serviceMode
        else interactive
      )
      .as(ExitCode.Success)
  }

  private val serviceMode = IO.println("Running in service mode, waiting for signal ...") *> IO.never
  private val interactive = for {
    _ <- IO.println("Press ENTER to exit the server ...")
    fiber <- Spawn[IO].start(IO.readLine)
    _ <- fiber.join
    _ <- IO.println("Shutting down server ...")
  } yield ()
