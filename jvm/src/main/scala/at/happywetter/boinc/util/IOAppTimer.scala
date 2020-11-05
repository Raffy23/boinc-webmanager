package at.happywetter.boinc.util

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}

import cats.effect.{Blocker, Clock, IO, Resource, Sync, Timer}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.language.higherKinds


/**
  * https://typelevel.org/cats-effect/datatypes/timer.html
  */
final class IOAppTimer(ec: ExecutionContext, sc: ScheduledExecutorService) extends Timer[IO] {

  override val clock: Clock[IO] =
    new Clock[IO] {
      override def realTime(unit: TimeUnit): IO[Long] =
        IO(unit.convert(System.currentTimeMillis(), MILLISECONDS))

      override def monotonic(unit: TimeUnit): IO[Long] =
        IO(unit.convert(System.nanoTime(), NANOSECONDS))
    }

  override def sleep(timespan: FiniteDuration): IO[Unit] =
    IO.cancelable { cb =>
      val tick = new Runnable {
        def run(): Unit = ec.execute(() => cb(Right(())))
      }

      val f = sc.schedule(tick, timespan.length, timespan.unit)
      IO(f.cancel(false))
    }

}

object IOAppTimer {

  implicit val cores: Int = Runtime.getRuntime.availableProcessors()

  implicit val defaultExecutionContext: ExecutionContextExecutor = ExecutionContext.global

  implicit val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(cores, new DaemonThreadFactory("io-timer"))

  implicit val timer: Timer[IO] = new IOAppTimer(ExecutionContext.global, scheduler)

  implicit val blocker: Blocker = Blocker.liftExecutionContext(
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool(new DaemonThreadFactory("blocker")))
  )

}
