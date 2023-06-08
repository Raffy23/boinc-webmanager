package at.happywetter.boinc.util

import java.io.File
import at.happywetter.boinc.AppConfig.Config
import cats.effect.IO
import org.typelevel.log4cats.SelfAwareStructuredLogger

/**
  * Created by: 
  *
  * @author Raphael
  * @version 03.11.2017
  */
object ConfigurationChecker:

  private def check(condition: Boolean, message: String, logger: SelfAwareStructuredLogger[IO]): IO[Unit] =
    IO(condition).ifM(logger.error(message) *> IO.raiseError(new RuntimeException(message)), IO.unit)

  def checkConfiguration(config: Config, logger: SelfAwareStructuredLogger[IO]): IO[Unit] = for {
    _ <- logger.trace("Checking environment ...")
    _ <- IO(config.development.getOrElse(false)).ifM(
      logger.info("WebServer was launched with development options!") *>
        logger.info("All resources will be served from: " + config.server.webroot),
      IO.unit
    )

    // Check environment
    _ <- check(
      !new File("./application.conf").exists(),
      "application.conf does not exist!",
      logger
    )

    // Check if XML-Source is available
    _ <- check(
      !new File(config.boinc.projects.xmlSource).exists(),
      "Unable to read Boinc XML Project definition!",
      logger
    )

    _ <- check(
      config.development.getOrElse(false) && !new File(config.server.webroot).exists(),
      "Webroot is not a valid Directory!",
      logger
    )

    _ <- check(
      config.hardware.isDefined && config.hardware.get.enabled && !new File(config.hardware.get.binary).exists(),
      "Binary for Hardware Data fetching is missing!",
      logger
    )
  } yield ()
