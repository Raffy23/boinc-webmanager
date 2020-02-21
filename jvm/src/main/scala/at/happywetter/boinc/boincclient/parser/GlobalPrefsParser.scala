package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.boincrpc.GlobalPrefsOverride

import scala.util.Try
import scala.xml.{NodeSeq, Text}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object GlobalPrefsParser {

  private implicit class ScalaBoolean(val b: Boolean) {
    def toBoincString: String = if(b) "1" else "0"
  }

  private implicit class BoincDouble(val d: Double) {
    def toBoincDouble: String = d.formatted("%.6f").replace(",",".")
  }

  private implicit class TryParser(string: String) {
    def tryToDouble: Double = Try(string.toDouble).getOrElse(-1D)
  }

  def fromXML(node: NodeSeq) = GlobalPrefsOverride(
    (node \ "run_on_batteries").toScalaBoolean,
    (node \ "battery_charge_min_pct").toScalaDouble,
    (node \ "battery_max_temperature").toScalaDouble,
    (node \ "run_if_user_active").toScalaBoolean,
    (node \ "run_gpu_if_user_active").toScalaBoolean,
    (node \ "idle_time_to_run").text.toDouble,
    (node \ "suspend_cpu_usage").text.toDouble,
    (node \ "leave_apps_in_memory").text.toInt == 1,
    (node \ "dont_verify_images").text.toInt==1,
    (node \ "work_buf_min_days").text.toDouble,
    (node \ "work_buf_additional_days").text.toDouble,
    (node \ "max_ncpus_pct").text.toDouble,
    (node \ "cpu_scheduling_period_minutes").text.toDouble,
    (node \ "disk_interval").text.toDouble,
    (node \ "disk_max_used_gb").text.toDouble,
    (node \ "disk_max_used_pct").text.toDouble,
    (node \ "disk_min_free_gb").text.toDouble,
    (node \ "ram_max_used_busy_pct").text.toDouble,
    (node \ "ram_max_used_idle_pct").text.toDouble,
    (node \ "max_bytes_sec_up").text.toDouble,
    (node \ "max_bytes_sec_down").text.toDouble,
    (node \ "cpu_usage_limit").text.toDouble,
    (node \ "daily_xfer_limit_mb").text.toDouble,
    (node \ "daily_xfer_period_days").text.toInt,
    (node \ "network_wifi_only").tryToInt==1,
    (
      (node \ "start_hour").head.child.collect{ case Text(t) => t}.mkString(" ").tryToDouble,
      (node \ "end_hour").head.child.collect{ case Text(t) => t}.mkString(" ").tryToDouble
    ),
    (
      (node \ "net_start_hour").head.child.collect{ case Text(t) => t}.mkString(" ").tryToDouble,
      (node \ "net_end_hour").head.child.collect{ case Text(t) => t}.mkString(" ").tryToDouble
    ),
    (node \ "start_hour").theSeq.zip(node \ "end_hour").map { case (start, end) => (start.text.toDouble, end.text.toDouble) }.toList,
    (node \ "net_start_hour").theSeq.zip(node \ "net_end_hour").map { case (start, end) => (start.text.toDouble, end.text.toDouble) }.toList,
  )

  def toXML(globalPrefsOverride: GlobalPrefsOverride): NodeSeq = {
    val g = globalPrefsOverride
    
    <global_preferences>
      <run_on_batteries>{g.runOnBatteries.toBoincString}</run_on_batteries>
      {/*<battery_charge_min_pct>{g.batteryChargeMinPct.toBoincDouble}</battery_charge_min_pct>*/}
      {/*<battery_max_temperature>{g.batteryMaxTemperature.toBoincDouble}</battery_max_temperature>*/}
      <run_if_user_active>{g.runIfUserActive.toBoincString}</run_if_user_active>
      <run_gpu_if_user_active>{g.runGPUIfUserActive.toBoincString}</run_gpu_if_user_active>
      <idle_time_to_run>{g.idleTimeToRun.toBoincDouble}</idle_time_to_run>
      <suspend_cpu_usage>{g.suspendCpuUsage.toBoincDouble}</suspend_cpu_usage>
      <leave_apps_in_memory>{g.leaveAppsInMemory.toBoincString}</leave_apps_in_memory>
      <dont_verify_images>{g.dontVerifyImages.toBoincString}</dont_verify_images>
      <work_buf_min_days>{g.workBufferMinDays.toBoincDouble}</work_buf_min_days>
      <work_buf_additional_days>{g.workBufferAdditionalDays.toBoincDouble}</work_buf_additional_days>
      <max_ncpus_pct>{g.maxNCpuPct.toBoincDouble}</max_ncpus_pct>
      <cpu_scheduling_period_minutes>{g.cpuSchedulingPeriodMinutes.toBoincDouble}</cpu_scheduling_period_minutes>
      <disk_interval>{g.diskInterval.toBoincDouble}</disk_interval>
      <disk_max_used_gb>{g.diskMaxUsedGB.toBoincDouble}</disk_max_used_gb>
      <disk_max_used_pct>{g.diskMaxUsedPct.toBoincDouble}</disk_max_used_pct>
      <ram_max_used_busy_pct>{g.ramUsedBusyPct.toBoincDouble}</ram_max_used_busy_pct>
      <ram_max_used_idle_pct>{g.ramUsedIdlePct.toBoincDouble}</ram_max_used_idle_pct>
      <max_bytes_sec_up>{g.maxBytesSecUpload.toBoincDouble}</max_bytes_sec_up>
      <max_bytes_sec_down>{g.maxBytesSecDownload.toBoincDouble}</max_bytes_sec_down>
      <cpu_usage_limit>{g.cpuUsageLimit.toBoincDouble}</cpu_usage_limit>
      <daily_xfer_limit_mb>{g.dailyXFerLimitMB.toBoincDouble}</daily_xfer_limit_mb>
      <daily_xfer_period_days>{g.dailyXFerPeriodDays.toBoincDouble}</daily_xfer_period_days>
      <network_wifi_only>{g.networkWifiOnly.toBoincString}</network_wifi_only>
      {
        if (g.cpuTime._1 > -1 && g.cpuTime._2 > -1) {
          <start_hour>{g.cpuTime._1.toBoincDouble}</start_hour>
          <end_hour>{g.cpuTime._2.toBoincDouble}</end_hour>
        }
      }
      {
        if (g.netTime._1 > -1 && g.netTime._2 > -1) {
          <net_start_hour>{g.netTime._1.toBoincDouble}</net_start_hour>
          <net_end_hour>{g.netTime._2.toBoincDouble}</net_end_hour>
        }
      }

      
      {/* Seems Wrong:
      <day_prefs>
        {
          globalPrefsOverride.cpuTimes.zip(globalPrefsOverride.netTimes).zipWithIndex.map {
            case (((start, end), (net_start, net_end)), idx) =>
              <day_of_week>
                {idx}
                <start_hour>{start}</start_hour>
                <end_hour>{end}</end_hour>
                <net_start_hour>{net_start}</net_start_hour>
                <net_end_hour>{net_end}</net_end_hour>
              </day_of_week>
          }
        }
      </day_prefs>*/}
    </global_preferences>
  }

}
