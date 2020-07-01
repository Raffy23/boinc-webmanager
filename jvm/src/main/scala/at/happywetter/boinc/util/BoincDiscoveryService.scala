package at.happywetter.boinc.util

import java.net.{InetSocketAddress, Socket}
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ScheduledFuture, TimeUnit}

import at.happywetter.boinc.AppConfig.AutoDiscovery
import cats.effect.{Blocker, ContextShift, IO}

import scala.util.Try

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.09.2017
  */
class BoincDiscoveryService(config: AutoDiscovery, autoScanCallback: List[IP] => Unit, blocker: Blocker)(implicit contextShift: ContextShift[IO]) {

  private val start = IP(config.startIp)
  private val end   = IP(config.endIp)

  val excluded = new AtomicReference[List[IP]](List.empty)
  private val scheduler = IOAppTimer.scheduler


  val task: Option[ScheduledFuture[_]] =
    if (config.enabled) Some(scheduler.scheduleWithFixedDelay(() => autoScanCallback( search().unsafeRunSync() ), config.scanTimeout, config.scanTimeout, TimeUnit.MINUTES))
    else None

  def destroy(): Unit = task.map(_.cancel(true))

  def search(): IO[List[IP]] = {
    import cats.instances.list._
    import cats.syntax.parallel._

    probeRange(config.port)
      .parSequence
      .map(_.filter { case (ip, found) => excludeIP(ip, found) }.map(_._1) )
  }

  private def excludeIP(ip: IP, found: Boolean): Boolean = {
    if (found)
      excluded.set(ip :: excluded.get())

    found
  }

  private def probeRange(port: Int): List[IO[(IP, Boolean)]] = (start to end)
    .filterNot(excluded.get().contains)
    .map(ip => { probeSocket(ip, port) })
    .toList

  private def probeSocket(ip: IP, port: Int): IO[(IP, Boolean)] = contextShift.blockOn(blocker) {
    IO.async { resolve =>
      resolve.apply(
        Try {
          val socket = new Socket()
          socket.connect(new InetSocketAddress(ip.toInetAddress, port), config.timeout)
          socket.close()

          (ip, true)
        }.recover(_ => (ip, false)).toEither
      )
    }
  }

}
