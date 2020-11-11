package at.happywetter.boinc

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentLinkedQueue, ScheduledExecutorService, TimeUnit}

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.BoincManager.{AddedBy, AddedByConfig, AddedByDiscovery, AddedByUser, BoincClientEntry}
import at.happywetter.boinc.dto.DatabaseDTO.CoreClient
import at.happywetter.boinc.shared.rpc.HostDetails
import at.happywetter.boinc.util.{IP, Logger, PooledBoincClient}
import cats.effect.{Blocker, ContextShift, IO, Resource}

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
  object AddedByUser extends AddedBy

  private case class BoincClientEntry(client: PooledBoincClient, addedBy: AddedBy, var lastUsed: Long)

  def apply(config: Config, db: Database, blocker: Blocker)(implicit scheduler: ScheduledExecutorService, cS: ContextShift[IO]): Resource[IO, BoincManager] =
    Resource.fromAutoCloseable(
      IO.pure(new BoincManager(config.boinc.connectionPool, config.boinc.encoding, blocker)).map { hostManager =>
        config.boinc.hosts.foreach(hostManager.add)
        config.hostGroups.foreach{ case (group, hosts) => hostManager.addGroup(group, hosts)}

        import monix.execution.Scheduler.Implicits.global
        db.clients.queryAll().runSyncUnsafe().foreach(coreClient => {
          hostManager.add(
            coreClient.name,
            coreClient.address, coreClient.port, coreClient.password,
            coreClient.addedBy match {
              case CoreClient.ADDED_BY_DISCOVERY => AddedByDiscovery
              case CoreClient.ADDED_BY_USER      => AddedByUser
            }
          )
        })

        hostManager
      }
    )

}
class BoincManager(poolSize: Int, encoding: String, blocker: Blocker)(implicit val scheduler: ScheduledExecutorService, cS: ContextShift[IO]) extends Logger with AutoCloseable {

  // TODO: make these variable configurable ...
  private val timeout        = 5 minutes
  private val deathCollector = 30 minutes
  private val healthTimeout  = 14 minutes
  private val boincClients = new TrieMap[String, BoincClientEntry]()
  private val clientGroups = new TrieMap[String, ListBuffer[String]]()

  private val version = new AtomicLong(0L)
  val versionChangeListeners = new ConcurrentLinkedQueue[BoincManager => Unit]()

  // Close connections periodically
  private val autoCloser = scheduler.scheduleWithFixedDelay(() => {
    val curTime = System.currentTimeMillis() - timeout.toMillis

    boincClients.foreach { case (name, BoincClientEntry(client, _, time)) =>
      if (time < curTime && client.hasOpenConnections) {
        LOG.info("Close Socket Connection from " + name)
        client.closeOpen(timeout.toMillis)
      }
    }
  }, timeout.toMinutes, timeout.toMinutes, TimeUnit.MINUTES )

  // Throw out all dead boinc core clients
  private val connectionCloser = scheduler.scheduleWithFixedDelay(() => {
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

  private val healthChecker = scheduler.scheduleWithFixedDelay(() => {
    boincClients.values.map(entry =>
      cS.blockOn(blocker)(entry.client.checkConnection()).unsafeRunAsyncAndForget()
    )
  }, (1 minutes).toMinutes, healthTimeout.toMinutes, TimeUnit.MINUTES)

  def get(name: String): Option[PooledBoincClient] = {
    // Update time only if Client exists
    if (boincClients.contains(name) )
      boincClients(name).lastUsed = System.currentTimeMillis()

    boincClients.get(name).map(_.client)
  }

  def add(name: String, client: PooledBoincClient, addedBy: AddedBy): Unit = {
    if (!boincClients.contains(name)) {

      LOG.debug(s"Adding new client $name")
      boincClients += (name -> BoincClientEntry(client, addedBy, System.currentTimeMillis()))

      updateVersion()
    }
  }

  def add(name: String, address: String, port: Int, password: String, addedBy: AddedBy): Unit =
    add(name, new PooledBoincClient(poolSize, address, port, password, encoding, blocker), addedBy)

  protected def add(name: String, host: AppConfig.Host): Unit =
    add(name, new PooledBoincClient(poolSize, host.address, host.port, host.password, encoding, blocker), AddedByConfig)

  protected def add(config: (String, AppConfig.Host)): Unit = add(config._1, config._2)

  def getAllHostNames: Seq[String] = boincClients.keys.toSeq

  protected def destroy(): Unit = boincClients.foreach { case (_, BoincClientEntry(client, _, _)) => client.closeAll() }

  def queryDeathCounter: Map[String, Int] =
    boincClients.map { case (name, BoincClientEntry(client, _, _)) => (name, client.deathCounter.get()) }.toMap

  def checkHealth: IO[Map[String, Boolean]] = {
    import cats.implicits._

    boincClients
      .map{ case(name, BoincClientEntry(client, _, _)) => client.checkConnection().map(state => (name, state))}
      .toList
      .sequence
      .map(_.toMap)
  }

  def getAddresses: List[(String, Int)] = boincClients.values.map{ case BoincClientEntry(client, _, _) => (client.address, client.port)}.toList

  def getGroups: TrieMap[String, ListBuffer[String]] = clientGroups

  def getSerializableGroups: Map[String, List[String]] = getGroups.map{ case (key, value) => (key, value.toList)}.toMap

  def getDetailedHosts: List[HostDetails] = boincClients.map { case (name, entry) =>
    HostDetails(
      name,
      entry.client.address,
      entry.client.port,
      entry.client.password,
      entry.addedBy match {
        case AddedByDiscovery => "added_by_discovery"
        case AddedByConfig    => "added_by_config"
        case AddedByUser      => "added_by_user"
      },
      entry.client.deathCounter.get()
    )
  }.toList

  def addToGroup(group: String, hostname: String): Unit = {
    clientGroups.getOrElseUpdate(group, ListBuffer.empty) += hostname
    updateVersion()
  }

  def removeFromGroup(group: String, hostname: String): Unit = {
    clientGroups.getOrElseUpdate(group, ListBuffer.empty) -= hostname
    updateVersion()
  }

  def addGroup(group: String, hosts: List[String]): Unit = {
    clientGroups.getOrElseUpdate(group, new ListBuffer[String]()) ++= hosts
    updateVersion()
  }

  def remove(name: String): Unit = {
    boincClients.remove(name).foreach(_.client.closeAll())
    clientGroups.keys.foreach(removeFromGroup(_, name))
    updateVersion()
  }

  def getVersion: Long = version.get()

  private def updateVersion(): Unit = {
    version.set(System.currentTimeMillis())
    versionChangeListeners.forEach(_(this))
  }

  override def close(): Unit = {
    autoCloser.cancel(false)
    connectionCloser.cancel(false)
    healthChecker.cancel(false)
    destroy()
  }

}