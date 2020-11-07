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
class BoincDiscoveryService(config: AutoDiscovery, autoScanCallback: List[IP] => Unit, blocker: Blocker)(implicit contextShift: ContextShift[IO]) extends Logger with AutoCloseable {

  private val start = IP(config.startIp)
  private val end   = IP(config.endIp)

  val excluded = new AtomicReference[Set[IP]](Set.empty)
  private val scheduler = IOAppTimer.scheduler

  private val task: Option[ScheduledFuture[_]] =
    if (config.enabled) Some(
      scheduler.scheduleWithFixedDelay(() => autoScanCallback( search().unsafeRunSync() ),
        config.scanTimeout,
        config.scanTimeout, TimeUnit.MINUTES)
    )
    else None

  def search(): IO[List[IP]] = {
    LOG.info(s"Start probing range ${start} - ${end}")

    // Can't blockOn io-compute-X
    contextShift.blockOn(blocker)(probeRange(config.port))
      .map(_.filter { case (ip, found) => excludeIP(ip, found) }.map(_._1) )
  }

  private def excludeIP(ip: IP, found: Boolean): Boolean = {
    if (found)
      excluded.set(excluded.get() + ip)

    found
  }

  private def probeRange(port: Int): IO[List[(IP, Boolean)]] =  {
    import cats.instances.list._
    import cats.syntax.parallel._

    (start to end)
      .filterNot(excluded.get().contains)
      .map(ip => { probeSocket(ip, port) })
      .toList
      .parSequence
  }

  private def probeSocket(ip: IP, port: Int): IO[(IP, Boolean)] = contextShift.blockOn(blocker) {
    IO.async { resolve =>
      LOG.debug(s"Start scanning ${ip}:${port}")

      resolve.apply(
        Try {
          val socket = new Socket()
          socket.connect(new InetSocketAddress(ip.toInetAddress, port), config.timeout)
          socket.close()

          LOG.debug(s"Can connect to ${ip}:${port}")
          (ip, true)
        }.recover(_ => (ip, false)).toEither
      )
    }
  }

  override def close(): Unit = task.map(_.cancel(true))

}
