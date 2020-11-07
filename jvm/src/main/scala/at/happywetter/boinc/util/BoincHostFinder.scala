package at.happywetter.boinc.util

import java.net.InetAddress
import java.util.concurrent.Executors

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.BoincManager.AddedByDiscovery
import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.dto.DatabaseDTO.CoreClient
import at.happywetter.boinc.{AppConfig, BoincManager, Database}
import cats.data.{EitherT, OptionT}
import cats.effect.{Blocker, ContextShift, IO, Resource}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.09.2017
  */
class BoincHostFinder(config: Config, boincManager: BoincManager, db: Database)(implicit contextShift: ContextShift[IO]) extends Logger with AutoCloseable {

  private val blocker = IOAppTimer.createMaxParallelismBlocker("io-autoDiscovery")
  private val autoDiscovery = new BoincDiscoveryService(config.autoDiscovery, discoveryCompleted, blocker)

  def beginSearch(): IO[Unit] =
    if (config.autoDiscovery.enabled) {
      LOG.info("Starting to search for boinc core clients ...")
      autoDiscovery.search().flatMap(discoveryCompleted)
    } else {
      IO.unit
    }

  def close(): Unit =
    autoDiscovery.close()

  private def discoveryCompleted(hosts: List[IP]): IO[Unit] = {
    if (hosts.nonEmpty) LOG.info("Following hosts can be added: " + hosts.diff(getUsedIPs))
    else LOG.info("No new hosts can be added")

    import cats.implicits._
    case class FoundCoreClient(name: String, password: String)

    hosts.diff(getUsedIPs).map(ip =>
      config.autoDiscovery.password.map(password =>
        OptionT(
          Resource.fromAutoCloseableBlocking(blocker)(IO {
            new BoincClient(ip.toString, config.autoDiscovery.port, password, config.boinc.encoding)(implicitly, blocker)
          }).use { boincCoreClient =>
            boincCoreClient.authenticate().flatMap { auth =>
              if (auth) boincCoreClient.getHostInfo.map(_.domainName).map(name => Some(FoundCoreClient(name, password)))
              else      IO.pure(Option.empty)
            }
          }
        )
      ).findM(_.isDefined).flatMap {
        case None    => IO { LOG.info(s"Didn't find any usable password for $ip") } /*Core client isn't usable ...*/
        case Some(a) =>
          a.semiflatMap { case FoundCoreClient(domainName, password) =>
            LOG.info(s"Found usable core client ($domainName) at $ip")

            boincManager.add(
              domainName,
              ip.toString, config.autoDiscovery.port.toShort, password,
              AddedByDiscovery
            )

            contextShift.blockOn(blocker)(IO {
              import monix.execution.Scheduler.Implicits.global
              db.clients
                .update(CoreClient(domainName, ip.toString, config.autoDiscovery.port, password, CoreClient.ADDED_BY_DISCOVERY))
                .runSyncUnsafe()
            })
          }.value
      }
    ).parSequence.map(_ => ())
  }

  private def getUsedIPs: List[IP] = boincManager
    .getAddresses.filter(_._2 == config.autoDiscovery.port)
    .map{ case (addr, _ ) =>
      val ip = IP(addr)

      if (ip == IP.empty) IP(InetAddress.getByName(addr).getHostAddress)
      else ip
    }

}
object BoincHostFinder {

  def apply(config: Config, boincManager: BoincManager, db: Database)(implicit contextShift: ContextShift[IO]): Resource[IO, BoincHostFinder] =
    Resource.fromAutoCloseable(
      IO { new BoincHostFinder(config, boincManager, db) }.map { hostFinder =>
        hostFinder.beginSearch().unsafeRunAsyncAndForget()
        hostFinder
      }
    )

}