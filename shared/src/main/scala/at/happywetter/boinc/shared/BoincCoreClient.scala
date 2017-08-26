package at.happywetter.boinc.shared

import at.happywetter.boinc.shared.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.BoincRPC.WorkunitAction.WorkunitAction

import scala.concurrent.Future


/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.07.2016
  */
trait BoincCoreClient {

  // Read Boinc Stuff
  def getTasks(active: Boolean = true): Future[List[Result]]
  def getHostInfo: Future[HostInfo]
  def isNetworkAvailable: Future[Boolean]
  def getDiskUsage: Future[DiskUsage]
  def getProjects: Future[List[Project]]
  def getState: Future[BoincState]
  def getFileTransfer: Future[List[FileTransfer]]
  def getCCState: Future[CCState]

  def getGlobalPrefsOverride: Future[GlobalPrefsOverride]
  def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride): Future[Boolean]

  // Change Boinc workunits / projects
  def workunit(project: String, name: String, action: WorkunitAction): Future[Boolean]
  def project(name: String, action: ProjectAction): Future[Boolean]
  def attachProject(url: String, authenticator: String, name: String): Future[Boolean]

  // Change Run Modes
  def setCpu(mode: BoincRPC.Modes.Value, duration: Double = 0): Future[Boolean]
  def setGpu(mode: BoincRPC.Modes.Value, duration: Double = 0): Future[Boolean]
  def setNetwork(mode: BoincRPC.Modes.Value, duration: Double = 0): Future[Boolean]
  def setRun(mode: BoincRPC.Modes.Value, duration: Double = 0): Future[Boolean]
}
