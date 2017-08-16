package at.happywetter.boinc.shared


/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.07.2016
  */
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

final case class Result(name: String
                        ,wuName: String
                        ,platfrom: String
                        ,version: String
                        ,plan: String
                        ,project: String
                        ,state: Int
                        ,supsended: Boolean
                        ,activeTask: Option[Task]
                        ,remainingCPU: Double
                        ,reportDeadline: Double) {

  override def equals(obj: scala.Any): Boolean = obj.asInstanceOf[Result].wuName.equals(wuName)
  override def hashCode(): Int = wuName.hashCode
}

object Result {
  object State extends Enumeration {
    val Result_New = Value(0)
    val Result_File_Downloading = Value(1)
    val Result_Files_Downloaded = Value(2)
    val Result_Compute_Error = Value(3)
    val Result_Files_Uploading = Value(4)
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
                         ,coproc: List[CoProcessor])

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
                             ,fileXfer: FileXfer)

final case class PersistentFileXfer(retries: Int
                                    ,firstRequest: Double
                                    ,nextRequest: Double
                                    ,timeSoFar: Double
                                    ,lastBytesXfered: Double
                                    ,isUpload: Boolean)

final case class FileXfer(bytesXfered: Double,xferSpeed: Double,url: String)

final case class NetworkStatus(isNetworkAvailable: Boolean)

final case class Project(name: String
                   , url: String
                   , userName: String
                   , teamName: String
                   , cpid: String
                   , userTotalCredit: Double
                   , userAvgCredit: Double
                   , userID: String // javascript long bug ? (must be long!)
                   , teamID: String
                   , hostID: String
                   , hostTotalCredit: Double
                   , hostAvgCredit: Double
                   , dontRequestWork: Boolean
                   , trickles: Boolean
                   ,guiURLs: List[ProjectGuiURL]) //<-- Bug NullPointerException

final case class ProjectGuiURL(name: String,desc: String,url: String)

final case class App(name: String
               ,userFriendlyName: String
               ,nonCpuIntensive: Boolean
               ,version: AppVersion
               ,project: String)

final case class AppVersion(version: Int
                      ,platform: String
                      ,flops: Double
                      ,avgCpus: Double
                      ,maxCpus: Double)

final case class BoincState(hostInfo: HostInfo
                     ,projects: List[Project]
                     ,apps: Map[String,App]
                     ,workunits: List[Workunit]
                     ,boincVersion: String
                     ,platform: String)

final case class Workunit(name: String
                    ,appName: String
                    ,fopsEst: Double
                    ,fopsBound: Double
                    ,memBound: Double
                    ,diskBound: Double)