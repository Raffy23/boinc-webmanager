package at.happywetter.boinc.shared.rpc

import at.happywetter.boinc.shared.boincrpc.BoincRPC
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.duration.FiniteDuration

package object jobs:

  sealed trait JobMode
  case object Once extends JobMode
  case class Every(interval: FiniteDuration, until: Option[LocalDateTime] = None) extends JobMode
  case class At(timestamp: LocalDateTime) extends JobMode

  sealed trait JobAction:
    def hosts: List[String]
  case class BoincProjectAction(hosts: List[String], url: String, action: ProjectAction) extends JobAction
  case class BoincRunModeAction(hosts: List[String],
                                target: BoincRunModeTarget,
                                mode: BoincRPC.Modes.Value,
                                duration: Double = 0
  ) extends JobAction

  sealed trait BoincRunModeTarget
  case object CPU extends BoincRunModeTarget
  case object GPU extends BoincRunModeTarget
  case object Network extends BoincRunModeTarget

  sealed trait JobStatus
  case object Running extends JobStatus
  case object Stopped extends JobStatus
  case class Errored(exception: String) extends JobStatus

  final case class Job(id: Option[UUID], name: String, mode: JobMode, action: JobAction, status: JobStatus)
