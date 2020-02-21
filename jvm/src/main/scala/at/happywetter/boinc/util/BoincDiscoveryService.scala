package at.happywetter.boinc.util

import java.net.{InetSocketAddress, Socket}
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture, TimeUnit}

import at.happywetter.boinc.AppConfig.AutoDiscovery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.09.2017
  */
class BoincDiscoveryService(config: AutoDiscovery, autoScanCallback: Future[List[IP]] => Unit)(implicit val scheduler: ScheduledExecutorService) {

  private val start = IP(config.startIp)
  private val end   = IP(config.endIp)

  val excluded = new AtomicReference[List[IP]](List.empty)
  val task: Option[ScheduledFuture[_]] =
    if (config.enabled) Some(scheduler.scheduleWithFixedDelay(() => autoScanCallback( search() ), config.scanTimeout, config.scanTimeout, TimeUnit.MINUTES))
    else None

  def destroy(): Unit = task.map(_.cancel(true))

  def search(): Future[List[IP]] =
    Future
      .sequence( probeRange(config.port) )
      .map(_.filter { case (ip, found) => excludeIP(ip, found) }.map(_._1) )

  private def excludeIP(ip: IP, found: Boolean): Boolean = {
    if (found)
      excluded.set(ip :: excluded.get())

    found
  }

  private def probeRange(port: Int) = (start to end)
    .filterNot(excluded.get().contains)
    .map(ip => { probeSocket(ip, port) })
    .toList

  private def probeSocket(ip: IP, port: Int) = Future {
    val socket = new Socket()
    socket.connect(new InetSocketAddress(ip.toInetAddress, port), config.timeout)
    socket.close()

    (ip, true)
  }.recover {
    case _: Exception => (ip, false)
  }

}
