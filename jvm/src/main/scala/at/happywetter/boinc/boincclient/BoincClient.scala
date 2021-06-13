package at.happywetter.boinc.boincclient

import java.io.InputStream
import java.net.{InetAddress, Socket}
import java.util.concurrent.locks.{ReentrantLock, StampedLock}
import at.happywetter.boinc.BuildInfo
import at.happywetter.boinc.boincclient.parser.AppConfigParser
import at.happywetter.boinc.boincclient.parser.BoincParserUtils._
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.boincrpc.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared.boincrpc.{BoincCoreClient, BoincRPC, _}
import cats.effect.{Blocker, ContextShift, IO}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.xml.{NodeSeq, XML}

/**
  * Basic Class for the BOINC Communication
  *
  * Created by
  * @author Raphael Ludwig
  * @version 08.07.2016
  */
object BoincClient {

  private val CLIENT_IDENTIFER = s"BOINC WebManager ${BuildInfo.version}"
  private val CLIENT_VERSION   = BoincVersion(major = 7, minor = 16, release = 6)

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
  }

}
class BoincClient(address: String, port: Int = 31416, password: String, encoding: String = "UTF-8")(implicit cS: ContextShift[IO], blocker: Blocker) extends BoincCoreClient[IO] with AutoCloseable {
  var socket: Socket = _
  var reader: InputStream = _

  @volatile var authenticated = false
  @volatile var version: Option[BoincVersion] = Option.empty
  private val socketLock = new ReentrantLock()

  protected val logger: Logger = LoggerFactory.getLogger(BoincClient.getClass.getCanonicalName)

  private def connect(): Unit = {
    this.socket = new Socket(InetAddress.getByName(address), port)
    this.reader = this.socket.getInputStream
  }

  private def sendData(data: String): Unit =
    this.socket.getOutputStream.write(("<boinc_gui_rpc_request>\n" + data + "\n</boinc_gui_rpc_request>\n\u0003").getBytes)

  private def readXML(): NodeSeq = XML.loadString(readStringFromSocket())
  private def readHMTL(): Document =
    Jsoup.parse(new String(readStringFromSocket().getBytes(encoding), "UTF-8"))

  private def readStringFromSocket(): String = LazyList.continually(read).takeWhile(_ != '\u0003').mkString

  private def read: Char = {
    val c = reader.read()

    if (c == -1) {
      logTrace(s"Input stream seems to be closed, closing socket (${socket.getInetAddress})")
      authenticated = false
      socket.close()

      return '\u0003'
    }
    c.toChar
  }

  private def doRPC[T](data: String, receiveMethod: () => T): IO[T] = cS.blockOn(blocker)(IO {
    try {
      socketLock.lock()
      this.handleSocketConnection()

      sendData(data)
      receiveMethod()
    } finally {
      socketLock.unlock()
    }
  })

  private def xmlRpc(data: String): IO[NodeSeq] = doRPC(data, readXML)

  private def htmlRpc(data: String): IO[Document] = doRPC(data, readHMTL)

  @inline private def handleSocketConnection(): Unit = {
    if (socket == null || socket.isClosed || !socket.isConnected) {
      this.connect()
    }
  }

  def authenticate(): IO[Boolean] = {
    (for {
      _      <- logTrace(s"Sending AUTH Challenge")
      nonce  <- xmlRpc("<auth1>").map(_ \ "nonce" text)
      result <- xmlRpc("<auth2>\n<nonce_hash>" + BoincCryptoHelper.md5(nonce + password) + "</nonce_hash>\n</auth2>")
    } yield result).map(result => {
      authenticated = (result \ "_").xml_==(<authorized/>)
      logTrace(s"Client connection is${if(authenticated) "" else " *NOT* "}authenticated!")

      authenticated
    })
  }

  @inline def execCommand(cmd: BoincClient.Command.Value): IO[NodeSeq] = execAction(cmd.toString)
  @inline def execHtmlCommand(cmd: BoincClient.Command.Value): IO[Document] = execHTMLAction(cmd.toString)

  @inline private def execAction(action: NodeSeq): IO[NodeSeq] = execAction(action.toString())
  @inline private def execHTMLAction(action: NodeSeq): IO[Document] = execHTMLAction(action.toString())

