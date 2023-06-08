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
package object parser:
  import upickle.default.{macroRW, readwriter, ReadWriter => RW}

  implicit val fileRefParser: RW[boincrpc.FileRef] = macroRW
  implicit val appVersionCoProcParser: RW[boincrpc.AppVersionCoProc] = macroRW
  implicit val appVersionParser: RW[boincrpc.AppVersion] = macroRW
  implicit val appParser: RW[boincrpc.App] = macroRW
  implicit val coProcGlParser: RW[boincrpc.CoProcessorOpenCL] = macroRW
  implicit val coProcParser: RW[boincrpc.CoProcessor] = macroRW
  implicit val hostInfoParser: RW[boincrpc.HostInfo] = macroRW
  implicit val taskParser: RW[boincrpc.Task] = macroRW
  implicit val resultParser: RW[boincrpc.Result] = macroRW
  implicit val workunitParser: RW[boincrpc.Workunit] = macroRW
  implicit val projectGuiURLParser: RW[boincrpc.ProjectGuiURL] = macroRW
  implicit val projectParser: RW[boincrpc.Project] = macroRW
  implicit val netstatsParser: RW[boincrpc.NetStats] = macroRW
  implicit val timeStatsParser: RW[boincrpc.TimeStats] = macroRW
  implicit val boincStateParser: RW[boincrpc.BoincState] = macroRW
  implicit val ccStateParser: RW[boincrpc.CCState] = macroRW
  implicit val ccStatusParser: RW[boincrpc.CCStatus] = macroRW
  implicit val diskUsageParser: RW[boincrpc.DiskUsage] = macroRW
  implicit val pFilexFerParser: RW[boincrpc.PersistentFileXfer] = macroRW
  implicit val fileXFerParser: RW[boincrpc.FileXfer] = macroRW
  implicit val fileTransferParser: RW[boincrpc.FileTransfer] = macroRW
  implicit val dayEntryParser: RW[boincrpc.DayEntry] = macroRW
  implicit val globalPrefsParser: RW[boincrpc.GlobalPrefsOverride] = macroRW
  implicit val dailyStatsParser: RW[boincrpc.DailyStatistic] = macroRW
  implicit val statisticParser: RW[boincrpc.Statistics] = macroRW
  implicit val messageParser: RW[boincrpc.Message] = macroRW
  implicit val noticeParser: RW[boincrpc.Notice] = macroRW
  implicit val versionParser: RW[boincrpc.BoincVersion] = macroRW

  implicit val retryFileTransferParser: RW[boincrpc.RetryFileTransferBody] = macroRW
  implicit val wuRBodyParser: RW[boincrpc.WorkunitRequestBody] = macroRW
  implicit val projRBodyParser: RW[boincrpc.ProjectRequestBody] = macroRW
  implicit val addProjectParser: RW[boincrpc.AddProjectBody] = macroRW
  implicit val boincModeChangeParser: RW[boincrpc.BoincModeChange] = macroRW
  implicit val projectMetaDataParser: RW[boincrpc.BoincProjectMetaData] = macroRW
  implicit val userParser: RW[webrpc.User] = macroRW
  implicit val appErrorParser: RW[boincrpc.ApplicationError] = macroRW
  implicit val serverCfgParser: RW[boincrpc.ServerSharedConfig] = macroRW
  implicit val swAppStatusParser: RW[webrpc.ServerStatusApp] = macroRW
  implicit val dbfileParser: RW[webrpc.DatabaseFileStates] = macroRW
  implicit val daemoParser: RW[webrpc.Daemon] = macroRW
  implicit val serverStatusParser: RW[webrpc.ServerStatus] = macroRW

  implicit val hwSensorsParser: RW[extension.HardwareData.SensorsRow] = macroRW
  implicit val addNewHostRequestBodyParser: RW[boincrpc.AddNewHostRequestBody] = macroRW

  implicit val hostDetailsParser: RW[rpc.HostDetails] = macroRW

  implicit val dashboardDataEntryParser: RW[rpc.DashboardDataEntry] = macroRW

  implicit val appConfigGpuVersionParser: RW[boincrpc.AppConfig.GpuVersions] = macroRW
  implicit val appConfigAppVersionParser: RW[boincrpc.AppConfig.AppVersion] = macroRW
  implicit val appConfigAppParser: RW[boincrpc.AppConfig.App] = macroRW
  implicit val appConfigParser: RW[boincrpc.AppConfig] = macroRW

  implicit val localDateTimeParser: RW[LocalDateTime] = readwriter[String].bimap(
    _.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)
  )

  implicit val jobModeOnceParser: RW[rpc.jobs.Once.type] = macroRW
  implicit val jobModeRunningParser: RW[rpc.jobs.Running.type] = macroRW
  implicit val jobModeCPUParser: RW[rpc.jobs.CPU.type] = macroRW
  implicit val jobModeAtParser: RW[rpc.jobs.At] = macroRW
  implicit val jobModeEveryParser: RW[rpc.jobs.Every] = macroRW
  implicit val jobModeParser: RW[rpc.jobs.JobMode] = macroRW

  implicit val boincRpcModesParser: RW[BoincRPC.Modes.Value] = readwriter[Int].bimap(_.id, BoincRPC.Modes(_))
  implicit val projectActionParser: RW[ProjectAction] = readwriter[Int].bimap(_.id, ProjectAction(_))

  implicit val jobStatusErroredParser: RW[rpc.jobs.Errored] = macroRW
  implicit val jobStatusStoppedParser: RW[rpc.jobs.Stopped.type] = macroRW
  implicit val jobStatusParser: RW[rpc.jobs.JobStatus] = macroRW

  implicit val jobRunModeGPUParser: RW[rpc.jobs.GPU.type] = macroRW
  implicit val jobRunModeNetworkParser: RW[rpc.jobs.Network.type] = macroRW
  implicit val jobRunModeTargetParser: RW[rpc.jobs.BoincRunModeTarget] = macroRW

  implicit val jobProjectActionParser: RW[rpc.jobs.BoincProjectAction] = macroRW
  implicit val jobRunModeAction: RW[rpc.jobs.BoincRunModeAction] = macroRW
  implicit val jobActionParser: RW[rpc.jobs.JobAction] = macroRW

  implicit val jobParser: RW[rpc.jobs.Job] = macroRW
