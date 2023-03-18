package at.happywetter.boinc.shared

import at.happywetter.boinc.shared.boincrpc.BoincRPC
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.07.2019
  */
package object parser {
  import upickle.default._

  implicit val fileRefParser = macroRW[boincrpc.FileRef]
  implicit val appVersionCoProcParser = macroRW[boincrpc.AppVersionCoProc]
  implicit val appVersionParser = macroRW[boincrpc.AppVersion]
  implicit val appParser = macroRW[boincrpc.App]
  implicit val coProcGlParser = macroRW[boincrpc.CoProcessorOpenCL]
  implicit val coProcParser = macroRW[boincrpc.CoProcessor]
  implicit val hostInfoParser = macroRW[boincrpc.HostInfo]
  implicit val taskParser = macroRW[boincrpc.Task]
  implicit val resultParser = macroRW[boincrpc.Result]
  implicit val workunitParser = macroRW[boincrpc.Workunit]
  implicit val projectGuiURLParser = macroRW[boincrpc.ProjectGuiURL]
  implicit val projectParser = macroRW[boincrpc.Project]
  implicit val netstatsParser = macroRW[boincrpc.NetStats]
  implicit val timeStatsParser = macroRW[boincrpc.TimeStats]
  implicit val boincStateParser = macroRW[boincrpc.BoincState]
  implicit val ccStateParser = macroRW[boincrpc.CCState]
  implicit val ccStatusParser = macroRW[boincrpc.CCStatus]
  implicit val diskUsageParser = macroRW[boincrpc.DiskUsage]
  implicit val pFilexFerParser = macroRW[boincrpc.PersistentFileXfer]
  implicit val fileXFerParser = macroRW[boincrpc.FileXfer]
  implicit val fileTransferParser = macroRW[boincrpc.FileTransfer]
  implicit val dayEntryParser = macroRW[boincrpc.DayEntry]
  implicit val globalPrefsParser = macroRW[boincrpc.GlobalPrefsOverride]
  implicit val dailyStatsParser = macroRW[boincrpc.DailyStatistic]
  implicit val statisticParser = macroRW[boincrpc.Statistics]
  implicit val messageParser = macroRW[boincrpc.Message]
  implicit val noticeParser = macroRW[boincrpc.Notice]
  implicit val versionParser = macroRW[boincrpc.BoincVersion]

  implicit val retryFileTransferParser = macroRW[boincrpc.RetryFileTransferBody]
  implicit val wuRBodyParser = macroRW[boincrpc.WorkunitRequestBody]
  implicit val projRBodyParser = macroRW[boincrpc.ProjectRequestBody]
  implicit val addProjectParser = macroRW[boincrpc.AddProjectBody]
  implicit val boincModeChangeParser = macroRW[boincrpc.BoincModeChange]
  implicit val projectMetaDataParser = macroRW[boincrpc.BoincProjectMetaData]
  implicit val userParser = macroRW[webrpc.User]
  implicit val appErrorParser = macroRW[boincrpc.ApplicationError]
  implicit val serverCfgParser = macroRW[boincrpc.ServerSharedConfig]
  implicit val swAppStatusParser = macroRW[webrpc.ServerStatusApp]
  implicit val dbfileParser = macroRW[webrpc.DatabaseFileStates]
  implicit val daemoParser = macroRW[webrpc.Daemon]
  implicit val serverStatusParser = macroRW[webrpc.ServerStatus]

  implicit val hwSensorsParser = macroRW[extension.HardwareData.SensorsRow]
  implicit val addNewHostRequestBodyParser = macroRW[boincrpc.AddNewHostRequestBody]

  implicit val hostDetailsParser = macroRW[rpc.HostDetails]

  implicit val dashboardDataEntryParser = macroRW[rpc.DashboardDataEntry]

  implicit val appConfigGpuVersionParser = macroRW[boincrpc.AppConfig.GpuVersions]
  implicit val appConfigAppVersionParser = macroRW[boincrpc.AppConfig.AppVersion]
  implicit val appConfigAppParser = macroRW[boincrpc.AppConfig.App]
  implicit val appConfigParser = macroRW[boincrpc.AppConfig]

  implicit val localDateTimeParser = readwriter[String].bimap[LocalDateTime](
    _.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)
  )

  implicit val jobModeOnceParser = macroRW[rpc.jobs.Once.type]
  implicit val jobModeRunningParser = macroRW[rpc.jobs.Running.type]
  implicit val jobModeCPUParser = macroRW[rpc.jobs.CPU.type]
  implicit val jobModeAtParser = macroRW[rpc.jobs.At]
  implicit val jobModeEveryParser = macroRW[rpc.jobs.Every]
  implicit val jobModeParser = macroRW[rpc.jobs.JobMode]

  implicit val boincRpcModesParser = readwriter[Int].bimap[BoincRPC.Modes.Value](_.id, BoincRPC.Modes(_))
  implicit val projectActionParser = readwriter[Int].bimap[ProjectAction](_.id, ProjectAction(_))

  implicit val jobStatusErroredParser = macroRW[rpc.jobs.Errored]
  implicit val jobStatusStoppedParser = macroRW[rpc.jobs.Stopped.type]
  implicit val jobStatusParser = macroRW[rpc.jobs.JobStatus]

  implicit val jobRunModeGPUParser = macroRW[rpc.jobs.GPU.type]
  implicit val jobRunModeNetworkParser = macroRW[rpc.jobs.Network.type]
  implicit val jobRunModeTargetParser = macroRW[rpc.jobs.BoincRunModeTarget]

  implicit val jobProjectActionParser = macroRW[rpc.jobs.BoincProjectAction]
  implicit val jobRunModeAction = macroRW[rpc.jobs.BoincRunModeAction]
  implicit val jobActionParser = macroRW[rpc.jobs.JobAction]

  implicit val jobParser = macroRW[rpc.jobs.Job]

}
