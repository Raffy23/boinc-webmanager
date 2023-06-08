package at.happywetter.boinc.server

import at.happywetter.boinc.Database
import at.happywetter.boinc.util.JobManager
import at.happywetter.boinc.util.http4s.ResponseEncodingHelper
import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.rpc.jobs.{Job, Running, Stopped}
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import at.happywetter.boinc.util.http4s.RichMsgPackRequest.RichMsgPacKResponse

import java.util.UUID

object JobManagerRoutes extends ResponseEncodingHelper:

  def apply(db: Database, manager: JobManager): HttpRoutes[IO] = HttpRoutes.of[IO]:

    case req @ GET -> Root =>
      Ok(
        manager
          .all()
          .map { jobs => jobs.map(_.dto) },
        req
      )

    case req @ POST -> Root =>
      req.decodeJson[Job] { job =>
        Ok(
          db.jobs.insert(job).flatMap { job =>
            manager.add(JobManager.User("admin"), job)
          },
          req
        )
      }

    case req @ POST -> Root / uuid / "start" =>
      val jobID = UUID.fromString(uuid)

      manager
        .status(jobID)
        .flatMap:
          case Stopped => Ok(manager.start(jobID).as(""), req)
          case Running => Ok("", req)

    case req @ POST -> Root / uuid / "stop" =>
      val jobID = UUID.fromString(uuid)

      manager
        .status(jobID)
        .flatMap:
          case Stopped => Ok("", req)
          case Running => Ok(manager.stop(jobID).as(""), req)

    case req @ DELETE -> Root / uuid =>
      val jobID = UUID.fromString(uuid)

      Ok(
        db.jobs.delete(jobID) *>
          manager.delete(jobID) *>
          IO.pure(""),
        req
      )
