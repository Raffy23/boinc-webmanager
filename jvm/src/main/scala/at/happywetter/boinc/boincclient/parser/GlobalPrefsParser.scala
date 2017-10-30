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

  private implicit class BoincDouble(val d: Double) {
    def toBoincDouble: String = d.formatted("%.6f").replace(",",".")
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
      <run_on_batteries>{globalPrefsOverride.runOnBatteries.toBoincString}</run_on_batteries>
      {/*<battery_charge_min_pct>{globalPrefsOverride.batteryChargeMinPct.toBoincDouble}</battery_charge_min_pct>*/}
      {/*<battery_max_temperature>{globalPrefsOverride.batteryMaxTemperature.toBoincDouble}</battery_max_temperature>*/}
      <run_if_user_active>{globalPrefsOverride.runIfUserActive.toBoincString}</run_if_user_active>
      <run_gpu_if_user_active>{globalPrefsOverride.runGPUIfUserActive.toBoincString}</run_gpu_if_user_active>
      <idle_time_to_run>{globalPrefsOverride.idleTimeToRun.toBoincDouble}</idle_time_to_run>
      <suspend_cpu_usage>{globalPrefsOverride.suspendCpuUsage.toBoincDouble}</suspend_cpu_usage>
      <leave_apps_in_memory>{globalPrefsOverride.leaveAppsInMemory.toBoincString}</leave_apps_in_memory>
      <dont_verify_images>{globalPrefsOverride.dontVerifyImages.toBoincString}</dont_verify_images>
      <work_buf_min_days>{globalPrefsOverride.workBufferMinDays.toBoincDouble}</work_buf_min_days>
      <work_buf_additional_days>{globalPrefsOverride.workBufferAdditionalDays.toBoincDouble}</work_buf_additional_days>
      <max_ncpus_pct>{globalPrefsOverride.maxNCpuPct.toBoincDouble}</max_ncpus_pct>
      <cpu_scheduling_period_minutes>{globalPrefsOverride.cpuSchedulingPeriodMinutes.toBoincDouble}</cpu_scheduling_period_minutes>
      <disk_interval>{globalPrefsOverride.diskInterval.toBoincDouble}</disk_interval>
      <disk_max_used_gb>{globalPrefsOverride.diskMaxUsedGB.toBoincDouble}</disk_max_used_gb>
      <disk_max_used_pct>{globalPrefsOverride.diskMaxUsedPct.toBoincDouble}</disk_max_used_pct>
      <ram_max_used_busy_pct>{globalPrefsOverride.ramUsedBusyPct.toBoincDouble}</ram_max_used_busy_pct>
      <ram_max_used_idle_pct>{globalPrefsOverride.ramUsedIdlePct.toBoincDouble}</ram_max_used_idle_pct>
      <max_bytes_sec_up>{globalPrefsOverride.maxBytesSecUpload.toBoincDouble}</max_bytes_sec_up>
      <max_bytes_sec_down>{globalPrefsOverride.maxBytesSecDownload.toBoincDouble}</max_bytes_sec_down>
      <cpu_usage_limit>{globalPrefsOverride.cpuUsageLimit.toBoincDouble}</cpu_usage_limit>
      <daily_xfer_limit_mb>{globalPrefsOverride.dailyXFerLimitMB.toBoincDouble}</daily_xfer_limit_mb>
      <daily_xfer_period_days>{globalPrefsOverride.dailyXFerPeriodDays.toBoincDouble}</daily_xfer_period_days>
      <network_wifi_only>{globalPrefsOverride.networkWifiOnly.toBoincString}</network_wifi_only>
      {/* Doesn't show up in BOINC MANAGER:
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
