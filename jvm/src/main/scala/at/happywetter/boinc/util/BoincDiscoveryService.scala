package at.happywetter.boinc.util

import java.net.{InetSocketAddress, Socket}
import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import at.happywetter.boinc.AppConfig.AutoDiscovery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.09.2017
  */
class BoincDiscoveryService(config: AutoDiscovery, autoScanCallback: (Future[List[IP]]) => Unit)(implicit val scheduler: ScheduledExecutorService) {

  private val start = IP(config.startIp)
  private val end   = IP(config.endIp)

  if (config.enabled)
    scheduler.scheduleWithFixedDelay(() => autoScanCallback( search ), config.scanTimeout, config.scanTimeout, TimeUnit.MINUTES)

  def search: Future[List[IP]] =
    Future
      .sequence( propeRange(config.port) )
      .map(_.filter { case (_, found) => found })
      .map(_.map { case (ip, _) => ip })

  private def propeRange(port: Int) = (start to end).map(ip => { propeSocket(ip, port) }).toList

  private def propeSocket(ip: IP, port: Int) = Future {
    val socket = new Socket()
    socket.connect(new InetSocketAddress(ip.toString, port), config.timeout)
    socket.close()

    (ip, true)
  }.recover {
    case _: Exception => (ip, false)
  }

}
