package at.happywetter.boinc.boincclient

import java.io.InputStream
import java.net.{InetAddress, Socket}

import at.happywetter.boinc.boincclient.parser.BoincParserUtils._
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.boincrpc.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared.boincrpc.{BoincCoreClient, BoincRPC, _}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.{Logger, LoggerFactory}

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
    val GetNotices   = Value("<get_notices><seqno>0</seqno></get_notices>")
    val RunBenchmarks = Value("<run_benchmarks/>")
    val GetCCStatus   = Value("<get_cc_status/>")
    val GetNetworkAvailable = Value("<network_available/>")
    val GetProjectStatus = Value("<get_project_status/>")
    val GetFileTransfer = Value("<get_file_transfers/>")
    val GetGlobalPrefsOverride = Value("<get_global_prefs_working/>")
    val ReadGlobalPrefsFile = Value("<read_global_prefs_override/>")
    val GetGlobalPrefsWorking = Value("<get_global_prefs_working/>")
    val getStatistics = Value("<get_statistics/>")
  }

}
class BoincClient(address: String, port: Int = 31416, password: String, encoding: String = "UTF-8") extends BoincCoreClient {
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
  private def readHMTL(): Document =
    Jsoup.parse(new String(readStringFromSocket().getBytes(encoding), "UTF-8"))

  private def readStringFromSocket(): String = Stream.continually(read).takeWhile(_ != '\u0003').mkString

  private def read: Char = {
    val c = reader.read()

    if (c == -1) return '\u0003'
    c.toChar
  }

  private def xmlRpc(data: String): NodeSeq = {
    this.handleSocketConnection()

    sendData(data)
    readXML()
  }

  private def htmlRpc(data: String): Document = {
    this.handleSocketConnection()

    sendData(data)
    readHMTL()
  }

  private def handleSocketConnection(): Unit = {
    if (socket == null || socket.isClosed) {
      this.connect()
    }
  }

  def authenticate(): Boolean = {
    logger.trace("Sending auth Challenge to " + address + ":" + port)
    val nonce = (this.xmlRpc("<auth1>") \ "nonce").text
    val result = this.xmlRpc("<auth2>\n<nonce_hash>" + BoincCryptoHelper.md5(nonce + password) + "</nonce_hash>\n</auth2>")

    authenticated = (result \ "_").xml_==(<authorized/>)
    logger.trace(s"Client connection is ${if(authenticated) "" else "*NOT*"} authenticated!")

    authenticated
  }

  def execCommand(cmd: BoincClient.Command.Value): NodeSeq = execAction(cmd.toString)
  def execHtmlCommand(cmd: BoincClient.Command.Value): Document = execHTMLAction(cmd.toString)

  private def execAction(action: NodeSeq): NodeSeq = execAction(action.toString())
  private def execHTMLAction(action: NodeSeq): Document = execHTMLAction(action.toString())

  private def execAction(action: String): NodeSeq = {
    this.synchronized {
      if (!this.authenticated) this.authenticate()
      xmlRpc(action)
    }
  }

  private def execHTMLAction(action: String): Document = {
    this.synchronized {
      if (!this.authenticated) this.authenticate()
      htmlRpc(action)
    }
  }

  override def getTasks(active: Boolean = true): Future[List[Result]] = Future {
    logger.trace("Get Tasks from" + address + ":" + port)

    val xml = execCommand(if (active) BoincClient.Command.GetActiveResults else BoincClient.Command.GetResults)
    (for (task <- xml \ "results" \ "result") yield task.toResult).toList
  }

  override def getHostInfo: Future[HostInfo] = Future {
    logger.trace("Get HostInfo from" + address + ":" + port)
    (execCommand(BoincClient.Command.GetHostInfo) \ "host_info").toHostInfo
  }

  override def isNetworkAvailable: Future[Boolean] = Future {
    logger.trace("Get NetworkStatus from" + address + ":" + port)
    (execCommand(BoincClient.Command.GetNetworkAvailable) \ "success").xml_==(<success/>)
  }

  override def getDiskUsage: Future[DiskUsage] = Future {
    logger.trace("Get Diskusage from" + address + ":" + port)
    (execCommand(BoincClient.Command.GetDiskUsage) \ "disk_usage_summary").toDiskUsage
  }

  override def getProjects: Future[List[Project]] = Future {
    logger.trace("Get Projects from" + address + ":" + port)
    (execCommand(BoincClient.Command.GetProjectStatus) \ "projects").toProjects
  }

  override def getState: Future[BoincState] = Future {
    logger.trace("Get State from" + address + ":" + port)
    (execCommand(BoincClient.Command.GetState) \ "client_state").toState
  }

