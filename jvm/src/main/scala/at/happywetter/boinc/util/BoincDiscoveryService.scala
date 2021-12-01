package at.happywetter.boinc.util

import java.net.{InetSocketAddress, Socket}
import scala.concurrent.duration._
import at.happywetter.boinc.AppConfig.AutoDiscovery
import at.happywetter.boinc.BoincManager
import cats.effect.std.Semaphore
import cats.effect.{IO, Ref, Resource}
import com.comcast.ip4s.{Host, IpAddress, SocketAddress}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.comcast.ip4s._
import fs2.io.net.Network

import scala.language.postfixOps

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.09.2017
  */
class BoincDiscoveryService private (config: AutoDiscovery, excludedHosts: Ref[IO, Set[Host]], autoScanCallback: Seq[Host] => IO[Unit], lock: Semaphore[IO], logger: SelfAwareStructuredLogger[IO]) {

  // TODO: check these as they are user input:
  private val start = IpAddress.fromString(config.startIp).get
  private val end   = IpAddress.fromString(config.endIp).get

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

  def search(): IO[Seq[Host]] = {
    logger.info(s"Start probing range ${start} - ${end}") *>
    probeRange(config.port)
      .map(_.filter { case (_, found) => found }.map(_._1) )
  }

  private def probeRange(port: Int): IO[Seq[(Host, Boolean)]] = {
    import BoincDiscoveryService.IpAddressToAble
    import cats.syntax.parallel._

    for {
      excluded <- excludedHosts.get
      result   <- (start to end)
        .filterNot(excluded.contains)
        .map(ip =>
          // Make sure to rate limit the probing to max of N requests
          // some firewalls might not light too many requests ...
          lock
            .permit
            .use(_ => probeSocket(ip, port))
        )
        .toSeq
        .parSequence
    } yield result
  }

  private def probeSocket(host: Host, port: Int): IO[(Host, Boolean)] = for {
    _ <- logger.debug(s"Start scanning ${host.toString}:$port")

    socket  <- Network[IO]
      .client(SocketAddress(host, Port.fromInt(port).get))
      .use(_ => IO.pure(true))
      .flatTap(_ => logger.debug(s"Can connect to ${host.toString}:$port"))
      .timeout(config.timeout milliseconds)
      .handleError(_ => false)

  } yield (host, socket)

}

object BoincDiscoveryService {

  def apply(config: AutoDiscovery, boincManager: BoincManager, autoScanCallback: Seq[Host] => IO[Unit]): Resource[IO, BoincDiscoveryService] =
    for {
      service <- Resource.eval(
        for {
          ratelimit <- Semaphore[IO](config.maxScanRequests)
          logger    <- Slf4jLogger.fromClass[IO](getClass)

          initialExcluded <- boincManager.getAddresses.map(_.map(address => Host.fromString(address._1).get).toSet)
          excluded        <- Ref.of[IO, Set[Host]](initialExcluded)
          _               <- logger.debug(s"Excluding $initialExcluded from auto discovery!")

          // Update excluded list for everytime the boinc manager
          // changes, now added hosts (manually or automatically are not scanned anymore)
          _ <- boincManager.changeListener.register(mgr => for {
            addresses <- mgr.getAddresses.map(_.map(address => Host.fromString(address._1).get).toSet)
            _         <- excluded.update(_ => addresses)
          } yield ())

          service   <- IO(new BoincDiscoveryService(config, excluded, autoScanCallback, ratelimit, logger))
        } yield service
      )

      // Don't forget to start a background task for scanning Hosts
      _       <- service.task.background
    } yield service

  private implicit class IpAddressToAble(private val ip: IpAddress) extends AnyVal {
    def to(end: IpAddress): Iterable[IpAddress] = Iterable.unfold(ip) { current =>
      val next = current.next
      if (next.equals(end)) None
      else                  Some((next, next))
    }
  }

}
