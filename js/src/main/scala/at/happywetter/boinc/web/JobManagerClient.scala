package at.happywetter.boinc.web

import java.util.UUID

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import at.happywetter.boinc.shared.parser._
import at.happywetter.boinc.shared.rpc.jobs.Job
import at.happywetter.boinc.web.util.FetchHelper

object JobManagerClient:

  def all(): Future[List[Job]] =
    FetchHelper.get[List[Job]]("/jobs")

  def create(job: Job): Future[Job] =
    FetchHelper
      .post[Job, String]("/jobs", job)
      .map(uuid => job.copy(id = Some(UUID.fromString(uuid))))

  def start(job: Job): Future[String] =
    FetchHelper.post[String, String](s"/jobs/${job.id.get}/start", "")

  def stop(job: Job): Future[String] =
    FetchHelper.post[String, String](s"/jobs/${job.id.get}/stop", "")

  def delete(job: Job): Future[String] =
    FetchHelper.delete[String](s"/jobs/${job.id.get}")
