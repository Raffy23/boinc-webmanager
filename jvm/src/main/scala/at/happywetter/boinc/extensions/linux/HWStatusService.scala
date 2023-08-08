package at.happywetter.boinc.extensions.linux

import java.util

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.languageFeature.postfixOps
import scala.xml.XML

import at.happywetter.boinc.extensions.linux.HWStatusService.Action
import at.happywetter.boinc.shared.extension.HardwareData.SensorsData

import cats.effect.IO
import fs2.io.process.{ProcessBuilder, Processes}
import fs2.text
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.Tracer

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.11.2017
  */
class HWStatusService(resolverBinary: String,
                      parameters: List[String],
                      cacheTimeout: Long,
                      val actions: Map[String, Action],
                      val globalActions: Map[String, Action]
):
  import HWStatusService._

  // TODO: Query from config file
  private val TASK_TIMEOUT: FiniteDuration = 30.seconds

  private val hostData = new TrieMap[String, (Timestamp, CPUFrequency, SensorsData)]()
  private val logger = Slf4jLogger.getLoggerFromClass[IO](HWStatusService.getClass)

  private def executeResolver(host: String): IO[(CPUFrequency, SensorsData)] =
    ProcessBuilder(resolverBinary, parameters ::: List(host))
      .spawn[IO]
      .use { process =>
        process.stdout.through(text.utf8.decode).compile.string
      }
      .map { stdout =>
        val data = XML.load(stdout)

        (
          CpuFreqOutputParser.parse((data \ "cpu-freq").text),
          SensorsOutputParser.parse((data \ "sensors").text)
        )
      }

  def query(host: String): IO[(CPUFrequency, SensorsData)] =
    val cached = hostData.get(host)

    if cached.isEmpty then
      executeResolver(host).flatTap(data =>
        IO {
          hostData += (host -> (System.currentTimeMillis(), data._1, data._2))
        }
      )
    else
      val data = cached.get
      if data._1 + cacheTimeout < System.currentTimeMillis() then
        executeResolver(host).flatTap(data =>
          IO {
            hostData += (host -> (System.currentTimeMillis(), data._1, data._2))
          }
        )
      else IO.pure((data._2, data._3))

  def executeAction(host: String, action: String)(implicit T: Tracer[IO]): IO[(Boolean, String)] = T
    .span("executeAction", Attribute("host", host), Attribute("action", action))
    .surround(
      logger.info(s"Executing action $action for $host") *>
        this.executeAction(actions(action).command, actions(action).args ++ Seq(host))
    )

  def executeGlobalAction(action: String)(implicit T: Tracer[IO]): IO[(Boolean, String)] = T
    .span("executeGlobalAction", Attribute("action", action))
    .surround(
      logger.info(s"Executing global action $action") *>
        this.executeAction(globalActions(action).command, globalActions(action).args)
    )

  private def executeAction(command: String, args: Seq[String]): IO[(Boolean, String)] =
    ProcessBuilder(command, args: _*)
      .spawn[IO]
      .use { process =>
        IO.race(
          IO.sleep(TASK_TIMEOUT),
          for {
            out <- process.stdout.through(text.utf8.decode).compile.string
            _ <- logger.debug(out)
            exitCode <- process.exitValue
          } yield (exitCode, out)
        ).flatMap {
          case Left(_) =>
            logger.warn(s"Aborted action \"$command ${args.mkString(" ")}\" after $TASK_TIMEOUT").map(_ => (-255, ""))
          case Right(value) => IO.pure(value)
        }
      }
      .flatTap { (exitCode, stdout) =>
        if exitCode != 0 then logger.warn(s"Process exited with non-zero exit code: $exitCode")
        else IO.unit
      }
      .map { (exitCode, stdout) =>
        (exitCode == 0, stdout)
      }
      .handleError { case e: Throwable =>
        e.printStackTrace()
        (false, "Execution crashed")
      }

object HWStatusService:
  type Timestamp = Long
  type CPUFrequency = Double

  type Action = at.happywetter.boinc.shared.extension.HardwareData.Action
