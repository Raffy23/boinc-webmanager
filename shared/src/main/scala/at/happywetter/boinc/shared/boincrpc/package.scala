package at.happywetter.boinc.shared

/**
  * Created by: 
  *
  * @author Raphael
  * @version 05.07.2019
  */
package object boincrpc {


  final case class Task(activeTaskState: Int // 0=>inactive,1=>active,9=>pause(time)
                        ,appVersionNum: Int
                        ,slot: Int
                        ,pid: Int
                        ,schedulerState: Int
                        ,checkpoint: Double
                        ,done: Double
                        ,cpuTime: Double
                        ,time: Double
                        ,swapSize: Double
                        ,workingSet: Double
                        ,progress: Double)

  final case class Result(name: String,
                          wuName: String,
                          platfrom: String,
                          version: String, // version_num
                          plan: String, // plan_class
                          project: String,
                          state: Int,
                          supsended: Boolean,
                          activeTask: Option[Task],
                          remainingCPU: Double, // estimated_cpu_time_remaining
                          reportDeadline: Double,
                          finalCPUTime: Double,
                          finalElapsedTime: Double,
                          exitStatus: Int) {

    override def equals(obj: scala.Any): Boolean = obj.asInstanceOf[Result].wuName.equals(wuName)
    override def hashCode(): Int = wuName.hashCode
  }

  object Result {
    object State extends Enumeration {
      val Result_New = Value(0)
      val Result_File_Downloading = Value(1)
      val Result_Files_Downloaded = Value(2)
      val Result_Compute_Error = Value(3)
      val Result_Files_Uploading = Value(4) // aka. Task finished computing
      val Result_Files_Uploaded = Value(5)
      val Result_Aborted = Value(6)
      val Result_Upload_Failed = Value(7)
    }

    object ActiveTaskState extends Enumeration {
      val PROCESS_UNINITIALIZED = Value(0)
      val PROCESS_EXECUTING = Value(1)
      val PROCESS_SUSPENDED = Value(9)
      val PROCESS_ABORT_PENDING = Value(5)
      val PROCESS_QUIT_PENDING = Value(8)
      val PROCESS_COPY_PENDING = Value(10)
      val PROCESS_EXITED = Value(2)
      val PROCESS_WAS_SIGNALED = Value(3)
      val PROCESS_EXIT_UNKNOWN = Value(4)
      val PROCESS_ABORTED = Value(6)
      val PROCESS_COULDNT_START = Value(7)
    }
  }

  final case class HostInfo(domainName: String
                            ,ipAddr: String
                            ,cpid: String
                            ,cpus: Int
                            ,cpuVendor: String
                            ,cpuModel: String
                            ,cpuFeatures: List[String]
                            ,fops: Double
                            ,iops: Double
                            ,memBW: Double
                            ,memory: Double
                            ,cache: Double
                            ,swap: Double
                            ,diskTotal: Double
                            ,diskFree: Double
                            ,osName: String
                            ,osVersion: String
                            ,coproc: List[CoProcessor]
                            ,virtualBox: Option[String])

  final case class CoProcessor(name: String
                               ,memory: Double
                               ,flops: Double
                               ,openCL: Option[CoProcessorOpenCL])

  final case class CoProcessorOpenCL(name: String
                                     ,vendor: String
                                     ,platfromVersion: String
                                     ,deviceVersion: String
                                     ,driverVersion: String)

  final case class CCStatus(networkStatus: Int
                            ,taskMode: Int
                            ,gpuMode: Int
                            ,networkMode: Int)

  final case class DiskUsage(diskUsage: Map[String,Double]
                             ,total: Double
                             ,free: Double
                             ,boinc: Double
                             ,allowed: Double)

  final case class FileTransfer(projectUrl: String
                                ,projectName: String
                                ,name: String
                                ,byte: Double
                                ,status: Int
                                ,xfer: PersistentFileXfer
                                ,fileXfer: FileXfer
                                ,projectBackoff: Double)
  object FileTransfer {
    object Status extends Enumeration {
      val GiveUpDownload: Status.Value = Value(-114)
      val GiveUpUpload: Status.Value = Value(-115)
      val Normal: Status.Value = Value(1)
      val Default: Status.Value = Value(0)
    }
  }

  final case class PersistentFileXfer(retries: Int
                                      ,firstRequest: Double
                                      ,nextRequest: Double
                                      ,timeSoFar: Double
                                      ,lastBytesXfered: Double
                                      ,isUpload: Boolean)

  final case class FileXfer(bytesXfered: Double, xferSpeed: Double, url: String)

  final case class NetworkStatus(isNetworkAvailable: Boolean)

  final case class Project(name: String
                           , url: String
                           , userName: String
                           , teamName: String
                           , cpid: String
                           , userTotalCredit: Double
                           , userAvgCredit: Double
                           , userID: Long
                           , teamID: Long
                           , hostID: Long
                           , hostTotalCredit: Double
                           , hostAvgCredit: Double
                           , dontRequestWork: Boolean
                           , suspended: Boolean
                           , trickles: Boolean
                           , resourceShare: Double
                           , desiredDiskUsage: Double
                           , durationCorrection: Double
                           , jobSucc: Int
                           , jobErrors: Int
                           , guiURLs: List[ProjectGuiURL])

  final case class ProjectGuiURL(name: String,desc: String,url: String)

  final case class App(name: String,
                       userFriendlyName: String,
                       nonCpuIntensive: Boolean,
                       version: AppVersion,
                       project: String)

  final case class AppVersion(version: Int,
                              platform: String,
                              flops: Double,
                              avgCpus: Double,
                              maxCpus: Option[Double],
                              apiVersion: String,
                              planClass: Option[String],
                              files: List[FileRef],
                              coproc: Option[AppVersionCoProc],
                              gpuRam: Option[Double],
                              dontThrottle: Boolean,
                              needsNetwork: Boolean)

  final case class FileRef(fileName: String,
                           openName: Option[String],
                           mainProgram: Boolean,
                           copyFile: Boolean)

  final case class AppVersionCoProc(`type`: String, count: Double)

  final case class BoincState(hostInfo: HostInfo,
                              projects: List[Project],
                              apps: Map[String,App],
                              workunits: List[Workunit],
                              boincVersion: String,
                              platform: String,
                              results: List[Result],
                              netStats: NetStats,
                              timeStats: TimeStats)

  final case class Workunit(name: String
                            ,appName: String
                            ,fopsEst: Double
                            ,fopsBound: Double
                            ,memBound: Double
                            ,diskBound: Double)

  final case class NetStats(bwup: Double,
                            avgUpload: Double,
                            avgUploadTime: Double,
                            bwdowm: Double,
                            avgDownload: Double,
                            avgDownloadTime: Double)

  final case class TimeStats(on: Double,
                             connected: Double,
                             cpuAndNetworkAvailable: Double,
                             active: Double,
                             gpuActive: Double,
                             clientStartTime: Double,
                             totalStartTime: Double,
                             totalDuration: Double,
                             totalActiveDuration: Double,
                             totalGPUActiveDuration: Double,
                             now: Double,
                             prevUpdate: Double,
                             sessionActiveDuration: Double,
                             sessionGPUActiveDuration: Double)

  //TODO: Convert Magic Numbers to "Magic" Scala Structures
  final case class CCState(networkStatus: Int, //CCState.State.Value,
                           amsPassword_error: Int,
                           taskSuspendReason: Int,
                           taskMode: Int, //CCState.State.Value,
                           taskModePermanent: Int,
                           taskModeDelay: Double,
                           gpuSuspendReason: Int,
                           gpuMode: Int, //CCState.State.Value,
                           gpuModePermanent: Int,
                           gpuModeDelay: Double,
                           networkSuspendReason: Int,
                           networkMode: Int, //CCState.State.Value,
                           networkModePermanent: Int,
                           networkModeDelay: Double,
                           disallowAttach: Boolean,
                           simpleGUIOnly: Boolean,
                           maxEventLogLines: Int)

  object CCState {

    object State extends Enumeration {
      val Default: State.Value  = Value(0)
      val Enabled: State.Value  = Value(1)
      val Auto: State.Value     = Value(2)
      val Disabled: State.Value = Value(3)
    }

  }

  final case class GlobalPrefsOverride(runOnBatteries: Boolean
                                       ,batteryChargeMinPct: Double
                                       ,batteryMaxTemperature: Double
                                       ,runIfUserActive: Boolean
                                       ,runGPUIfUserActive: Boolean
                                       ,idleTimeToRun: Double
                                       ,suspendCpuUsage: Double
                                       ,leaveAppsInMemory: Boolean
                                       ,dontVerifyImages: Boolean
                                       ,workBufferMinDays: Double
                                       ,workBufferAdditionalDays: Double
                                       ,maxNCpuPct: Double
                                       ,cpuSchedulingPeriodMinutes: Double
                                       ,diskInterval: Double
                                       ,diskMaxUsedGB: Double
                                       ,diskMaxUsedPct: Double
                                       ,diskMinFreeGB: Double
                                       ,ramUsedBusyPct: Double
                                       ,ramUsedIdlePct: Double
                                       ,maxBytesSecUpload: Double
                                       ,maxBytesSecDownload: Double
                                       ,cpuUsageLimit: Double
                                       ,dailyXFerLimitMB: Double
                                       ,dailyXFerPeriodDays: Int
                                       ,networkWifiOnly: Boolean
                                       ,cpuTime: (Double, Double)
                                       ,netTime: (Double, Double)
                                       ,cpuTimes: List[(Double, Double)]    // length == 7
                                       ,netTimes: List[(Double, Double)]    // length == 7
                                      )

  final case class TimeSpan(start: Double, end: Double)

  final case class Statistics(stats: Map[String, List[DailyStatistic]])
  final case class DailyStatistic(day: Double, userTotal: Double, userAvg: Double, hostTotal: Double, hostAvg: Double)

  final case class Message(project: String, priority: Int, seqno: Long, time: Long, msg: String)
  object Message {

    object Priority extends Enumeration {

      val Info: Priority.Value = Value(1)
      val UserAlert: Priority.Value = Value(2)
      val InternalError: Priority.Value = Value(3)
      val SchedulerAlert: Priority.Value = Value(4)

    }

  }

  final case class Notice(title: String,
                          description: String,
                          createTime: Double,
                          arrivalTime: Double,
                          isPrivate: Boolean,
                          project: String,
                          category: String,
                          link: String,
                          seqno: Int)

  object BoincRPC {

    object Modes extends Enumeration {
      type Mode = Value

      val Always  = Value("always")
      val Never   = Value("never")
      val Auto    = Value("auto")
      val Restore = Value("restore")

      def fromValue(s: String): Option[Value] = values.find(_.toString == s)
    }

    object Command extends Enumeration {
      type Command = Value

      val GetHostInfo   = Value("hostinfo")
      val GetState      = Value("state")
      val GetDiskUsage  = Value("disk")
      val GetStatistics = Value("statistics")
      val GetResults    = Value("all_tasks")
      val GetActiveResults = Value("tasks")
      val GetMessages   = Value("messages")
      val RunBenchmarks = Value("run_benchmarks")
      val GetCCStatus   = Value("ccstate")
      val GetNetworkAvailable = Value("network_available")
      val GetProjectStatus = Value("projects")
      val GetFileTransfer = Value("filetransfers")
      val ReadGlobalPrefsOverride = Value("global_prefs_override")
      val GetNotices = Value("notices")
      val RetryFileTransfer = Value("retry_file_transfer")

      import scala.language.implicitConversions
      implicit def unapply(arg: Command): String = arg.toString
    }

    object WorkunitAction extends Enumeration {
      type WorkunitAction = Value

      val Suspend = Value("suspend_result")
      val Resume = Value("resume_result")
      val Abort = Value("abort_result")

      def fromValue(s: String): Option[Value] = values.find(_.toString == s)
    }

    object ProjectAction extends Enumeration {
      type ProjectAction = Value

      val Update = Value("project_update")
      val Reset = Value("project_reset")
      val Suspend = Value("project_suspend")
      val Resume = Value("project_resume")
      val Remove = Value("project_detach")
      val NoMoreWork = Value("project_nomorework")
      val AllowMoreWork = Value("project_allowmorework")
      val DetachWhenDone = Value("project_detach_when_done")
      val DontDetachWhenDone = Value("project_dont_detach_when_done")

      def fromValue(s: String): Option[Value] = values.find(_.toString == s)
    }

  }

}
