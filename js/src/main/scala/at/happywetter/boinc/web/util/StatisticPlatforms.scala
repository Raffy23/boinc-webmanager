package at.happywetter.boinc.web.util

import at.happywetter.boinc.web.util.I18N.Locale

/**
  * Created by: 
  *
  * @author Raphael
  * @version 06.09.2017
  */
object StatisticPlatforms {

  def freedc(cpid: String) = s"http://stats.free-dc.org/stats.php?page=hostbycpid&cpid=$cpid"
  def boincStats(cpid: String) = s"http://boincstats.com/${Locale.current}/stats/-1/host/detail/$cpid"

}
