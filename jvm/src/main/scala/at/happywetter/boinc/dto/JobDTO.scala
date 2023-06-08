package at.happywetter.boinc.dto

import at.happywetter.boinc.shared.rpc
import at.happywetter.boinc.shared.parser.jobParser
import upickle.default._

import java.util.UUID
import scala.language.implicitConversions

object JobDTO:

  // Database Record
  case class Job(uuid: UUID, contents: Array[Byte]):
    def toRPC: rpc.jobs.Job = readBinary[rpc.jobs.Job](contents)

  implicit class JobConverter(private val job: rpc.jobs.Job) extends AnyVal:
    def toDB: Job =
      val jobUUID = job.id.getOrElse(UUID.randomUUID())
      Job(jobUUID, writeBinary(job.copy(id = Some(jobUUID))))
