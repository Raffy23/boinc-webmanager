package at.happywetter.boinc.repository

import at.happywetter.boinc.dto.JobDTO.Job
import at.happywetter.boinc.dto.JobDTO.JobConverter
import at.happywetter.boinc.shared.rpc.jobs.{Job => RpcJob}
import cats.effect.IO
import io.getquill.{H2JdbcContext, SnakeCase}

import java.util.UUID

class JobRepository(ctx: H2JdbcContext[SnakeCase]) {
  import ctx.{IO => _, _}

  def insert(job: RpcJob): IO[RpcJob] = IO.blocking {
    val dbJob = job.toDB

    run {
      quote {
        query[Job].insert(lift(dbJob))
      }
    }

    job.copy(id = Some(dbJob.uuid))
  }

  def queryAll(): IO[List[RpcJob]] = IO.blocking {
    run {
      quote {
        query[Job]
      }
    }
  }.map(_.map(_.toRPC))

  def exists(id: UUID): IO[Boolean] = IO.blocking {
    run {
      quote {
        query[Job].filter(_.uuid == lift(id))
      }.size
    } > 0
  }

  def update(job: RpcJob): IO[RpcJob] = {
    job.id match {
      case None       => insert(job)
      case Some(uuid) => exists(uuid).flatMap {
        case false => insert(job)
        case _     => delete(uuid).flatMap(_ => insert(job))
      }
    }
  }

  def delete(id: UUID): IO[Long] = IO.blocking {
    run {
      quote {
        query[Job].filter(_.uuid == lift(id)).delete
      }
    }
  }

}
