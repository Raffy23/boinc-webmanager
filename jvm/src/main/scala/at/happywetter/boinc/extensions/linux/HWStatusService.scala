package at.happywetter.boinc.extensions.linux

import java.util
import at.happywetter.boinc.shared.extension.HardwareData.SensorsData
import cats.effect.IO
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.collection.concurrent.TrieMap
import scala.xml.XML

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.11.2017
  */
class HWStatusService(resolverBinary: String,
                      parameters: List[String],
                      cacheTimeout: Long,
                      val actions: Map[String, Seq[String]]
):
  import HWStatusService._

  private val hostData = new TrieMap[String, (Timestamp, CPUFrequency, SensorsData)]()
  private val logger = Slf4jLogger.getLoggerFromClass[IO](HWStatusService.getClass)

  private def executeResolver(host: String): (CPUFrequency, SensorsData) =
    val procLine = new util.ArrayList[String]()
    procLine.add(resolverBinary)
    parameters.foreach(procLine.add)
    procLine.add(host)

    val pb = new ProcessBuilder(procLine)
    pb.redirectOutput(ProcessBuilder.Redirect.PIPE)

    val process = pb.start()
    process.waitFor()

    val data = XML.load(process.getInputStream)

    (
      CpuFreqOutputParser.parse((data \ "cpu-freq").text),
      SensorsOutputParser.parse((data \ "sensors").text)
    )

  def query(host: String): IO[(CPUFrequency, SensorsData)] = IO:
    val cached = hostData.get(host)

    if cached.isEmpty then
      val data = executeResolver(host)
      hostData += (host -> (System.currentTimeMillis(), data._1, data._2))

      data
    else
      val data = cached.get
      if data._1 + cacheTimeout < System.currentTimeMillis() then
        val data = executeResolver(host)
        hostData += (host -> (System.currentTimeMillis(), data._1, data._2))

        data
      else (data._2, data._3)

  def executeAction(host: String, action: String): IO[Boolean] =
    logger.info(s"Executing action $action for $host") *>
      IO.blocking {
        val procBuilder = new ProcessBuilder(actions(action) ++ Seq(host): _*)
        procBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE)
        procBuilder.redirectError(ProcessBuilder.Redirect.PIPE)

        val process = procBuilder.start()
        process.getInputStream.close()

        val exitCode = process.waitFor()

        exitCode
      }.flatMap { exitCode =>
        if exitCode == 0 then
          logger.warn(s"Process exited with non-zero exit code: $exitCode") *>
            IO.pure(true)
        else IO.pure(false)
      }.handleErrorWith { ex =>
        logger.error(s"Exception occured while exeuting action $action for host $host:\n${ex.getMessage}") *>
          IO.pure(false)
      }

object HWStatusService:
  type Timestamp = Long
  type CPUFrequency = Double
