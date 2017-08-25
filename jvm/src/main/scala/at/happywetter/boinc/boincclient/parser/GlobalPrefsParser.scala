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

  def fromXML(node: NodeSeq) = GlobalPrefsOverride(
    (node \ "run_on_batteries").text.toInt == 1,
    (node \ "battery_charge_min_pct").text.toDouble,
    (node \ "battery_max_temperature").text.toDouble,
    (node \ "run_if_user_active").text.toInt == 1,
    (node \ "run_gpu_if_user_active").text.toInt == 1,
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
    (node \ "network_wifi_only").text.toInt==1,
    (node \ "start_hour").theSeq.zip(node \ "end_hour").map { case (start, end) => (start.text.toDouble, end.text.toDouble) }.toArray,
    (node \ "net_start_hour").theSeq.zip(node \ "net_end_hour").map { case (start, end) => (start.text.toDouble, end.text.toDouble) }.toArray,
  )

  //TODO: Implement this
  def toXML(globalPrefsOverride: GlobalPrefsOverride): NodeSeq =
    <global_preferences>
      <run_on_batteries>${ if(globalPrefsOverride.runOnBatteries) 1 else 0 }</run_on_batteries>
      <battery_charge_min_pct>${globalPrefsOverride.batteryChargeMinPct}</battery_charge_min_pct>
    </global_preferences>

}
