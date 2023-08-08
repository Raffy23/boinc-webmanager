package at.happywetter.boinc.repository

import java.util.UUID

import at.happywetter.boinc.dto.JobDTO.{Job, JobConverter}
import at.happywetter.boinc.shared.rpc.jobs.{Job => RpcJob}

import cats.effect.IO
import doobie.Transactor
import doobie.h2.implicits._
import doobie.implicits._

class JobRepository(xa: Transactor[IO]):

  def insert(job: RpcJob): IO[RpcJob] = IO.blocking:
    val dbJob = job.toDB

    sql"""INSERT INTO job (uuid, contents) VALUES (${dbJob.uuid}, ${dbJob.contents})""".update.run
      .transact(xa)

    job.copy(id = Some(dbJob.uuid))

  def queryAll(): IO[List[RpcJob]] =
    sql"""SELECT * FROM job"""
      .query[Job]
      .to[List]
      .transact(xa)
      .map(_.map(db => db.toRPC))

  def exists(id: UUID): IO[Boolean] =
    sql"""SELECT COUNT(*) FROM job WHERE uuid = $id"""
      .query[Int]
      .unique
      .transact(xa)
      .map(_ == 1)

  def update(job: RpcJob): IO[RpcJob] =
    job.id match
      case None => insert(job)
      case Some(uuid) =>
        exists(uuid).flatMap:
          case false => insert(job)
          case _     => delete(uuid).flatMap(_ => insert(job))

  def delete(id: UUID): IO[Int] =
    sql"""DELETE FROM job WHERE id = $id""".update.run
      .transact(xa)
