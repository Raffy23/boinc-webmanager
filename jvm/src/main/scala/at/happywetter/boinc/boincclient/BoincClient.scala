package at.happywetter.boinc.boincclient

import at.happywetter.boinc.BuildInfo
import at.happywetter.boinc.boincclient.BoincClient.CLIENT_AUTH_TIMEOUT
import at.happywetter.boinc.boincclient.parser.AppConfigParser
import at.happywetter.boinc.boincclient.parser.BoincParserUtils._
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.boincrpc.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared.boincrpc._
import cats.effect.kernel.Resource
import cats.effect.std.Mutex
import cats.effect.{IO, Ref}
import com.comcast.ip4s.{Host, Port, SocketAddress}
import fs2.concurrent.SignallingRef
import fs2.io.net.{Network, Socket}
import fs2.{Chunk, text}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.Tracer

import scala.concurrent.TimeoutException
import scala.concurrent.duration.DurationInt
import scala.language.{existentials, postfixOps}
import scala.xml.{NodeSeq, XML}

/**
 * Basic Class for the BOINC Communication
 *
 * Created by
 * @author Raphael Ludwig
 * @version 08.07.2016
 */
object BoincClient:

  private val CLIENT_IDENTIFER    = s"BOINC WebManager ${BuildInfo.version}"
  private val CLIENT_VERSION      = BoincVersion(major = 7, minor = 16, release = 6)
  private val CLIENT_AUTH_TIMEOUT = 5 seconds

  object Mode extends Enumeration:
    val Always  = Value("<always/>")
    val Never   = Value("<never/>")
    val Auto    = Value("<auto/>")
    val Restore = Value("<restore/>")

  object Command extends Enumeration:
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


  def tryConnect(address: String, port: Int = 31416, password: String)(implicit T: Tracer[IO]): Resource[IO, Option[BoincClient]] =
    BoincClient(address, port, password)
      .map(client => Some(client))
      .handleErrorWith:
        (ex: Throwable) =>
          ex match
            case _: RuntimeException => Resource.pure[IO, Option[BoincClient]](Option.empty)
            case t: TimeoutException => Resource.eval(IO.raiseError(t))


  def apply(address: String, port: Int = 31416, password: String)(implicit T: Tracer[IO]): Resource[IO, BoincClient] =

    for {
      socket <- Network[IO].client(SocketAddress(Host.fromString(address).get, Port.fromInt(port).get)) // maybe move actual connection to func
      client <- Resource.make(
        for {
          logger  <- Slf4jLogger.fromClass[IO](BoincClient.getClass)
          mutex   <- Mutex[IO]
          version <- Ref.of[IO, Option[BoincVersion]](Option.empty)
          signal  <- SignallingRef[IO, Boolean](false)

          client <- IO { new BoincClient(socket, mutex, version, logger, s"[$address:$port]: ", signal) }

          _ <- client
            .authenticate(password)
            .ifM(IO.unit, IO.raiseError(new RuntimeException("Unable to authenticate core client!")))

          _ <- client.exchangeVersion().flatMap(v => version.set(Some(v)))
        }

        yield client
      )(_.close())
    }

    yield client



