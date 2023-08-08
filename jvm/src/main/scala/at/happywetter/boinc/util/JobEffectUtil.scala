package at.happywetter.boinc.util

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import scala.concurrent.duration._
import scala.language.postfixOps

import at.happywetter.boinc.BoincManager
import at.happywetter.boinc.shared.rpc.jobs
import at.happywetter.boinc.shared.rpc.jobs.{At, BoincProjectAction, BoincRunModeAction, CPU, Every, GPU, Network, Once}

import cats.effect.IO

object JobEffectUtil:
  import cats.implicits._

  def mkEffect(rpc: jobs.Job, manager: BoincManager): IO[IO[Unit]] = IO:

    val effect = rpc.action match
      case BoincRunModeAction(hosts, target, mode, duration) =>
        hosts
          .map(host =>
            manager
              .get(host)
              .semiflatMap(client => {
                target match {
                  case CPU     => client.setCpu(mode, duration)
                  case GPU     => client.setGpu(mode, duration)
                  case Network => client.setNetwork(mode, duration)
                }
              })
              .value
          )
          .sequence_

      case BoincProjectAction(hosts, url, action) =>
        hosts
          .map(host =>
            manager
              .get(host)
              .semiflatMap(client => {
                IO.println(s"${client.details.address}, ${url}, ${action.toString}") *>
                  client.project(url, action).flatMap(r => IO.println("result: " + r))
              })
              .value
          )
          .sequence_

    rpc.mode match
      case Once =>
        effect

      case At(timestamp) =>
        val now = LocalDateTime.now()

        if timestamp.isBefore(now) then IO.unit
        else
          IO
            .sleep(ChronoUnit.MILLIS.between(now, timestamp) millis)
            .flatMap(_ => effect)

      case Every(interval, until) =>
        val now = LocalDateTime.now()

        if until.isDefined && until.get.isBefore(now) then IO.unit
        else if until.isDefined then
          IO
            .sleep(interval)
            .flatMap(_ => IO.println("after sleep: " + interval))
            .flatMap(_ => effect *> IO.println("after effect"))
            .whileM_(IO {
              val x = until.forall(_.isAfter(LocalDateTime.now()))
              println((until, LocalDateTime.now(), until.get.isAfter(LocalDateTime.now()), x))
              x
            }) *> IO
            .println("after every")
            .handleError(_.printStackTrace()) *> IO.println("DONE")
        else
          IO
            .sleep(interval)
            .flatMap(_ => effect)
            .foreverM
