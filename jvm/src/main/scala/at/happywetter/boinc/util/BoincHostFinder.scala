package at.happywetter.boinc.util

import java.net.InetAddress
import java.util.concurrent.Executors

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.BoincManager.AddedByDiscovery
import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.dto.DatabaseDTO.CoreClient
import at.happywetter.boinc.{AppConfig, BoincManager, Database}
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

  private val blocker = BoincHostFinder.createBlocker()
  private val autoDiscovery = new BoincDiscoveryService(config.autoDiscovery, discoveryCompleted, blocker)

  def beginSearch(): IO[Unit] =
    if (config.autoDiscovery.enabled) {
      LOG.info("Starting to search for boinc core clients ...")
      autoDiscovery.search().map(discoveryCompleted)
    } else {
      IO.unit
    }

  def close(): Unit =
    autoDiscovery.close()

  private def discoveryCompleted(hosts: List[IP]): Unit = {
    if (hosts.nonEmpty)
      LOG.info("Following hosts can be added: " + hosts.diff(getUsedIPs))
    else
      LOG.debug("No new hosts can be added")

    hosts.diff(getUsedIPs).foreach( ip => {
      Future {
        var found: Boolean = false

        config.autoDiscovery.password.map( password => {
          if (found)
            (false, Future{""})

          val boincCoreClient = new BoincClient(ip.toString, config.autoDiscovery.port, password, config.boinc.encoding)
          found = boincCoreClient.authenticate()

          val result =
            if (found) (found, boincCoreClient.getHostInfo.map(_.domainName), password)
            else (found, Future { "" }, "")

          boincCoreClient.close()
          result
        }).find{ case (succ, _, _) => succ }
          .foreach{ case (_, domainName, pw) =>
            domainName.foreach( domainName => {
              LOG.info(s"Found usable core client ($domainName) at $ip")

              boincManager.add(
                domainName,
                ip, config.autoDiscovery.port.toShort, pw,
                AddedByDiscovery
              )

              import monix.execution.Scheduler.Implicits.global
              db.clients
                .update(CoreClient(domainName, ip.toString, config.autoDiscovery.port, pw, CoreClient.ADDED_BY_DISCOVERY))
                .runSyncUnsafe()
            })
          }
      }
    })
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

  def createBlocker(): Blocker = {
    Blocker.liftExecutionContext(
      ExecutionContext.fromExecutor(
        Executors.newWorkStealingPool()
      )
    )
  }

  def apply(config: Config, boincManager: BoincManager, db: Database)(implicit contextShift: ContextShift[IO]): Resource[IO, BoincHostFinder] =
    Resource.fromAutoCloseable(
      IO.pure(new BoincHostFinder(config, boincManager, db)).flatMap { hostinder =>
        hostinder.beginSearch().map(_ => hostinder)
      }
    )

}