package at.happywetter.boinc

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentLinkedQueue, ScheduledExecutorService, TimeUnit}

import at.happywetter.boinc.BoincManager.{AddedBy, AddedByConfig, AddedByDiscovery, BoincClientEntry}
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
object BoincManager {

  trait AddedBy
  object AddedByConfig extends AddedBy
  object AddedByDiscovery extends AddedBy

  private case class BoincClientEntry(client: PooledBoincClient, addedBy: AddedBy, var lastUsed: Long)

}
class BoincManager(poolSize: Int, encoding: String)(implicit val scheduler: ScheduledExecutorService) extends Logger {

  private val timeout        = 5 minutes
  private val deathCollector = 30 minutes
  private val boincClients = new TrieMap[String, BoincClientEntry]()
  private val clientGroups = new TrieMap[String, ListBuffer[String]]()

  private val version = new AtomicLong(0L)
  val versionChangeListeners = new ConcurrentLinkedQueue[BoincManager => Unit]()

  // Close connections periodically
  scheduler.scheduleWithFixedDelay(() => {
    val curTime = System.currentTimeMillis() - timeout.toMillis

    boincClients.foreach { case (name, BoincClientEntry(client, _, time)) =>
      if (time < curTime && client.hasOpenConnections) {
        LOG.info("Close Socket Connection from " + name)
        client.closeOpen(timeout.toMillis)
      }
    }
  }, timeout.toMinutes, timeout.toMinutes, TimeUnit.MINUTES )

  // Throw out all dead boinc core clients
  scheduler.scheduleWithFixedDelay(() => {
    var somethingChanged = false
    boincClients.foreach {
      case (name, BoincClientEntry(client, AddedByConfig, _)) =>
        if (!client.hasOpenConnections && client.deathCounter.get() > 10) {
          LOG.info(s"Client '$name' does have over ${client.deathCounter.get()} errored connections, not removing added by config ...")
        }

      case (name, BoincClientEntry(client, AddedByDiscovery, _)) =>
        if (!client.hasOpenConnections && client.deathCounter.get() > 10) {
          client.checkConnection().map(isAvailable => if (!isAvailable) {
            LOG.info(s"Removing client '$name', has ${client.deathCounter.get()} errored connections and can't be reached!")
            boincClients.remove(name)

            somethingChanged = true
          })
        }
    }

    if (somethingChanged)
      updateVersion()

  }, deathCollector.toMinutes, deathCollector.toMinutes, TimeUnit.MINUTES)

  def get(name: String): Option[PooledBoincClient] = {
    // Update time only if Client exists
    if (boincClients.get(name).nonEmpty )
      boincClients(name).lastUsed = System.currentTimeMillis()

    boincClients.get(name).map(_.client)
  }

  def add(name: String, client: PooledBoincClient, addedBy: AddedBy): Unit = {
    if (!boincClients.keys.exists(_ == name)) {

      LOG.debug(s"Adding new client $name")
      boincClients += (name -> BoincClientEntry(client, addedBy, System.currentTimeMillis()))

      updateVersion()
    }
  }

  def add(name: String, host: AppConfig.Host): Unit =
    add(name, new PooledBoincClient(poolSize, host.address, host.port, host.password, encoding), AddedByConfig)

  def add(config: (String,AppConfig.Host)): Unit = add(config._1, config._2)

  def getAllHostNames: Seq[String] = boincClients.keys.toSeq

  def destroy(): Unit = boincClients.foreach { case (_, BoincClientEntry(client, _, _)) => client.closeAll() }

  def queryDeathCounter: Map[String, Int] =
    boincClients.map { case (name, BoincClientEntry(client, _, _)) => (name, client.deathCounter.get()) }.toMap

  def checkHealth: Future[Map[String, Boolean]] =
    Future.sequence(
      boincClients
        .map{ case(name, BoincClientEntry(client, _, _)) => client.checkConnection().map(state => (name, state))}
        .toList
    ).map(_.toMap)

  def getAddresses: List[(String, Int)] = boincClients.values.map{ case BoincClientEntry(client, _, _) => (client.address, client.port)}.toList

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