  private def executeAction[T](action: String, f: String => IO[T]): IO[T] = {
    import cats.implicits._
    (
      if (!this.authenticated) {
        authenticate().flatMap { auth =>
          if (!auth) IO.raiseError(new RuntimeException("Not Authenticated"))
          else exchangeVersion().whenA(version.isEmpty)
        }
      } else IO.unit
    ) *> f(action)
  }

  @inline private def execAction(action: String): IO[NodeSeq] = {
    executeAction(action, xmlRpc)
  }

  @inline private def execHTMLAction(action: String): IO[Document] = {
    executeAction(action, htmlRpc)
  }

  private def exchangeVersion(): IO[BoincVersion] = {
    import BoincClient._

    // Do not use exeAction or so since it's a wrapper aground auth & fetchVersion ...
    doRPC(
      <exchange_versions>
        <major>{CLIENT_VERSION.major}</major>
        <minor>{CLIENT_VERSION.minor}</minor>
        <release>{CLIENT_VERSION.release}</release>
        <name>{CLIENT_IDENTIFER}</name>
      </exchange_versions>.toString(),
      readXML
    ).map(_ \ "server_version" toVersion)
  }

  override def getTasks(active: Boolean = true): IO[List[Result]] = {
    logTrace("Get Tasks") *>
    execCommand(if (active) BoincClient.Command.GetActiveResults else BoincClient.Command.GetResults).map(xml =>
      (for (task <- xml \ "results" \ "result") yield task.toResult).toList
    )
  }

  override def getHostInfo: IO[HostInfo] =
    logTrace("Get Hostinfo")*>
    execCommand(BoincClient.Command.GetHostInfo).map(_ \ "host_info" toHostInfo)

  override def isNetworkAvailable: IO[Boolean] =
    logTrace("Get NetworkStatus") *>
    execCommand(BoincClient.Command.GetNetworkAvailable).map(_ \ "success" xml_== <success/>)

  override def getDiskUsage: IO[DiskUsage] =
    logTrace("Get Diskusage") *>
    execCommand(BoincClient.Command.GetDiskUsage).map(_ \ "disk_usage_summary" toDiskUsage)

  override def getProjects: IO[List[Project]] = 
    logTrace("Get Projects") *>
    execCommand(BoincClient.Command.GetProjectStatus).map(_ \ "projects" toProjects)

  override def getState: IO[BoincState] =
    logTrace("Get State")  *>
    execCommand(BoincClient.Command.GetState).map(_ \ "client_state" toState)

  override def getFileTransfer: IO[List[FileTransfer]] =
    logTrace("Get FileTransfer")  *>
    execCommand(BoincClient.Command.GetFileTransfer).map(_ \ "file_transfers" toFileTransfers)

  override def workunit(project: String, name: String, action: WorkunitAction): IO[Boolean] =
    logTrace(s"Set Workunit $name to ${action.toString}") *>
    execAction(
      s"""<${action.toString}>
         |  <project_url>$project</project_url>
         |  <name>$name</name>
         |</${action.toString}>""".stripMargin
    ).map(_ \ "success" xml_== <success/>)

  override def project(url: String, action: ProjectAction): IO[Boolean] =
    logTrace(s"Set Project $url to state ${action.toString}") *>
    this.execAction(
      s"""<${action.toString}>
         |  <project_url>$url</project_url>
         |</${action.toString}>""".stripMargin
    ).map(_ \ "success" xml_== <success/>)

  override def getCCState: IO[CCState] =
    logTrace("Get CCState from " + address + ":" + port) *>
    execCommand(BoincClient.Command.GetCCStatus).map(_ \ "cc_status" toCCState)

  override def getGlobalPrefsOverride: IO[GlobalPrefsOverride] =
    logTrace("Get GlobalPrefsOverride for " + address + ":" + port) *>
    execCommand(BoincClient.Command.GetGlobalPrefsOverride).map(_ \ "global_preferences" toGlobalPrefs)

