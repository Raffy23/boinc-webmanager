package at.happywetter.boinc.util

import java.util.concurrent.{Executors, LinkedBlockingQueue, ScheduledExecutorService, ThreadPoolExecutor, TimeUnit}

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

  implicit val blocker: Blocker = createBlocker("io-blocker")

  def createMaxParallelismBlocker(prefix: String, nThreads: Int = cores): Blocker = Blocker.liftExecutionContext(
    ExecutionContext.fromExecutorService(
      new ThreadPoolExecutor(
        nThreads, nThreads,
        5L, TimeUnit.SECONDS,
        new LinkedBlockingQueue[Runnable](),
        new DaemonThreadFactory(prefix)
      )
    )
  )

  def createBlocker(prefix: String): Blocker = Blocker.liftExecutionContext(
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool(new DaemonThreadFactory(prefix)))
  )

}
