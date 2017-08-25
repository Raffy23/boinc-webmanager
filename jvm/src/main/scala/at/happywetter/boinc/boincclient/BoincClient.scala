package at.happywetter.boinc.boincclient

import java.io.InputStream
import java.math.BigInteger
import java.net.{InetAddress, Socket}
import java.security.MessageDigest

import at.happywetter.boinc.shared.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared.{BoincRPC, _}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.{NodeSeq, XML}

/**
  * Basic Class for the BOINC Communication
  *
  * Created by
  * @author Raphael Ludwig
  * @version 08.07.2016
  */


//https://github.com/BOINC/boinc/blob/f691a3f0f9e30a12ee19558125465d779fae815e/android/BOINC/app/src/main/java/edu/berkeley/boinc/rpc/RpcClient.java
object BoincClient {
  object Mode extends Enumeration {
    val Always  = Value("<always/>")
    val Never   = Value("<never/>")
    val Auto    = Value("<auto/>")
    val Restore = Value("<restore/>")
  }
  object Command extends Enumeration {
    val GetHostInfo   = Value("<get_host_info/>")
    val GetState      = Value("<get_state/>")
    val GetDiskUsage  = Value("<get_disk_usage/>")
    val GetStatistics = Value("<get_statistics/>")
    val GetResults    = Value("<get_results></get_results>")
    val GetActiveResults = Value("<get_results><active_only>1</active_only></get_results>")
    val GetMessages   = Value("<get_messages><seqno>0</seqno></get_messages>")
    val RunBenchmarks = Value("<run_benchmarks/>")
    val GetCCStatus   = Value("<get_cc_status/>")
    val GetNetworkAvailable = Value("<network_available/>")
    val GetProjectStatus = Value("<get_project_status/>")
    val GetFileTransfer = Value("<get_file_transfers/>")
  }
}
class BoincClient(address: String, port: Int = 31416, password: String) extends BoincCoreClient {
  var socket: Socket = _
  var reader: InputStream = _
  var authenticated = false

  protected val logger: Logger = LoggerFactory.getLogger(BoincClient.getClass.getCanonicalName)

  private def connect(): Unit = {
    this.socket = new Socket(InetAddress.getByName(address), port)
    this.reader = this.socket.getInputStream
  }

  private def sendData(data: String): Unit = this.socket.getOutputStream.write(("<boinc_gui_rpc_request>\n" + data + "\n</boinc_gui_rpc_request>\n\u0003").getBytes)

  private def readXML(): NodeSeq = XML.loadString(readStringFromSocket())

  private def readStringFromSocket(): String = Stream.continually(read).takeWhile(_ != '\u0003').mkString

  private def read: Char = {
    val c = reader.read()

    if (c == -1) return '\u0003'
    c.toChar
  }

  private def rpc(data: String): NodeSeq = {
    this.handleSocketConnection()

    sendData(data)
    readXML()
  }

  private def handleSocketConnection(): Unit = {
    if (socket == null || socket.isClosed) {
      this.connect()
    }
  }

  def authenticate(): Boolean = {
    logger.trace("Sending auth Challenge to " + address + ":" + port)
    val nonce = (this.rpc("<auth1>") \ "nonce").text
    val result = this.rpc("<auth2>\n<nonce_hash>" + md5(nonce + password) + "</nonce_hash>\n</auth2>")

    authenticated = (result \ "_").xml_==(<authorized/>)
    logger.trace("Client connection is " + (if(authenticated) "" else " *NOT* authenticated!"))

    authenticated
  }

  private def md5(str: String): String = {
    //http://web.archive.org/web/20140209230440/http://www.sergiy.ca/how-to-make-java-md5-and-sha-1-hashes-compatible-with-php-or-mysql/
    val hash = new BigInteger(1, MessageDigest.getInstance("MD5").digest(str.getBytes("UTF-8")))
    var result = hash.toString(16)

    while (result.length() < 32) { //40 for SHA-1
      result = "0" + result
    }

    result
  }


  def execCommand(cmd: BoincClient.Command.Value): NodeSeq = execAction(cmd.toString)

  private def execAction(action: String): NodeSeq = {
    this.synchronized {
      if (!this.authenticated) this.authenticate()
      rpc(action)
    }
  }