  override def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride): IO[Boolean] =
    logTrace("Setting GlobalPrefsOverride at " + address + ":" + port) *>
    IO {
      println(s"<set_global_prefs_override>${globalPrefsOverride.toXML}</set_global_prefs_override>")
    } *>
    this.execAction(s"<set_global_prefs_override>\n${globalPrefsOverride.toXML}\n</set_global_prefs_override>").map(res => {
      println(res)
      (res \ "success").xml_==(<success/>)
    })

  override def setRun(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    logTrace(s"Setting RunMode to $mode") *>
    this.execAction(
      s"""<set_run_mode>
         |  <${mode.toString}/>
         |  <duration>${duration.toFloat}</duration>
         |</set_run_mode>""".stripMargin
    ).map(_ \ "success" xml_== <success/>)

  override def setCpu(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    logTrace(s"Setting CPU to $mode") *>
    this.execAction(
      s"""<set_cpu_mode>
         |  <${mode.toString}/>
         |  <duration>${duration.toFloat}</duration>
         |</set_cpu_mode>""".stripMargin
    ).map(_ \ "success" xml_== <success/>)

  override def setGpu(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    logTrace(s"Setting GPU to $mode") *>
    this.execAction(
      s"""<set_gpu_mode>
         |  <${mode.toString}/>
         |  <duration>${duration.toFloat}</duration>
         |</set_gpu_mode>""".stripMargin
    ).map(_ \ "success" xml_== <success/>)

  override def setNetwork(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    logTrace(s"Setting Network to $mode") *>
    this.execAction(
      s"""<set_network_mode>
         |  <${mode.toString}/>
         |  <duration>${duration.toFloat}</duration>
         |</set_network_mode>""".stripMargin
    ).map(_ \ "success" xml_== <success/>)

  override def attachProject(url: String, authenticator: String, name: String): IO[Boolean] =
    logTrace(s"Attach Project $url") *>
    this.execAction(
        <project_attach>
          <project_url>{url}</project_url>
          <authenticator>{authenticator}</authenticator>
          <project_name>{name}</project_name>
        </project_attach>
    ).map(_ \ "success" xml_== <success/>)

  override def getMessages(seqno: Int): IO[List[Message]] =
    logTrace(s"Getting Messages (seqno=$seqno)") *>
    this.execHTMLAction(
      <get_messages>
        <seqno>{seqno}</seqno>
      </get_messages>
    ).map(_.toMessages)

  override def getNotices(seqno: Int): IO[List[Notice]] =
    logTrace(s"Getting Notices (seqno=$seqno)") *>
    this.execHTMLAction(
      <get_notices>
        <seqno>{seqno}</seqno>
      </get_notices>
    ).map(_.toNotices)

  override def retryFileTransfer(project: String, file: String): IO[Boolean] =
    logTrace(s"Retry file transfer for $project ($file)") *>
    this.execAction(
      <retry_file_transfer>
        <project_url>{project}</project_url>
        <filename>{file}</filename>
      </retry_file_transfer>
    ).map(_ \ "success" xml_== <success/>)

  override def getStatistics: IO[Statistics] =
    logTrace("Getting Statistics") *>
    execCommand(BoincClient.Command.GetStatistics).map(_.toStatistics)

  override def readGlobalPrefsOverride: IO[Boolean] =
    logTrace("Reading global prefs override") *>
    this.execCommand(BoincClient.Command.ReadGlobalPrefsFile).map{ response =>
      val succ = response \ "success" xml_== <success/>
      if (!succ) {
        val status = (response \ "status").text.toInt
      }
      // ???
      succ
    }

  def isAuthenticated: Boolean = this.authenticated

  def close(): Unit = {
    if (socket != null && socket.isConnected) {
      logTrace("Closing Socket").unsafeRunSync();
      socket.close()
    }

    authenticated = false
  }

  override def getVersion: IO[BoincVersion] = {
    if (version.isDefined) IO.pure(version.get)
    else getCCState.map(_ => version.get)
  }

  private def logTrace(msg: String): IO[Unit] = IO { logger.trace(s"[$address:$port]: " + msg) }

  override def getAppConfig(url: String): IO[AppConfig] =
    logTrace(s"Getting app_config.xml for $url") *>
    this.execAction(
      <get_app_config>
        <url>${url}</url>
      </get_app_config>
    ).map(node => AppConfigParser.fromXML(node \ "app_config"))

  // TODO: Implement AppConfig XML parsing
  override def setAppConfig(url: String, config: AppConfig): IO[Boolean] = ???

  override def quit(): IO[Unit] =
    logTrace("Sending quit") *>
    this.execAction(<quit/>) *>
    IO.unit

}