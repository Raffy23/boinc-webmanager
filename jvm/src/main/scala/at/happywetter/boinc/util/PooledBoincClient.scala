package at.happywetter.boinc.util

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

import at.happywetter.boinc.boincclient.BoincClient
import at.happywetter.boinc.shared.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.BoincRPC.WorkunitAction.WorkunitAction
import at.happywetter.boinc.shared._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by: 
  *
  * @author Raphael
  * @version 12.09.2017
  */
class PooledBoincClient(poolSize: Int, address: String, port: Int = 31416, password: String) extends BoincCoreClient {

  val deathCounter = new AtomicInteger(0)

  private val all  = new ListBuffer[BoincClient]()
  private val pool = new LinkedBlockingQueue[BoincClient]
  (0 to poolSize).foreach(_ => {
    val client = new BoincClient(address, port, password)

    pool.offer(client)
    all += client
  })

  private def connection[R](extractor: (BoincClient) => Future[R]): Future[R] =
    Future { pool.take() }
      .map(client => (client, extractor(client)))
      .flatMap{ case (client, result) => pool.offer(client); result }
      .recover{ case e: Exception => deathCounter.incrementAndGet(); throw e }


  def checkConnection(): Future[Boolean] =
    connection(_.getCCState)
      .map(_ => true)
      .recover{ case _: Exception =>
        deathCounter.incrementAndGet();
        false
      }

  def closeOpen(): Unit = pool.iterator().forEachRemaining(_.close())
  def closeAll(): Unit = all.foreach(_.close())
  def hasOpenConnections: Boolean = all.exists(_.isAuthenticated)

  override def getTasks(active: Boolean): Future[List[Result]] = connection(_.getTasks(active))

  override def getHostInfo: Future[HostInfo] = connection(_.getHostInfo)

  override def isNetworkAvailable: Future[Boolean] = connection(_.isNetworkAvailable)

  override def getDiskUsage: Future[DiskUsage] = connection(_.getDiskUsage)

  override def getProjects: Future[List[Project]] = connection(_.getProjects)

  override def getState: Future[BoincState] = connection(_.getState)

  override def getFileTransfer: Future[List[FileTransfer]] = connection(_.getFileTransfer)

  override def getCCState: Future[CCState] = connection(_.getCCState)

  override def getStatistics: Future[Statistics] = connection(_.getStatistics)

  override def getGlobalPrefsOverride: Future[GlobalPrefsOverride] = connection(_.getGlobalPrefsOverride)

  override def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride): Future[Boolean] =
    connection(_.setGlobalPrefsOverride(globalPrefsOverride))

  override def workunit(project: String, name: String, action: WorkunitAction): Future[Boolean] =
    connection(_.workunit(project, name, action))

  override def project(name: String, action: ProjectAction): Future[Boolean] =
    connection(_.project(name, action))

  override def attachProject(url: String, authenticator: String, name: String): Future[Boolean] =
    connection(_.attachProject(url, authenticator, name))

  override def setCpu(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] =
    connection(_.setCpu(mode, duration))

  override def setGpu(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] =
    connection(_.setGpu(mode, duration))

  override def setNetwork(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] =
    connection(_.setNetwork(mode, duration))

  override def setRun(mode: BoincRPC.Modes.Value, duration: Double): Future[Boolean] =
    connection(_.setRun(mode, duration))

}
