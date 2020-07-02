package at.happywetter.boinc.util

import java.net.InetAddress
import java.util.concurrent.{Executors, ScheduledExecutorService}

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.{AppConfig, BoincManager}
import cats.effect.{Blocker, ContextShift, IO, Resource}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.09.2017
  */
class BoincHostFinder(config: Config, boincManager: BoincManager)(implicit contextShift: ContextShift[IO]) extends Logger {

  private val blocker = BoincHostFinder.createBlocker()
  private val autoDiscovery = new BoincDiscoveryService(config.autoDiscovery, discoveryCompleted, blocker)

  def beginSearch(): Resource[IO, IO[Unit]] =
    if (config.autoDiscovery.enabled) {
      LOG.info("Starting to search for boinc core clients ...")
      autoDiscovery.search().map(discoveryCompleted).background
    } else {
      Resource.pure[IO, IO[Unit]](IO.unit)
    }

  def stopSearch(): Unit =
    autoDiscovery.destroy()

  private def discoveryCompleted(hosts: List[IP]): Unit = {
    LOG.info("Following Hosts can be added: " + hosts.diff(getUsedIPs))

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
                AppConfig.Host(ip.toString, config.autoDiscovery.port.toShort, pw)
              )
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

}