package at.happywetter.boinc.shared

/**
 * Created by: 
 *
 * @author Raphael
 * @version 09.07.2020
 */
package object rpc {

  final case class HostDetails(name: String, address: String, port: Int, password: String,
                               addedBy: String, errors: Int)

  final case class DashboardDataEntry(state: boincrpc.BoincState, fileTransfers: List[boincrpc.FileTransfer])

}
