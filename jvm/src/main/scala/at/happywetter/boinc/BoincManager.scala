package at.happywetter.boinc

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.util.PooledBoincClient
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.07.2017
  */

class BoincManager(poolSize: Int)(implicit val scheduler: ScheduledExecutorService) {

  private val timeout      = 5 minutes
  private val lastUsed     = new TrieMap[String, Long]()
  private val boincClients = new TrieMap[String, PooledBoincClient]()

  // Close connections periodically
  scheduler.scheduleWithFixedDelay(() => {
    val curTime = System.currentTimeMillis() - timeout.toMillis

    lastUsed.foreach { case (name, time) => {
      if (time < curTime && boincClients(name).hasOpenConnections) {
        BoincManager.logger.debug("Close Socket Connection from " + name)
        boincClients(name).closeOpen()
      }
    }}
  }, timeout.toMinutes, timeout.toMinutes, TimeUnit.MINUTES )

  def get(name: String): Option[PooledBoincClient] = {
    // Update time only if Client exists
    if (lastUsed.get(name).nonEmpty )
      lastUsed(name) = System.currentTimeMillis()

    boincClients.get(name)
  }

  def add(name: String, client: PooledBoincClient): Unit = {
    lastUsed += (name -> System.currentTimeMillis())
    boincClients += (name -> client)
  }

  def add(name: String, host: AppConfig.Host): Unit =
    add(name, new PooledBoincClient(poolSize, host.address, host.port, host.password))

  def add(config: (String,AppConfig.Host)): Unit = add(config._1, config._2)

  def getAllHostNames: Seq[String] = lastUsed.keys.toSeq

  def destroy(): Unit = boincClients.foreach { case (_, client) => client.closeAll() }

}
object BoincManager {
  protected val logger: Logger = LoggerFactory.getLogger(BoincManager.getClass.getCanonicalName)
}