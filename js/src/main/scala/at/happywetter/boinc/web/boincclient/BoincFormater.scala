package at.happywetter.boinc.web.boincclient

import scala.scalajs.js.Date

/**
  * Created by: 
  *
  * @author Raphael
  * @version 17.08.2017
  */
object BoincFormater {

  def convertDate(date: Date): String = date.toLocaleDateString() + " " + date.toLocaleTimeString()

  def convertDate(unixtimestamp: Double): String = convertDate(new Date(unixtimestamp*1000))

  def convertTime(time: Double): String = {
    val day  = time.toInt / 86400
    val hour = time.toInt / 3600
    val min  = (time.toInt / 60) % 60
    val sec  = time.toInt % 60

    (if(day >0) s"$day T, " else "") ++
      s"${hour.formatted("%02d")}:${min.formatted("%02d")}:${sec.formatted("%02d")}"
  }

  def convertTime(time: String): Int = {
    if (time.length < 1)
      return 0

    val day =
      if(time.contains("T")) time.split("T")(0).toInt * 86400
      else 0

    val strRaw = time.split(":")
    val hour = (if (day > 0) strRaw(0).split(",")(1).toInt else strRaw(0).toInt) * 3600
    val min  = strRaw(1).toInt * 60
    val sec  = strRaw(2).toInt

    day + hour + min + sec
  }

  private val labels = List("B","KB","MB","GB","TB","PB")
  def convertSize(size: Double): String = {
    var value = size
    var cnt   = 0

    while ((value/1024) >= 1) {
      value = value / 1024
      cnt = cnt + 1
    }

    s"${value.formatted("%.1f")} ${labels(cnt)}"
  }

}