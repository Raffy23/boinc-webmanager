package at.happywetter.boinc.util

import java.net.InetAddress
import java.util.concurrent.ScheduledExecutorService

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.{AppConfig, BoincManager}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.09.2017
  */
class BoincHostSettingsResolver(config: Config, boincManager: BoincManager)(implicit val scheduler: ScheduledExecutorService) {

  private val logger = LoggerFactory.getILoggerFactory.getLogger(this.getClass.getCanonicalName)
  private val autoDiscovery = new BoincDiscoveryService(config.autoDiscovery, discoveryCompleted)

  def beginSearch(): Unit =
    if (config.autoDiscovery.enabled)
      discoveryCompleted( autoDiscovery.search )

  private def discoveryCompleted(data: Future[List[IP]]): Unit = {
    data.foreach{ hosts =>
      logger.debug("Completed Discovery: " + hosts)
      logger.debug("Following Hosts can be added: " + hosts.diff(getUsedIPs))

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
                logger.debug(s"Found usable Core Client at $ip")

                boincManager.add(
                  domainName,
                  AppConfig.Host(ip.toString, config.autoDiscovery.port.toShort, pw)
                )
              })
            }
        }
      })
    }
  }


  private def getUsedIPs: List[IP] = boincManager
    .getAddresses.filter(_._2 == config.autoDiscovery.port)
    .map{ case (addr, _ ) =>
      val ip = IP(addr)

      if (ip == IP.empty) IP(InetAddress.getByName(addr).getHostAddress)
      else ip
    }

}
