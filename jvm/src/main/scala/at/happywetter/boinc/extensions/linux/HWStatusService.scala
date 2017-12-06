package at.happywetter.boinc.extensions.linux

import java.util

import at.happywetter.boinc.shared.HardwareData.SensorsData

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.XML

/**
  * Created by: 
  *
  * @author Raphael
  * @version 02.11.2017
  */
class HWStatusService(resolverBinary: String, parameters: List[String], cacheTimeout: Long) {

  type Timestamp = Long
  type CPUFrequency = Double

  private val hostData = new TrieMap[String, (Timestamp, CPUFrequency, SensorsData)]()

  private def executeResolver(host: String): (CPUFrequency, SensorsData) = {
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
  }

  def query(host: String): Future[(CPUFrequency, SensorsData)] = Future {
    val cached = hostData.get(host)

    if (cached.isEmpty) {
      val data = executeResolver(host)
      hostData += (host -> (System.currentTimeMillis(), data._1, data._2))

      data
    } else {
      val data = cached.get
      if (data._1 + cacheTimeout < System.currentTimeMillis()) {
        val data = executeResolver(host)
        hostData += (host -> (System.currentTimeMillis(), data._1, data._2))

        data
      } else {
        (data._2, data._3)
      }
    }
  }
}
