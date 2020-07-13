package at.happywetter.boinc.shared.boincrpc

import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.boincrpc.BoincRPC.WorkunitAction.WorkunitAction

import scala.concurrent.Future

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.07.2016
  */
trait BoincCoreClient {

  def getVersion: Future[BoincVersion]

  // Read Boinc Stuff
  def getTasks(active: Boolean = true): Future[List[Result]]
  def getHostInfo: Future[HostInfo]
  def isNetworkAvailable: Future[Boolean]
  def getDiskUsage: Future[DiskUsage]
  def getProjects: Future[List[Project]]
  def getState: Future[BoincState]
  def getFileTransfer: Future[List[FileTransfer]]
  def getCCState: Future[CCState]
  def getStatistics: Future[Statistics]
  def getMessages(seqno: Int = 0): Future[List[Message]]
  def getNotices(seqno: Int = 0): Future[List[Notice]]

  def getGlobalPrefsOverride: Future[GlobalPrefsOverride]
  def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride): Future[Boolean]
  def readGlobalPrefsOverride: Future[Boolean]

  // Change Boinc workunits / projects
  def workunit(project: String, name: String, action: WorkunitAction): Future[Boolean]
  def project(url: String, action: ProjectAction): Future[Boolean]
  def attachProject(url: String, authenticator: String, name: String): Future[Boolean]
  def retryFileTransfer(project: String, file: String): Future[Boolean]

  // Change Run Modes
  def setCpu(mode: BoincRPC.Modes.Value, duration: Double = 0): Future[Boolean]
  def setGpu(mode: BoincRPC.Modes.Value, duration: Double = 0): Future[Boolean]
  def setNetwork(mode: BoincRPC.Modes.Value, duration: Double = 0): Future[Boolean]
  def setRun(mode: BoincRPC.Modes.Value, duration: Double = 0): Future[Boolean]
}
