package at.happywetter.boinc.shared.boincrpc

import at.happywetter.boinc.shared.rpc.DashboardDataEntry

/**
 * Created by: 
 *
 * @author Raphael
 * @version 22.11.2020
 */
trait WebmanagerClient[F[_]] extends BoincCoreClient[F]:

  def getDashboardData: F[DashboardDataEntry]
