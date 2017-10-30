package at.happywetter.boinc.shared

/**
  * Created by: 
  *
  * @author Raphael
  * @version 08.08.2016
  */


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
