package at.happywetter.boinc

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import at.happywetter.boinc.boincclient.BoincClient

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.07.2017
  */

class BoincManager(implicit val scheduler: ScheduledExecutorService) {

  private val timeout      = 5 minutes
  private val lastUsed     = new TrieMap[String, Long]()
  private val boincClients = new TrieMap[String, BoincClient]()

  // Close connections periodically
  scheduler.scheduleWithFixedDelay(() => {
    val curTime = System.currentTimeMillis() - timeout.toMillis

    lastUsed.foreach { case (name, time) => {
      if(time < curTime)
        boincClients(name).close()
    }}
  }, timeout.toMinutes, timeout.toMinutes, TimeUnit.MINUTES )

  def get(name: String): Option[BoincClient] = {
    // Update time only if Client exists
    if (lastUsed.get(name).nonEmpty )
      lastUsed(name) = System.currentTimeMillis()

    boincClients.get(name)
  }

  def add(name: String, client: BoincClient): Unit = {
    lastUsed += (name -> System.currentTimeMillis())
    boincClients += (name -> client)
  }

  def getAllHostNames: Seq[String] = lastUsed.keys.toSeq

}
