package at.happywetter.boinc.util

import at.happywetter.boinc.{BoincManager, Database}
import at.happywetter.boinc.shared.rpc
import at.happywetter.boinc.shared.rpc.jobs.{Errored, JobStatus, Running, Stopped}
import at.happywetter.boinc.util.JobManager._
import cats.effect.kernel.Fiber
import cats.effect.std.Supervisor
import cats.effect.{IO, Ref, Resource}

import java.util.UUID

object JobManager {

  sealed trait Owner
  case class Server(cancelable: Boolean) extends Owner
  case class User(user: String) extends Owner

  sealed case class Job(id: UUID, owner: Owner, effect: IO[Unit], fiber: Fiber[IO, Throwable, Unit], dto: rpc.jobs.Job)

  def apply(manager: BoincManager, database: Database): Resource[IO, JobManager] = for {
    supervisor <- Supervisor[IO]
    jobManager <- Resource.eval(
      for {
        jobRefs    <- Ref.of[IO, Map[UUID, Job]](Map.empty)
        jobManager <- IO(new JobManager(supervisor, jobRefs, manager))
      } yield jobManager
    )

    // Load data in background from database
    _ <- database
      .jobs
      .queryAll()
      .flatMap { jobList =>
        import cats.implicits._

        jobList
          .map(job => jobManager.add(User("admin"), job))
          .sequence_
      }
      .background

  } yield jobManager

}

class JobManager(supervisor: Supervisor[IO], jobs: Ref[IO, Map[UUID, Job]], manager: BoincManager) {

  def add(owner: Owner, rpcJob: rpc.jobs.Job): IO[UUID] = for {
    uuid   <- IO { rpcJob.id.getOrElse(UUID.randomUUID()) }
    effect <- JobEffectUtil
      .mkEffect(rpcJob, manager)
      .map(effect =>
        effect
          .flatMap(_ =>
            jobs.update { jobs =>
              val job = jobs(uuid)
              jobs + (uuid -> job.copy(dto = job.dto.copy(status = Stopped)))
            }
          )
          .handleErrorWith( exception =>
            jobs.update { jobs =>
              val job = jobs(uuid)
              jobs + (uuid -> job.copy(dto = job.dto.copy(status = Errored(exception.getMessage))))
            }
          )
      )

    fiber  <- supervisor.supervise(effect)

    _ <- jobs.update(
      _ + (uuid -> Job(uuid, owner, effect, fiber, rpcJob.copy(status = Running)))
    )

  } yield uuid


  def status(id: UUID): IO[JobStatus] =
    jobs
      .get
      .map(_(id).dto.status)


  def remove(id: UUID): IO[Unit] = for {
    job <- jobs.get.map(_(id))

    _ <- jobs.update(_ - job.id)
    _ <- job.fiber.cancel

  } yield ()


  def stop(id: UUID): IO[Unit] = for {
    job <- jobs.get.map(_(id))

    _ <- jobs.update(_ + (job.id -> job.copy(dto = job.dto.copy(status = Stopped))))
    _ <- job.fiber.cancel

  } yield ()

  def delete(id: UUID): IO[Unit] = {
    jobs
      .get
      .map(_.contains(id))
      .ifM(
        stop(id) *>
        jobs.update(_ - id),
        IO.unit
      )
  }

  def start(id: UUID): IO[Unit] = for {
    job   <- jobs.get.map(_(id))
    fiber <- supervisor.supervise(job.effect)

    _ <- jobs.update(_ + (job.id -> job.copy(dto = job.dto.copy(status = Running), fiber = fiber)))

  } yield ()

  def all(): IO[List[Job]] =
    jobs.get.map(_.values.toList)

}
