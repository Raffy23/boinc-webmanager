package at.happywetter.boinc.util

import at.happywetter.boinc.util.JobManager._
import cats.effect.kernel.Fiber
import cats.effect.std.Supervisor
import cats.effect.{IO, Ref, Resource}

import java.util.UUID

object JobManager {

  sealed trait Owner
  case class Server(cancelable: Boolean) extends Owner
  case class User(user: String) extends Owner

  sealed trait Status
  case object Running extends Status
  case object Stopped extends Status

  sealed case class Job(id: UUID, owner: Owner, status: Status, effect: IO[Unit], fiber: Fiber[IO, Throwable, Unit])

  def apply(): Resource[IO, JobManager] = for {
    supervisor <- Supervisor[IO]
    jobManager <- Resource.eval(
      for {
        jobRefs    <- Ref.of[IO, Map[UUID, Job]](Map.empty)
        jobManager <- IO.pure(new JobManager(supervisor, jobRefs))
      } yield jobManager
    )

  } yield jobManager

}

class JobManager(supervisor: Supervisor[IO], jobs: Ref[IO, Map[UUID, Job]]) {

  def add(owner: Owner, effect: IO[Unit]): IO[UUID] = for {
    uuid  <- IO.pure(UUID.randomUUID())
    fiber <- supervisor.supervise(effect)

    _ <- jobs.update(
      _ + (uuid -> Job(uuid, owner, Running, effect, fiber))
    )

  } yield uuid


  def status(id: UUID): IO[Status] =
    jobs
      .get
      .map(_(id).status)


  def remove(id: UUID): IO[Unit] = for {
    job <- jobs.get.map(_(id))

    _ <- jobs.update(_ - job.id)
    _ <- job.fiber.cancel

  } yield ()


  def stop(id: UUID): IO[Unit] = for {
    job <- jobs.get.map(_(id))

    _ <- jobs.update(_ + (job.id -> job.copy(status = Stopped)))
    _ <- job.fiber.cancel

  } yield ()


  def start(id: UUID): IO[Unit] = for {
    job   <- jobs.get.map(_(id))
    fiber <- supervisor.supervise(job.effect)

    _ <- jobs.update(_ + (job.id -> job.copy(status = Running, fiber = fiber)))

  } yield ()


}