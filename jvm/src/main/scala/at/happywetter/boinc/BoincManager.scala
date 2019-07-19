package at.happywetter.boinc

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentLinkedQueue, ScheduledExecutorService, TimeUnit}

import at.happywetter.boinc.util.{Logger, PooledBoincClient}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by: 
  *
  * @author Raphael
  * @version 19.07.2017
  */

class BoincManager(poolSize: Int, encoding: String)(implicit val scheduler: ScheduledExecutorService) extends Logger {

  private val timeout      = 5 minutes
  private val lastUsed     = new TrieMap[String, Long]()
  private val boincClients = new TrieMap[String, PooledBoincClient]()
  private val clientGroups = new TrieMap[String, ListBuffer[String]]()

  private val version = new AtomicLong(0L)
  val versionChangeListeners = new ConcurrentLinkedQueue[BoincManager => Unit]()

  // Close connections periodically
  scheduler.scheduleWithFixedDelay(() => {
    val curTime = System.currentTimeMillis() - timeout.toMillis

    lastUsed.foreach { case (name, time) =>
      if (time < curTime && boincClients(name).hasOpenConnections) {
        LOG.info("Close Socket Connection from " + name)
        boincClients(name).closeOpen(timeout.toMillis)
      }
    }
  }, timeout.toMinutes, timeout.toMinutes, TimeUnit.MINUTES )

  def get(name: String): Option[PooledBoincClient] = {
    // Update time only if Client exists
    if (lastUsed.get(name).nonEmpty )
      lastUsed(name) = System.currentTimeMillis()

    boincClients.get(name)
  }

  def add(name: String, client: PooledBoincClient): Unit = {
    if (!boincClients.keys.exists(_ == name)) {
      LOG.debug(s"Adding new client $name")

      lastUsed += (name -> System.currentTimeMillis())
      boincClients += (name -> client)

      updateVersion()
    }
  }

  def add(name: String, host: AppConfig.Host): Unit =
    add(name, new PooledBoincClient(poolSize, host.address, host.port, host.password, encoding))

  def add(config: (String,AppConfig.Host)): Unit = add(config._1, config._2)

  def getAllHostNames: Seq[String] = lastUsed.keys.toSeq

  def destroy(): Unit = boincClients.foreach { case (_, client) => client.closeAll() }

  def queryDeathCounter: Map[String, Int] =
    boincClients.map { case (name, client) => (name, client.deathCounter.get()) }.toMap

  def checkHealth: Future[Map[String, Boolean]] =
    Future.sequence(
      boincClients
        .map{ case(name, client) => client.checkConnection().map(state => (name, state))}
        .toList
    ).map(_.toMap)

  def getAddresses: List[(String, Int)] = boincClients.values.map(client => (client.address, client.port)).toList

  def getGroups: TrieMap[String, ListBuffer[String]] = clientGroups

  def getSerializableGroups: Map[String, List[String]] = getGroups.map{ case (key, value) => (key, value.toList)}.toMap

  def addGroup(group: String, hostname: String): Unit = {
    clientGroups.getOrElseUpdate(group, new ListBuffer[String]()) += hostname
    updateVersion()
  }

  def addGroup(group: String, hosts: List[String]): Unit = {
    clientGroups.getOrElseUpdate(group, new ListBuffer[String]()) ++= hosts
    updateVersion()
  }

  def getVersion: Long = version.get()

  private def updateVersion(): Unit = {
    version.set(System.currentTimeMillis())
    versionChangeListeners.forEach(_(this))
  }

}