  override def getFileTransfer: Future[List[FileTransfer]] = Future {
    logger.trace("Get FileTransfer from" + address + ":" + port)
    (execCommand(BoincClient.Command.GetFileTransfer) \ "file_transfers").toFileTransfers
  }

  override def workunit(project: String, name: String, action: WorkunitAction): Future[Boolean] = Future {
    logger.trace("Set Workunit state at" + address + ":" + port)
    (this.execAction(s"<${action.toString}><project_url>$project</project_url><name>$name</name></${action.toString}>") \ "success").xml_==(<success/>)
  }

  override def project(url: String, action: ProjectAction): Future[Boolean] = Future {
    logger.trace("Set Project state at " + address + ":" + port)
    (this.execAction(s"<${action.toString}><project_url>$url</project_url></${action.toString}>") \ "success").xml_==(<success/>)
  }

  override def getCCState: Future[CCState] = Future {
    logger.trace("Get CCState from " + address + ":" + port)
    (execCommand(BoincClient.Command.GetCCStatus) \ "cc_status").toCCState
  }

  override def getGlobalPrefsOverride: Future[GlobalPrefsOverride] = Future {
    logger.trace("Get GlobalPrefsOverride for " + address + ":" + port)
    (execCommand(BoincClient.Command.GetGlobalPrefsOverride) \ "global_preferences").toGlobalPrefs
  }

  override def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride) = Future {
    logger.trace("Setting GlobalPrefsOverride at " + address + ":" + port)
    println(s"<set_global_prefs_override>${globalPrefsOverride.toXML}</set_global_prefs_override>")

    val res = this.execAction(s"<set_global_prefs_override>\n${globalPrefsOverride.toXML}\n</set_global_prefs_override>")
    println(res)

    (res \ "success").xml_==(<success/>)
  }

  override def setRun(mode: BoincRPC.Modes.Value, duration: Double) = Future {
    logger.trace("Setting RunMode at " + address + ":" + port + " to " + mode)
    (this.execAction(s"<set_run_mode><${mode.toString}/><duration>${duration.toFloat}</duration></set_run_mode>") \ "success").xml_==(<success/>)
  }

  override def setCpu(mode: BoincRPC.Modes.Value, duration: Double) = Future {
    logger.trace("Setting CPU at " + address + ":" + port + " to " + mode)
    (this.execAction(s"<set_cpu_mode><${mode.toString}/><duration>${duration.toFloat}</duration></set_cpu_mode>") \ "success").xml_==(<success/>)
  }

  override def setGpu(mode: BoincRPC.Modes.Value, duration: Double) = Future {
    logger.trace("Setting GPU at " + address + ":" + port + " to " + mode)
    (this.execAction(s"<set_gpu_mode><${mode.toString}/><duration>${duration.toFloat}</duration></set_gpu_mode>") \ "success").xml_==(<success/>)
  }

  override def setNetwork(mode: BoincRPC.Modes.Value, duration: Double) = Future {
    logger.trace("Setting Network at " + address + ":" + port + " to " + mode)
    (this.execAction(s"<set_network_mode><${mode.toString}/><duration>${duration.toFloat}</duration></set_network_mode>") \ "success").xml_==(<success/>)
  }

  override def attachProject(url: String, authenticator: String, name: String) = Future {
    logger.trace("Attach Project (" + url + ") at " + address + ":" + port)
    (this.execAction(
        <project_attach>
          <project_url>{url}</project_url>
          <authenticator>{authenticator}</authenticator>
          <project_name>{name}</project_name>
        </project_attach>
      ) \ "success"
    ).xml_==(<success/>)
  }

  override def getMessages(seqno: Int) = Future {
    logger.trace("Getting Messages (" + seqno + ") from " + address + ":" + port)
    this.execHTMLAction(
      <get_messages>
        <seqno>{seqno}</seqno>
      </get_messages>
    ).toMessages
  }

  override def getNotices(seqno: Int) = Future {
    logger.trace("Getting Notices (" + seqno + ") from " + address + ":" + port)
    this.execHTMLAction(
      <get_notices>
        <seqno>{seqno}</seqno>
      </get_notices>
    ).toNotices
  }

  override def getStatistics = Future {
    logger.trace("Getting Statistics from " + address + ":" + port)
    execCommand(BoincClient.Command.GetStatistics).toStatistics
  }


  override def readGlobalPrefsOverride = Future {
    (this.execCommand(BoincClient.Command.ReadGlobalPrefsFile) \ "success").xml_==(<success/>)
  }

  def isAuthenticated: Boolean = this.authenticated

  def close(): Unit = {
    if (socket != null && socket.isConnected) {
      logger.trace("Closing Socket for " + address + ":" + port)
      socket.close()
    }

    authenticated = false
  }
}