package at.happywetter.boinc.util

import java.net.{InetSocketAddress, Socket}
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration._
import at.happywetter.boinc.AppConfig.AutoDiscovery
import cats.effect.std.Semaphore
import cats.effect.{IO, Resource}

import scala.util.Try
import scala.language.postfixOps

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.09.2017
  */
class BoincDiscoveryService private (config: AutoDiscovery, autoScanCallback: List[IP] => IO[Unit], lock: Semaphore[IO]) extends Logger {

  private val start = IP(config.startIp)
  private val end   = IP(config.endIp)

  val excluded = new AtomicReference[Set[IP]](Set.empty)

  private val task =
    IO
      .pure(config.enabled)
      .ifM(
          search()
            .flatMap(autoScanCallback)
            .flatMap(_ => IO.sleep(config.scanTimeout minutes))
            .foreverM,
        IO.unit
      )

  def search(): IO[List[IP]] = {
    IO { LOG.info(s"Start probing range ${start} - ${end}") } *>
    probeRange(config.port)
      .map(_.filter { case (ip, found) => excludeIP(ip, found) }.map(_._1) )
  }

  private def excludeIP(ip: IP, found: Boolean): Boolean = {
    if (found)
      excluded.set(excluded.get() + ip)

    found
  }

  private def probeRange(port: Int): IO[List[(IP, Boolean)]] = {
    import cats.instances.list._
    import cats.syntax.parallel._

    (start to end)
      .filterNot(excluded.get().contains)
      .map(ip => for {
        // Make sure to rate limit the probing to max of N requests
        // some firewalls might not light too many requests ...
        _      <- lock.acquire
        result <- probeSocket(ip, port)
        _      <- lock.release
      } yield result)
      .toList
      .parSequence
  }

  private def probeSocket(ip: IP, port: Int): IO[(IP, Boolean)] = IO.blocking {
      LOG.debug(s"Start scanning ${ip}:${port}")

      Try {
        val socket = new Socket()
        socket.connect(new InetSocketAddress(ip.toInetAddress, port), config.timeout)
        socket.close()

        LOG.debug(s"Can connect to ${ip}:${port}")
        (ip, true)
      }.getOrElse((ip, false))
  }

}

object BoincDiscoveryService {

  def apply(config: AutoDiscovery, autoScanCallback: List[IP] => IO[Unit]): Resource[IO, BoincDiscoveryService] = (
    for {
      lock    <- Resource.eval(Semaphore[IO](config.maxScanRequests))
      service <- Resource.pure(new BoincDiscoveryService(config, autoScanCallback, lock))
      _       <- service.task.background

    } yield service
  ).onFinalize(IO.println("onFinalize"))

}
