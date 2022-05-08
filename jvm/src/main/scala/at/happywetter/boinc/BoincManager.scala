package at.happywetter.boinc

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.BoincManager._
import at.happywetter.boinc.dto.DatabaseDTO.CoreClient
import at.happywetter.boinc.shared.rpc.HostDetails
import at.happywetter.boinc.util.{Observer, PooledBoincClient}
import cats.data.OptionT
import cats.effect.{IO, Ref, Resource, Spawn}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.language.postfixOps
import cats.implicits._

import java.util.concurrent.TimeUnit

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

  private case class BoincClientEntry(client: PooledBoincClient, addedBy: AddedBy, finalizer: IO[Unit], var lastUsed: Long)

  private val timeout            =  1 minutes
  private val deathCollector     = 30 minutes
  private val healthTimeout      = 14 minutes
  private val inactiveConnection =  5 minutes

  def apply(config: Config, db: Database): Resource[IO, BoincManager] = for {

    observer <- Observer.unbounded[BoincManager]
    manager <- Resource.make(
      for {

        logger      <- Slf4jLogger.fromClass[IO](BoincManager.getClass)
        hostManager <- IO { new BoincManager(config.boinc.connectionPool, observer, logger) }

      } yield hostManager
    )(_.close())

    /* Initialize the manager from the config */
    _ <- Spawn[IO].background[Unit]( for {
      logger <- Slf4jLogger.fromClass[IO](BoincManager.getClass)
      _ <- logger.info("Importing clients from configuration (application.conf)")
      _ <- config.hostGroups.map { case (group, hosts) => manager.addGroup(group, hosts) }.toList.sequence_
      _ <- config.boinc.hosts.map(manager.add).toList.parSequence_
      _ <- logger.info("Finished importing clients from configuration")
    } yield ())

    /* Start all background tasks for the manager */
    _ <- Spawn[IO].background[Nothing](manager.autoCloser)
    _ <- Spawn[IO].background[Nothing](manager.connectionCloser)


    /* Initialize the manager with all the clients that are stored in the DB
     * do this lazily in the background so we don't block the creation ...
     */
    _ <- Spawn[IO].background(
      db.clients.queryAll().flatMap { coreClients =>
        for {
          logger <- Slf4jLogger.fromClass[IO](BoincManager.getClass)

          _     <- logger.info(s"Loading clients from database, found ${coreClients.size} entries")
          added <- coreClients.map { coreClient =>
            manager.add(
              coreClient.name,
              coreClient.address, coreClient.port, coreClient.password,
              coreClient.addedBy match {
                case CoreClient.ADDED_BY_DISCOVERY => AddedByDiscovery
                case CoreClient.ADDED_BY_USER      => AddedByUser
              }
            )
          }.parSequence
           .map(_.count(identity))

          _ <- logger.info(s"Successfully loaded $added clients from database")
        } yield ()
      }.handleErrorWith(ex => Slf4jLogger.fromClass[IO](BoincManager.getClass).flatMap(_.error(ex.getMessage)))
    )

  } yield manager

}

class BoincManager private (poolSize: Int, val changeListener: Observer[BoincManager], logger: SelfAwareStructuredLogger[IO]) {

  private val boincClients: Ref[IO, Map[String, BoincClientEntry]] = Ref.unsafe(Map.empty)
  private val clientGroups: Ref[IO, Map[String, ListBuffer[String]]] = Ref.unsafe(Map.empty)
  private val version: Ref[IO, Long] = Ref.unsafe(0L)

  // Close connections periodically
  private val autoCloser: IO[Nothing] =
    IO
      .sleep(timeout)
      .flatMap { _ =>
        import cats.implicits._

        boincClients.get.flatMap {
          _.map {
            case (name, BoincClientEntry(client, _, _, time)) =>
              client
                .hasOpenConnections
                .ifM(
                  client.close(inactiveConnection).flatMap { count =>
                    if (count > 0) logger.info(s"Closed $count socket connections for $name")
                    else           IO.unit
                  },
                  IO.unit
                )
          }
          .toList
          .sequence
        }
      }
      .foreverM

  // Throw out all dead boinc core clients
  private val connectionCloser =
    IO
      .sleep(deathCollector)
      .flatMap { _ =>
        import cats.implicits._

        boincClients.get.flatMap(_
          .map {
            case (name, BoincClientEntry(client, AddedByConfig, _, _)) =>
              client
                .hasOpenConnections
                .both(client.deathCounter.get)
                .map { case (openConnections, deathCounter) => !openConnections && deathCounter > 10 }
                .ifM(
                  logger.info(s"Client '$name' does have over ___ errored connections, not removing added by config ...")
                  ,
                  IO.unit
                ).map(_ => false)

            case (name, BoincClientEntry(client, AddedByDiscovery, _, _)) =>
              client
                .hasOpenConnections
                .both(client.deathCounter.get)
                .map { case (openConnections, deathCounter) => !openConnections && deathCounter > 10 }
                .ifM(
                  logger.info(s"Removing client '$name', has ___ errored connections and can't be reached!") *>
                  remove(name) *>
                  IO.pure(true)
                  ,
                  IO.pure(false)
                )
          }.toList
           .sequence
        )
        .map(_.find(identity).getOrElse(false))
        .ifM(
          IO { updateVersion() },
          IO.unit
        )
      }
      .foreverM