class BoincClient private (socket: Socket[IO],
                           mutex: Mutex[IO],
                           val version: Ref[IO, Option[BoincVersion]],
                           logger: SelfAwareStructuredLogger[IO],
                           logHeader: String,
                           shutdownSignal: SignallingRef[IO, Boolean])(implicit T: Tracer[IO])
  extends BoincCoreClient[IO] {

  private val readXML: IO[(Int, NodeSeq)] =
    socket
      .reads
      .interruptScope
      .takeWhile(_ != '\u0003')
      .through(text.utf8.decode)
      .interruptWhen(shutdownSignal)
      .compile
      .string
      .map(content =>
        (content.length, XML.loadString(content))
      )

  private val readHMTL: IO[(Int, Document)] =
    socket
      .reads
      .interruptScope
      .takeWhile(_ != '\u0003')
      .interruptWhen(shutdownSignal)
      .compile
      .toVector
      .map { vec =>
        // TODO: Sometimes the encoding is different from different websites
        //       not all are iso or utf8, wrongly displayed content may
        //       be the result of this:
        val content = new String(vec.toArray, "iso-8859-1")
        (content.length, Jsoup.parse(content))
      }

  private def doRPC[T](data: String, receiveMethod: IO[(Int, T)]): IO[T] = T.span("doRPC").use { span =>
    mutex
      .lock
      .use[T](_ => for {

        remoteAddress  <- socket.remoteAddress
        _            <- span.addAttribute(Attribute("destination.address", remoteAddress.host.toString))
        _            <- span.addAttribute(Attribute("destination.port", remoteAddress.port.value.toLong))
        _            <- span.addAttribute(Attribute("request.size", data.length.toLong + 55L /* header size */))

        _            <- socket.write {
          val builder = Chunk.newBuilder[Byte]

          builder += Chunk.array("<boinc_gui_rpc_request>\n".getBytes())
          builder += Chunk.array(data.getBytes())
          builder += Chunk.array("\n</boinc_gui_rpc_request>\n".getBytes())
          builder += Chunk.array("\u0003".getBytes())

          builder.result
        }

        resp         <- receiveMethod

        _            <- span.addAttribute(Attribute("response.size",resp._1.toLong))
    }
      yield resp._2)
  }

  private def xmlRpc(data: String): IO[NodeSeq]   = doRPC(data, readXML)
  private def htmlRpc(data: String): IO[Document] = doRPC(data, readHMTL)

  private def authenticate(password: String): IO[Boolean] = T.span("authenticate").use { span =>
    for {
      // Assume clients that don't answer with a nonce in less then 5s are
      // actually clients, should probably be configurable
      nonce <- xmlRpc("<auth1>")
        .map(_ \ "nonce" text)
        .timeout(CLIENT_AUTH_TIMEOUT)
        .onError(_ => span.addEvent("Authentication timeout !"))

      result <- xmlRpc("<auth2>\n<nonce_hash>" + BoincCryptoHelper.md5(nonce + password) + "</nonce_hash>\n</auth2>")

      auth <- IO:
        (result \ "_").xml_==(<authorized/>)
    }
    yield auth
  }

  @inline def execCommand(cmd: BoincClient.Command.Value): IO[NodeSeq] = xmlRpc(cmd.toString)
  @inline def execHtmlCommand(cmd: BoincClient.Command.Value): IO[Document] = htmlRpc(cmd.toString)

  @inline private def execAction(action: NodeSeq): IO[NodeSeq] = xmlRpc(action.toString())
  @inline private def execAction(action: String): IO[NodeSeq] = xmlRpc(action)

  @inline private def execHTMLAction(action: NodeSeq): IO[Document] = htmlRpc(action.toString())
  @inline private def execHTMLAction(action: String): IO[Document] = htmlRpc(action)

  private def exchangeVersion(): IO[BoincVersion] =
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

  override def getTasks(active: Boolean = true): IO[List[Result]] = T.span("getTasks", Attribute("active", active)).surround:
    execCommand(if active then BoincClient.Command.GetActiveResults else BoincClient.Command.GetResults).map(xml =>
      (for task <- xml \ "results" \ "result" yield task.toResult).toList
    )

  override def getHostInfo: IO[HostInfo] = T.span("getHostInfo").surround:
    execCommand(BoincClient.Command.GetHostInfo).map(_ \ "host_info" toHostInfo)

  override def isNetworkAvailable: IO[Boolean] =  T.span("isNetworkAvailable").surround:
    execCommand(BoincClient.Command.GetNetworkAvailable).map(_ \ "success" xml_== <success/>)

  override def getDiskUsage: IO[DiskUsage] =  T.span("getDiskUsage").surround:
    execCommand(BoincClient.Command.GetDiskUsage).map(_ \ "disk_usage_summary" toDiskUsage)

  override def getProjects: IO[List[Project]] = T.span("getProjects").surround:
    execCommand(BoincClient.Command.GetProjectStatus).map(_ \ "projects" toProjects)

  override def getState: IO[BoincState] = T.span("getState").surround:
    execCommand(BoincClient.Command.GetState).map(_ \ "client_state" toState)

  override def getFileTransfer: IO[List[FileTransfer]] = T.span("getFileTransfer").surround:
    execCommand(BoincClient.Command.GetFileTransfer).map(_ \ "file_transfers" toFileTransfers)

  override def workunit(project: String, name: String, action: WorkunitAction): IO[Boolean] =
    T.span("workunit", Attribute("project", project), Attribute("name", name), Attribute("action", action.toString)).surround:
      execAction(
        s"""<${action.toString}>
           |  <project_url>$project</project_url>
           |  <name>$name</name>
           |</${action.toString}>""".stripMargin
      ).map(_ \ "success" xml_== <success/>)

  override def project(url: String, action: ProjectAction): IO[Boolean] =
    T.span("project").surround:
      this.execAction(
        s"""<${action.toString}>
           |  <project_url>$url</project_url>
           |</${action.toString}>""".stripMargin
      ).map(_ \ "success" xml_== <success/>)

  override def getCCState: IO[CCState] =
    T.span("getCCState").surround:
      execCommand(BoincClient.Command.GetCCStatus).map(_ \ "cc_status" toCCState)

  override def getGlobalPrefsOverride: IO[GlobalPrefsOverride] =
    T.span("getGlobalPrefsOverride").surround:
      execCommand(BoincClient.Command.GetGlobalPrefsOverride).map(_ \ "global_preferences" toGlobalPrefs)

  override def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride): IO[Boolean] =
    T.span("setGlobalPrefsOverride").surround:
      this.execAction(s"<set_global_prefs_override>\n${globalPrefsOverride.toXML}\n</set_global_prefs_override>").map(res => {
        (res \ "success").xml_==(<success/>)
      })

  override def setRun(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    T.span("setRun").surround:
      this.execAction(
        s"""<set_run_mode>
           |  <${mode.toString}/>
           |  <duration>${duration.toFloat}</duration>
           |</set_run_mode>""".stripMargin
      ).map(_ \ "success" xml_== <success/>)

  override def setCpu(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    T.span("setCpu").surround:
      this.execAction(
        s"""<set_cpu_mode>
           |  <${mode.toString}/>
           |  <duration>${duration.toFloat}</duration>
           |</set_cpu_mode>""".stripMargin
      ).map(_ \ "success" xml_== <success/>)

  override def setGpu(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    T.span("setGpu").surround:
      this.execAction(
        s"""<set_gpu_mode>
           |  <${mode.toString}/>
           |  <duration>${duration.toFloat}</duration>
           |</set_gpu_mode>""".stripMargin
      ).map(_ \ "success" xml_== <success/>)

  override def setNetwork(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    T.span("setNetwork").surround:
      this.execAction(
        s"""<set_network_mode>
           |  <${mode.toString}/>
           |  <duration>${duration.toFloat}</duration>
           |</set_network_mode>""".stripMargin
      ).map(_ \ "success" xml_== <success/>)

  override def attachProject(url: String, authenticator: String, name: String): IO[Boolean] =
    T.span("attachProject").surround:
      this.execAction(
        <project_attach>
          <project_url>
            {url}
          </project_url>
          <authenticator>
            {authenticator}
          </authenticator>
          <project_name>
            {name}
          </project_name>
        </project_attach>
      ).map(_ \ "success" xml_== <success/>)

  override def getMessages(seqno: Int): IO[List[Message]] =
    T.span("getMessages").surround:
      this.execHTMLAction(
        <get_messages>
          <seqno>
            {seqno}
          </seqno>
        </get_messages>
      ).map(_.toMessages)

  override def getNotices(seqno: Int): IO[List[Notice]] =
    T.span("getNotices").surround:
      this.execHTMLAction(
        <get_notices>
          <seqno>
            {seqno}
          </seqno>
        </get_notices>
      ).map(_.toNotices)

  override def retryFileTransfer(project: String, file: String): IO[Boolean] =
    T.span("retryFileTransfer").surround:
      this.execAction(
        <retry_file_transfer>
          <project_url>
            {project}
          </project_url>
          <filename>
            {file}
          </filename>
        </retry_file_transfer>
      ).map(_ \ "success" xml_== <success/>)

  override def getStatistics: IO[Statistics] =
    T.span("getStatistics").surround:
      execCommand(BoincClient.Command.GetStatistics).map(_.toStatistics)

  override def readGlobalPrefsOverride: IO[Boolean] =
    T.span("readGlobalPrefsOverride").surround:
      this.execCommand(BoincClient.Command.ReadGlobalPrefsFile).map { response =>
        val succ = response \ "success" xml_== <success/>
        if !succ then
          val status = (response \ "status").text.toInt
        // ???
        succ
      }

  override def getVersion: IO[BoincVersion] =
    version.get.map(_.get)

  def close(): IO[Unit] = for {
    _ <- IO.println(s"CLOSING ${logHeader}")
    _ <- shutdownSignal.set(true)
    _ <- socket.endOfInput
    _ <- socket.endOfOutput
  }
  yield ()

  private def logTrace(msg: String): IO[Unit] = logger.trace(logHeader + msg)

  override def getAppConfig(url: String): IO[AppConfig] =
    T.span("getAppConfig").surround:
      this.execAction(
        <get_app_config>
          <url>$
            {url}
          </url>
        </get_app_config>
      ).map(node => AppConfigParser.fromXML(node \ "app_config"))

  // TODO: Implement AppConfig XML parsing
  override def setAppConfig(url: String, config: AppConfig): IO[Boolean] = ???

  override def quit(): IO[Unit] =
    logTrace("Sending quit") *>
      this.execAction(<quit/>) *>
      IO.unit

}