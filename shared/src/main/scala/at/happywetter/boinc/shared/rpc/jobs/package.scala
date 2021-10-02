package at.happywetter.boinc.shared.rpc

import at.happywetter.boinc.shared.boincrpc.BoincRPC
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.duration.FiniteDuration

package object jobs {

  sealed trait JobMode
  case object Once extends JobMode
  case class Every(interval: FiniteDuration, until: Option[LocalDateTime] = None) extends JobMode
  case class At(timestamp: LocalDateTime) extends JobMode

  sealed trait JobAction
  case class BoincProjectAction(host: String, url: String, action: ProjectAction) extends JobAction
  case class BoincRunModeAction(host: String, target: BoincRunModeTarget, mode: BoincRPC.Modes.Value, duration: Double = 0) extends JobAction

  sealed trait BoincRunModeTarget
  case object CPU extends BoincRunModeTarget
  case object GPU extends BoincRunModeTarget
  case object Network extends BoincRunModeTarget

  final case class Job(id: Option[UUID], mode: JobMode, action: JobAction)

}
