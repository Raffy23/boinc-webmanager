package at.happywetter.boinc.util

import at.happywetter.boinc.BoincManager
import at.happywetter.boinc.shared.rpc.jobs
import at.happywetter.boinc.shared.rpc.jobs.{At, BoincProjectAction, BoincRunModeAction, CPU, Every, GPU, Network, Once}
import cats.effect.IO

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.concurrent.duration._
import scala.language.postfixOps

object JobEffectUtil {

  def mkEffect(rpc: jobs.Job, manager: BoincManager): IO[IO[Unit]] = IO {

    val effect = rpc.action match {
      case BoincRunModeAction(host, target, mode, duration) =>
        manager
          .get(host)
          .semiflatMap(client => {
            target match {
              case CPU => client.setCpu(mode, duration)
              case GPU => client.setGpu(mode, duration)
              case Network => client.setNetwork(mode, duration)
            }
          })

      case BoincProjectAction(host, url, action) =>
        manager
          .get(host)
          .semiflatMap(client => {
            client.project(url, action)
          })
    }

    rpc.mode match {
      case Once          =>
        effect.value.as(())

      case At(timestamp) =>
        val now = LocalDateTime.now()

        if (timestamp.isBefore(now)) IO.unit
        else {
          IO
            .sleep(ChronoUnit.MILLIS.between(now, timestamp) millis)
            .flatMap(_ => effect.value)
            .foreverM
        }

      case Every(interval, until) =>
        val now = LocalDateTime.now()

        if (until.isDefined && until.get.isBefore(now)) IO.unit
        else {
          IO
            .sleep(interval)
            .flatMap(_ => effect.value)
            .whileM_(IO { until.forall(_.isBefore(LocalDateTime.now())) } )
        }

    }


  }


}