  def get(name: String): OptionT[IO, PooledBoincClient] =
    OptionT(boincClients.get.map(_.get(name)))
      .map { entry =>
        entry.lastUsed = System.currentTimeMillis()
        entry.client
      }

  def add(name: String, client: Resource[IO, PooledBoincClient], addedBy: AddedBy): IO[Boolean] = {
    boincClients
      .get
      .map(_.contains(name))
      .ifM(
        IO.pure(false),
        client
          .allocated
          .flatMap { case (client, finalizer) =>
            boincClients.update(_ + (name -> BoincClientEntry(client, addedBy, finalizer, System.currentTimeMillis()))) *>
            updateVersion() *>
            client.isAvailable.ifM(
              IO.pure(true),
              logger.warn(s"Unable to query details for host ${client.details.address}:${client.details.port}!") *>
              IO.pure(false)
            )
          }
      )
  }

  def add(name: String, address: String, port: Int, password: String, addedBy: AddedBy): IO[Boolean] =
    add(name, PooledBoincClient(poolSize, address, port, password), addedBy)

  protected def add(name: String, host: AppConfig.Host): IO[Boolean] =
    add(name, PooledBoincClient(poolSize, host.address, host.port, host.password), AddedByConfig)

  protected def add(config: (String, AppConfig.Host)): IO[Boolean] =
    add(config._1, config._2)


  def getAllHostNames: IO[Seq[String]] = boincClients.get.map(_.keys.toSeq)

  def queryDeathCounter: IO[Map[String, Int]] = {
    boincClients
      .get
      .flatMap { clients =>
        clients
          .map { case (name, BoincClientEntry(client, _, _, _)) => client.deathCounter.get.map(c => (name, c)) }
          .toList
          .sequence
          .map(_.toMap)
      }
  }

  def getAddresses: IO[List[(String, Int)]] = {
    boincClients
      .get
      .map(_
        .values
        .map{ case BoincClientEntry(client, _, _, _) => (client.details.address, client.details.port) }
        .toList
      )
  }

  def getGroups: Ref[IO, Map[String, ListBuffer[String]]] = clientGroups

  def getSerializableGroups: IO[Map[String, List[String]]] =
    clientGroups
      .get
      .map(_
        .map { case (key, value) => (key, value.toList)}.toMap
      )

  def getDetailedHosts: IO[List[HostDetails]] =
    boincClients
      .get
      .flatMap(_
        .map { case (name, entry) =>
          entry
            .client
            .deathCounter
            .get
            .map(deathCounter =>
              HostDetails(
                name,
                entry.client.details.address,
                entry.client.details.port,
                entry.client.details.password,
                entry.addedBy match {
                  case AddedByDiscovery => "added_by_discovery"
                  case AddedByConfig    => "added_by_config"
                  case AddedByUser      => "added_by_user"
                },
                deathCounter
              )
            )
        }.toList
         .sequence
    )

  def addToGroup(group: String, hostname: String): IO[Unit] = {
    clientGroups.update(groups =>
      if (groups.contains(group)) {
        groups(group) += hostname
        groups

      } else {
        groups + (group -> ListBuffer(hostname))
      }
    ) *>
    updateVersion()
  }

  def removeFromGroup(group: String, hostname: String): IO[Unit] = {
    clientGroups
      .get
      .map(_.contains(group))
      .ifM(
        clientGroups.update { map =>
          map(group) -= hostname
          map
        } *>
        updateVersion()
        ,
        IO.unit
      )
  }

  def addGroup(group: String, hosts: List[String]): IO[Unit] = {
    clientGroups
      .get
      .map(_.contains(group))
      .ifM(
        clientGroups.update { map =>
          map(group) ++= hosts
          map
        }
        ,
        clientGroups.update { map =>
          val buffer = new ListBuffer[String]()
          buffer ++= hosts

          map + (group -> buffer)
        }
      ) *>
      updateVersion()
  }

  def remove(name: String): IO[Unit] = {
    boincClients
      .get
      .map(_.contains(name))
      .ifM(
        boincClients
          .getAndUpdate(_ - name)
          .map(_(name))
          .flatMap { client =>
            client.finalizer
          } *>
          updateVersion()
        ,
        IO.unit
      )
  }

  def getVersion: IO[Long] = version.get

  private def updateVersion(): IO[Unit] = {
    version.set(System.currentTimeMillis()) *>
    changeListener.enqueue(this)
  }

  def close(): IO[Unit] = {
    boincClients
      .get
      .flatMap(_
        .map { case (_, BoincClientEntry(_, _, finalizer, _)) => finalizer }
        .toList
        .sequence_
      )
  }

}