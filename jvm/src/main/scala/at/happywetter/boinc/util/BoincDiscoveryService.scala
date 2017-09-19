package at.happywetter.boinc.util

import java.net.{InetSocketAddress, Socket, SocketAddress}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.09.2017
  */
class BoincDiscoveryService(start: IP, end: IP) {

  def search(found: (IP) => Unit = (_) => Unit): Future[List[IP]] =
    Future.sequence(
      (start to end).map(ip => {
        Future {
          val socket = new Socket()
          socket.connect(new InetSocketAddress(ip.toString, 31416), 500)
          socket.close()
          found(ip)
          (ip, true)
        }.recover {
          case _: Exception => (ip, false)
        }
      }).toList
    ).map(_.filter { case (_, found) => found }).map(_.map { case (ip, _) => ip })

}