  override def getTasks(active: Boolean = true): Future[List[Result]] = Future {
    logger.trace("Get Tasks from" + address + ":" + port)

    val tasks: mutable.MutableList[Result] = new mutable.MutableList()
    val xml = execCommand(if (active) BoincClient.Command.GetActiveResults else BoincClient.Command.GetResults)

    for (task <- xml \ "results" \ "result")
      tasks.+=(ResultParser.fromXML(task))

    tasks.toList
  }

  override def getHostInfo: Future[HostInfo] = Future {
    logger.trace("Get HostInfo from" + address + ":" + port)
    HostInfoParser.fromXML(execCommand(BoincClient.Command.GetHostInfo) \ "host_info")
  }

  override def isNetworkAvailable: Future[Boolean] = Future {
    logger.trace("Get NetworkStatus from" + address + ":" + port)
    (execCommand(BoincClient.Command.GetNetworkAvailable) \ "success").xml_==(<success/>)
  }

  override def getDiskUsage: Future[DiskUsage] = Future {
    logger.trace("Get Diskusage from" + address + ":" + port)
    DiskUsageParser.fromXML(execCommand(BoincClient.Command.GetDiskUsage) \ "disk_usage_summary")
  }

  override def getProjects: Future[List[Project]] = Future {
    logger.trace("Get Projects from" + address + ":" + port)
    ProjectParser.fromXML(execCommand(BoincClient.Command.GetProjectStatus) \ "projects")
  }

  override def getState: Future[BoincState] = Future {
    logger.trace("Get State from" + address + ":" + port)
    BoincStateParser.fromXML(execCommand(BoincClient.Command.GetState) \ "client_state")
  }

  override def getFileTransfer: Future[List[FileTransfer]] = Future {
    logger.trace("Get FileTransfer from" + address + ":" + port)
    FileTransferParser.fromXML(execCommand(BoincClient.Command.GetFileTransfer) \ "file_transfers")
  }

  override def workunit(project: String, name: String, action: WorkunitAction): Future[Boolean] = Future {
    logger.trace("Set Workunit state for" + address + ":" + port)
    (this.execAction(s"<${action.toString}><project_url>$project</project_url><name>$name</name></${action.toString}>") \ "success").xml_==(<success/>)
  }

  override def project(name: String, action: ProjectAction): Future[Boolean] = Future {
    logger.trace("Set Project state for " + address + ":" + port)
    (this.execAction(s"<${action.toString}><project_url>$name</project_url></${action.toString}>") \ "success").xml_==(<success/>)
  }

  override def getCCState = Future {
    logger.trace("Get CCState for " + address + ":" + port)
    CCStateParser.fromXML(execCommand(BoincClient.Command.GetCCStatus))
  }



  override def getGlobalPrefsOverride = ???

  override def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride) = ???

  override def setRun(mode: BoincRPC.Modes.Value, duration: Double) = Future {
    (this.execAction(s"<set_run_mode>${mode.toString}${if(duration>0) s"<duration>${duration.toFloat}</duration>"}</set_run_mode>") \ "success").xml_==(<success/>)
  }

  override def setCpu(mode: BoincRPC.Modes.Value, duration: Double) = Future {
    (this.execAction(s"<set_cpu_mode>${mode.toString}${if(duration>0) s"<duration>${duration.toFloat}</duration>"}</set_cpu_mode>") \ "success").xml_==(<success/>)
  }

  override def setGpu(mode: BoincRPC.Modes.Value, duration: Double) = Future {
    (this.execAction(s"<set_gpu_mode>${mode.toString}${if(duration>0) s"<duration>${duration.toFloat}</duration>"}</set_gpu_mode>") \ "success").xml_==(<success/>)
  }

  override def setNetwork(mode: BoincRPC.Modes.Value, duration: Double) = Future {
    (this.execAction(s"<set_network_mode>${mode.toString}${if(duration>0) s"<duration>${duration.toFloat}</duration>"}</set_network_mode>") \ "success").xml_==(<success/>)
  }

  def isAuthenticated: Boolean = this.authenticated

  def close(): Unit = {
    if (socket != null && socket.isConnected) socket.close()
    authenticated = false
  }
}