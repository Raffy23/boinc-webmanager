package at.happywetter.boinc.shared

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.07.2019
  */
package object parser {
  import upickle.default._

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
  implicit val boincStateParser = macroRW[boincrpc.BoincState]
  implicit val ccStateParser = macroRW[boincrpc.CCState]
  implicit val ccStatusParser = macroRW[boincrpc.CCStatus]
  implicit val diskUsageParser = macroRW[boincrpc.DiskUsage]
  implicit val pFilexFerParser = macroRW[boincrpc.PersistentFileXfer]
  implicit val fileXFerParser = macroRW[boincrpc.FileXfer]
  implicit val fileTransferParser = macroRW[boincrpc.FileTransfer]
  implicit val globalPrefsParser = macroRW[boincrpc.GlobalPrefsOverride]
  implicit val dailyStatsParser = macroRW[boincrpc.DailyStatistic]
  implicit val statisticParser = macroRW[boincrpc.Statistics]
  implicit val messageParser = macroRW[boincrpc.Message]
  implicit val noticeParser = macroRW[boincrpc.Notice]


  implicit val wuRBodyParser = macroRW[webrpc.WorkunitRequestBody]
  implicit val projRBodyParser = macroRW[webrpc.ProjectRequestBody]
  implicit val addProjectParser = macroRW[webrpc.AddProjectBody]
  implicit val boincModeChangeParser = macroRW[webrpc.BoincModeChange]
  implicit val projectMetaDataParser = macroRW[webrpc.BoincProjectMetaData]
  implicit val userParser = macroRW[webrpc.User]
  implicit val appErrorParser = macroRW[webrpc.ApplicationError]
  implicit val serverCfgParser = macroRW[webrpc.ServerSharedConfig]
  implicit val swAppStatusParser = macroRW[webrpc.ServerStatusApp]
  implicit val dbfileParser = macroRW[webrpc.DatabaseFileStates]
  implicit val daemoParser = macroRW[webrpc.Daemon]
  implicit val serverStatusParser = macroRW[webrpc.ServerStatus]

  implicit val hwSensorsParser = macroRW[extension.HardwareData.SensorsRow]




}
