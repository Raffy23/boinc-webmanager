package at.happywetter.boinc.util

import java.util.concurrent.{Executors, ScheduledExecutorService}

import cats.effect.{Blocker, IO, Timer}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object IOAppTimer {

  implicit val cores: Int = Runtime.getRuntime.availableProcessors()

  implicit val defaultExecutionContext: ExecutionContextExecutor = ExecutionContext.global

  implicit val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(cores, new DaemonThreadFactory("io-scheduler"))

  implicit val timer: Timer[IO] = IO.timer(
    ExecutionContext.fromExecutor(Executors.newWorkStealingPool(cores)),
    scheduler
  )

  implicit val blocker: Blocker = Blocker.liftExecutionContext(
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool(new DaemonThreadFactory("io-blocker")))
  )

}
