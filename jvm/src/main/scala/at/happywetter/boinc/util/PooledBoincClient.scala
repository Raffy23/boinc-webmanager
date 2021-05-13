package at.happywetter.boinc.util

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicInteger

import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.boincrpc.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared.boincrpc.{BoincCoreClient, BoincRPC, _}
import at.happywetter.boinc.util.PooledBoincClient.ConnectionException
import cats.effect.ExitCase.{Canceled, Completed, Error}
import cats.effect.{Blocker, ContextShift, IO}

import scala.collection.concurrent.TrieMap

/**
  * Created by: 
  *
  * @author Raphael
  * @version 12.09.2017
  */
object PooledBoincClient {

  case class ConnectionException(e: Throwable) extends RuntimeException(e)

}
class PooledBoincClient(poolSize: Int, val address: String, val port: Int = 31416, val password: String, encoding: String, blocker: Blocker)(implicit cS: ContextShift[IO]) extends BoincCoreClient[IO] {

  val deathCounter = new AtomicInteger(0)

  private def all = lastUsed.keys.toList
  private val lastUsed = new TrieMap[BoincClient, Long]()
  private val pool = new LinkedBlockingDeque[BoincClient]
  (0 to poolSize).foreach(_ => {
    val client = new BoincClient(address, port, password, encoding)(implicitly, blocker)

    pool.offer(client)
    lastUsed += (client -> 0)
  })

  @inline private def takeConnection(): IO[BoincClient] = cS.blockOn(blocker)(IO {
    val con = pool.takeFirst()
    lastUsed(con) = System.currentTimeMillis()

    con
  })

  private def connection[R](extractor: BoincClient => IO[R]): IO[R] = {
   takeConnection().bracketCase(extractor) {
     case (connection, Completed) => IO { pool.addFirst(connection) }
     case (connection, Canceled)  => IO { pool.addFirst(connection) }
     case (connection, Error(e))  => IO {
       pool.addFirst(connection)
       deathCounter.incrementAndGet()

       IO.raiseError(ConnectionException(e))
     }
   }
  }

  def checkConnection(): IO[Boolean] =
    connection(_.getCCState).map{_ =>
      deathCounter.set(0)
      true
    }.handleErrorWith(_ => IO {
      deathCounter.incrementAndGet()
      false
    })

  def closeAll(): Unit = all.foreach(_.close())
  def closeOpen(): Unit = pool.iterator().forEachRemaining(_.close())
  def closeOpen(timeout: Long): Unit = {
    val current = System.currentTimeMillis()

    lastUsed.foreach {
      case (client, timestamp) if (timestamp+timeout) < current => client.close()
      case _ => /* Do nothing ... */
    }
  }

  def hasOpenConnections: Boolean = all.exists(_.isAuthenticated)

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
