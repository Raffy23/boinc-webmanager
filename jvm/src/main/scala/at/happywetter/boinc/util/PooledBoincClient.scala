package at.happywetter.boinc.util

import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.boincrpc.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared.boincrpc.{BoincCoreClient, BoincRPC, _}
import at.happywetter.boinc.util.PooledBoincClient.{BoincClientParameters, ConnectionException}
import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.std.Semaphore
import cats.effect.{IO, Ref, Resource}

/**
  * Created by: 
  *
  * @author Raphael
  * @version 12.09.2017
  */
object PooledBoincClient {

  case class ConnectionException(e: Throwable) extends RuntimeException(e)

  case class BoincClientParameters(address: String, val port: Int, password: String)

  def apply(poolSize: Int, address: String, port: Int = 31416, password: String): Resource[IO, PooledBoincClient] =
    Resource.make(
      for {
        lock   <- Semaphore[IO](poolSize)
        client <- IO {
          new PooledBoincClient(lock, BoincClientParameters(address, port, password))
        }
      } yield client
    )(_.close())

}
class PooledBoincClient private (lock: Semaphore[IO], val details: BoincClientParameters) extends BoincCoreClient[IO] {

  val deathCounter: Ref[IO, Int] = Ref.unsafe(0)

  private def all = lastUsed.get.map(_.keys.toSeq)

  private val lastUsed: Ref[IO, Map[BoincClient, Long]] = Ref.unsafe(Map.empty)
  private val finalizer: Ref[IO, Map[BoincClient, IO[Unit]]] = Ref.unsafe(Map.empty)
  private val pool: Ref[IO, List[BoincClient]] = Ref.unsafe(List.empty)

  private def makeConnection(): IO[BoincClient] = {
    BoincClient(details.address, details.port, details.password)
      .allocated
      .flatTap { case (client, finalizer) =>
        this.finalizer.update(_ + (client -> finalizer)) *>
        this.lastUsed.update(_ + (client -> System.currentTimeMillis()))
      }
      .map(_._1)
  }

  private def takeConnection(): IO[BoincClient] =
    for {

      _   <- lock.acquire
      con <- pool.get.flatMap {
        case _ :: _ => pool.getAndUpdate(_.tail).map(_.head)
        case Nil    => makeConnection()
      }

      _   <- lastUsed.update(_ + (con -> System.currentTimeMillis()))
      _   <- lock.release

    } yield con


  private def connection[R](extractor: BoincClient => IO[R]): IO[R] = {
   takeConnection().bracketCase(extractor) {
     case (connection, Succeeded(_)) => pool.update(connection :: _)
     case (connection, Canceled())   => pool.update(connection :: _)
     case (connection, Errored(e))   => for {

       _ <- deathCounter.update(_ + 1)
       _ <- closeConnection(connection)
       _ <- IO.raiseError(ConnectionException(e))

     } yield ()
   }
  }

  private def closeConnection(connection: BoincClient): IO[Unit] = for {
    _ <- lastUsed.update(_ - connection)

    finalizer <- this.finalizer.get.map(_(connection))
    _         <- this.finalizer.update(_ - connection)

    _ <- finalizer

  } yield ()

  def close(): IO[Unit] = {
    import cats.implicits._

    all
      .flatMap(_
        .map(closeConnection)
        .sequence
      )
      .void
  }

  def close(timeout: Long): IO[Unit] = {
    import cats.implicits._
    val current = System.currentTimeMillis()

    lastUsed.get.flatMap(_
      .map {
        case (client, timestamp) if (timestamp+timeout) < current =>
          for {
            _ <- lock.acquire
            _ <- pool.update(_.filterNot(_ == client))
            _ <- closeConnection(client)
            _ <- lock.release
          } yield ()

        case _ =>
          IO.unit
      }
      .toList
      .sequence
      .void
    )
  }

  def hasOpenConnections: IO[Boolean] = pool.get.map(_.nonEmpty)

  override def getTasks(active: Boolean): IO[List[Result]] = connection(_.getTasks(active))

  override def getHostInfo: IO[HostInfo] = connection(_.getHostInfo)

  override def isNetworkAvailable: IO[Boolean] = connection(_.isNetworkAvailable)

  override def getDiskUsage: IO[DiskUsage] = connection(_.getDiskUsage)

  override def getProjects: IO[List[Project]] = connection(_.getProjects)

  override def getState: IO[BoincState] = connection(_.getState)

  override def getFileTransfer: IO[List[FileTransfer]] = connection(_.getFileTransfer)

  override def getCCState: IO[CCState] = connection(_.getCCState)

  override def getStatistics: IO[Statistics] = connection(_.getStatistics)

  override def getGlobalPrefsOverride: IO[GlobalPrefsOverride] = connection(_.getGlobalPrefsOverride)

  override def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride): IO[Boolean] =
    connection(_.setGlobalPrefsOverride(globalPrefsOverride))

  override def workunit(project: String, name: String, action: WorkunitAction): IO[Boolean] =
    connection(_.workunit(project, name, action))

  override def project(name: String, action: ProjectAction): IO[Boolean] =
    connection(_.project(name, action))

  override def attachProject(url: String, authenticator: String, name: String): IO[Boolean] =
    connection(_.attachProject(url, authenticator, name))

  override def setCpu(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    connection(_.setCpu(mode, duration))

  override def setGpu(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    connection(_.setGpu(mode, duration))

  override def setNetwork(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    connection(_.setNetwork(mode, duration))

  override def setRun(mode: BoincRPC.Modes.Value, duration: Double): IO[Boolean] =
    connection(_.setRun(mode, duration))

  override def getMessages(seqno: Int): IO[List[Message]] = connection(_.getMessages(seqno))

  override def getNotices(seqno: Int): IO[List[Notice]] = connection(_.getNotices(seqno))

  override def readGlobalPrefsOverride: IO[Boolean] = connection(_.readGlobalPrefsOverride)

  override def retryFileTransfer(project: String, file: String): IO[Boolean] =
    connection(_.retryFileTransfer(project, file))

  override def getVersion: IO[BoincVersion] =
    connection(_.getVersion)

  override def getAppConfig(url: String): IO[AppConfig] =
    connection(_.getAppConfig(url))

  override def setAppConfig(url: String, config: AppConfig): IO[Boolean] =
    connection(_.setAppConfig(url, config))

  override def quit(): IO[Unit] =
    connection(_.quit())
}
