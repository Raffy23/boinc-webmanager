package at.happywetter.boinc.util

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}

import cats.effect.{Clock, IO, Timer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


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

  implicit val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(
    cores,
    (r: Runnable) => {
      val t = new Thread(r)
      t.setName(s"io-timer-${r.hashCode()}")
      t.setDaemon(true)

      t
    })

  implicit val timer: Timer[IO] = new IOAppTimer(ExecutionContext.global, scheduler)

}
