package at.happywetter.boinc.util

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.BoincManager.AddedByDiscovery
import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.dto.DatabaseDTO.CoreClient
import at.happywetter.boinc.{BoincManager, Database}
import cats.data.OptionT
import cats.effect.{IO, Resource}

import java.net.InetAddress


/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.09.2017
  */
class BoincHostFinder private (config: Config, boincManager: BoincManager, db: Database) extends Logger {

  /*
  private def beginSearch(autoDiscovery: BoincDiscoveryService): IO[Unit] =
    if (config.autoDiscovery.enabled) {
      LOG.info("Starting to search for boinc core clients ...")
      autoDiscovery.search().flatMap(discoveryCompleted)
    } else {
      IO.unit
    }
  */

  case class FoundCoreClient(name: String, password: String)

  private def findCorrectPassword(ip: IP, passwords: List[String]): OptionT[IO, FoundCoreClient] = {
    passwords match {
      case password :: xs =>
        OptionT(
          BoincClient
            .tryConnect(ip.toString, config.autoDiscovery.port, password)
            .use {
              case Some(coreClient) => coreClient.getHostInfo.map(name => Some(FoundCoreClient(name.domainName, password)))
              case None             => IO.pure(Option.empty)
            }
        ).orElse(findCorrectPassword(ip, xs))

      case Nil            =>
        OptionT.none
    }
  }

  private def discoveryCompleted(hosts: List[IP]): IO[Unit] = {
    getUsedIPs.flatMap { usedIPs =>
      if (hosts.nonEmpty) LOG.info("Following hosts can be added: " + hosts.diff(usedIPs))
      else LOG.info("No new hosts can be added")

      import cats.implicits._
      hosts.diff(usedIPs).map(ip =>
        findCorrectPassword(ip, config.autoDiscovery.password).semiflatMap {
          case FoundCoreClient(domainName, password) =>
            LOG.info(s"Found usable core client ($domainName) at $ip")

            for {

              _ <- boincManager.add(
                domainName,
                ip.toString, config.autoDiscovery.port.toShort, password,
                AddedByDiscovery
              )

              _ <- db.clients.update(
                CoreClient(domainName, ip.toString, config.autoDiscovery.port, password, CoreClient.ADDED_BY_DISCOVERY)
              )

            } yield ()

        }
      ).parSequence
       .value
    }.as(())
  }

  private def getUsedIPs: IO[List[IP]] =
    boincManager.getAddresses.map(_
      .filter(_._2 == config.autoDiscovery.port)
      .map{ case (addr, _ ) =>
        val ip = IP(addr)

        if (ip == IP.empty) IP(InetAddress.getByName(addr).getHostAddress)
        else ip
      }
    )

}
object BoincHostFinder {

  def apply(config: Config, boincManager: BoincManager, db: Database): Resource[IO, BoincHostFinder] = for {
    hostFinder    <- Resource.pure(new BoincHostFinder(config, boincManager, db))
    autoDiscovery <- BoincDiscoveryService(config.autoDiscovery, hostFinder.discoveryCompleted)
    // _             <- hostFinder.beginSearch(autoDiscovery).background
  } yield hostFinder

}