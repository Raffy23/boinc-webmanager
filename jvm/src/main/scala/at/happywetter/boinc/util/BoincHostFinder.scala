package at.happywetter.boinc.util

import java.net.InetAddress

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.BoincManager.AddedByDiscovery
import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.dto.DatabaseDTO.CoreClient
import at.happywetter.boinc.{BoincManager, Database}

import cats.data.OptionT
import cats.effect.{IO, Resource}
import com.comcast.ip4s.Host
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.otel4s.trace.Tracer

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.09.2017
  */
class BoincHostFinder private (config: Config,
                               boincManager: BoincManager,
                               db: Database,
                               logger: SelfAwareStructuredLogger[IO]
)(implicit T: Tracer[IO]):

  case class FoundCoreClient(name: String, password: String)

  private def findCorrectPassword(ip: Host, passwords: List[String]): OptionT[IO, FoundCoreClient] =
    passwords match
      case password :: xs =>
        OptionT(
          BoincClient
            .tryConnect(ip.toString, config.autoDiscovery.port, password)
            .use {
              case Some(coreClient) =>
                coreClient.getHostInfo.map(name => Some(FoundCoreClient(name.domainName, password)))
              case None => IO.pure(Option.empty)
            }
        ).orElse(findCorrectPassword(ip, xs))

      case Nil =>
        OptionT.none

  private def discoveryCompleted(hosts: Seq[Host]): IO[Unit] =
    getUsedHosts
      .flatMap { usedIPs =>
        import cats.implicits._

        (
          if hosts.nonEmpty then logger.info("Following hosts can be added: " + hosts.diff(usedIPs))
          else logger.info("No new hosts can be added")
        ) *>
          hosts
            .diff(usedIPs)
            .map(ip =>
              findCorrectPassword(ip, config.autoDiscovery.password)
                .semiflatMap { case FoundCoreClient(domainName, password) =>
                  for {

                    _ <- logger.info(s"Found usable core client ($domainName) at $ip")
                    _ <- boincManager.add(
                      domainName,
                      ip.toString,
                      config.autoDiscovery.port.toShort,
                      password,
                      AddedByDiscovery
                    )

                    _ <- db.clients.update(
                      CoreClient(domainName,
                                 ip.toString,
                                 config.autoDiscovery.port,
                                 password,
                                 CoreClient.ADDED_BY_DISCOVERY
                      )
                    )
                  } yield ()

                }
                .value
                .handleErrorWith(_ => logger.info(s"Could not add core client ($ip), client timed out"))
            )
            .parSequence_
      }
      .as(())

  private def getUsedHosts: IO[List[Host]] =
    boincManager.getAddresses.map(
      _.filter(_._2 == config.autoDiscovery.port)
        .map { case (addr, _) => Host.fromString(addr).get }
    )

object BoincHostFinder {

  def apply(config: Config, boincManager: BoincManager, db: Database)(implicit
    T: Tracer[IO]
  ): Resource[IO, BoincHostFinder] = for {
    hostFinder <- Resource.eval(
      for {
        logger <- Slf4jLogger.fromClass[IO](BoincHostFinder.getClass)
        hostFinder <- IO { new BoincHostFinder(config, boincManager, db, logger) }
      } yield hostFinder
    )

    autoDiscovery <- BoincDiscoveryService(config.autoDiscovery, boincManager, hostFinder.discoveryCompleted)
    // _             <- hostFinder.beginSearch(autoDiscovery).background
  } yield hostFinder

}
