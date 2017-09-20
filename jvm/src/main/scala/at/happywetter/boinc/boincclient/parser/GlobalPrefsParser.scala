package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.GlobalPrefsOverride

import scala.xml.NodeSeq

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
    (node \ "start_hour").theSeq.zip(node \ "end_hour").map { case (start, end) => (start.text.toDouble, end.text.toDouble) }.toList,
    (node \ "net_start_hour").theSeq.zip(node \ "net_end_hour").map { case (start, end) => (start.text.toDouble, end.text.toDouble) }.toList,
  )

  def toXML(globalPrefsOverride: GlobalPrefsOverride): NodeSeq =
    <global_preferences>
      <run_on_batteries>${globalPrefsOverride.runOnBatteries.toBoincString}</run_on_batteries>
      <battery_charge_min_pct>${globalPrefsOverride.batteryChargeMinPct}</battery_charge_min_pct>
      <battery_max_temperature>${globalPrefsOverride.batteryMaxTemperature}</battery_max_temperature>
      <run_if_user_active>${globalPrefsOverride.runIfUserActive}</run_if_user_active>
      <run_gpu_if_user_active>${globalPrefsOverride.runGPUIfUserActive}</run_gpu_if_user_active>
      <idle_time_to_run>${globalPrefsOverride.idleTimeToRun}</idle_time_to_run>
      <suspend_cpu_usage>${globalPrefsOverride.suspendCpuUsage}</suspend_cpu_usage>
      <leave_apps_in_memory>${globalPrefsOverride.leaveAppsInMemory}</leave_apps_in_memory>
      <dont_verify_images>${globalPrefsOverride.dontVerifyImages}</dont_verify_images>
      <work_buf_min_days>${globalPrefsOverride.workBufferMinDays}</work_buf_min_days>
      <work_buf_additional_days>${globalPrefsOverride.workBufferAdditionalDays}</work_buf_additional_days>
      <max_ncpus_pct>${globalPrefsOverride.maxNCpuPct}</max_ncpus_pct>
      <cpu_scheduling_period_minutes>${globalPrefsOverride.cpuSchedulingPeriodMinutes}</cpu_scheduling_period_minutes>
      <disk_interval>${globalPrefsOverride.diskInterval}</disk_interval>
      <disk_max_used_gb>${globalPrefsOverride.diskMaxUsedGB}</disk_max_used_gb>
      <disk_max_used_pct>${globalPrefsOverride.diskMaxUsedPct}</disk_max_used_pct>
      <ram_max_used_busy_pct>${globalPrefsOverride.ramUsedBusyPct}</ram_max_used_busy_pct>
      <ram_max_used_idle_pct>${globalPrefsOverride.ramUsedIdlePct}</ram_max_used_idle_pct>
      <max_bytes_sec_up>${globalPrefsOverride.maxBytesSecUpload}</max_bytes_sec_up>
      <max_bytes_sec_down>${globalPrefsOverride.maxBytesSecDownload}</max_bytes_sec_down>
      <cpu_usage_limit>${globalPrefsOverride.cpuUsageLimit}</cpu_usage_limit>
      <daily_xfer_limit_mb>${globalPrefsOverride.dailyXFerLimitMB}</daily_xfer_limit_mb>
      <daily_xfer_period_days>${globalPrefsOverride.dailyXFerPeriodDays}</daily_xfer_period_days>
      <network_wifi_only>${globalPrefsOverride.networkWifiOnly}</network_wifi_only>
      <day_prefs>
        ${
          globalPrefsOverride.cpuTimes.zip(globalPrefsOverride.netTimes).zipWithIndex.map {
            case (((start, end), (net_start, net_end)), idx) =>
              <day_of_week>
                ${idx}
                <start_hour>${start}</start_hour>
                <end_hour>${end}</end_hour>
                <net_start_hour>${net_start}</net_start_hour>
                <net_end_hour>${net_end}</net_end_hour>
              </day_of_week>
          }
        }
      </day_prefs>
    </global_preferences>

}
