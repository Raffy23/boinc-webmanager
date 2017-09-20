package at.happywetter.boinc.util

import java.net.{Inet4Address, InetAddress}
import java.util.concurrent.ScheduledExecutorService

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.{AppConfig, BoincManager}
import at.happywetter.boinc.boincclient.BoincClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.09.2017
  */
class BoincHostSettingsResolver(config: Config, boincManager: BoincManager)(implicit val scheduler: ScheduledExecutorService) {

  private val autoDiscovery = new BoincDiscoveryService(config.autoDiscovery, discoveryCompleted)

  def beginSearch(): Unit =
    if (config.autoDiscovery.enabled)
      discoveryCompleted( autoDiscovery.search )

  private def discoveryCompleted(data: Future[List[IP]]): Unit = data.foreach{ hosts =>
    hosts.diff(getUsedIPs).foreach( ip => {
      Future {

        config.autoDiscovery.password.map( password => {
          val boincCoreClient = new BoincClient(ip.toString, config.autoDiscovery.port, password, config.boinc.encoding)
          val succ = boincCoreClient.authenticate()

          val result =
            if (succ) (succ, boincCoreClient.getHostInfo.map(_.domainName), password)
            else (succ, Future { "" }, "")

          boincCoreClient.close()
          result
        }).find{ case (succ, _, _) => succ }
          .foreach{ case (_, domainName, pw) =>
            domainName.foreach( domainName =>
              boincManager.add(
                domainName,
                AppConfig.Host(ip.toString, config.autoDiscovery.port.toShort, pw)
              )
            )
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
