package at.happywetter.boinc.shared.boincrpc

import at.happywetter.boinc.shared.boincrpc.BoincRPC.ProjectAction.ProjectAction
import at.happywetter.boinc.shared.boincrpc.BoincRPC.WorkunitAction.WorkunitAction

/**
  * Created by: 
  *
  * @author Raphael
  * @version 20.07.2016
  */
trait BoincCoreClient[F[_]]:

  def getVersion: F[BoincVersion]

  // Read Boinc Stuff
  def getTasks(active: Boolean = true): F[List[Result]]
  def getHostInfo: F[HostInfo]
  def isNetworkAvailable: F[Boolean]
  def getDiskUsage: F[DiskUsage]
  def getProjects: F[List[Project]]
  def getState: F[BoincState]
  def getFileTransfer: F[List[FileTransfer]]
  def getCCState: F[CCState]
  def getStatistics: F[Statistics]
  def getMessages(seqno: Int = 0): F[List[Message]]
  def getNotices(seqno: Int = 0): F[List[Notice]]

  def getGlobalPrefsOverride: F[GlobalPrefsOverride]
  def setGlobalPrefsOverride(globalPrefsOverride: GlobalPrefsOverride): F[Boolean]
  def readGlobalPrefsOverride: F[Boolean]

  // Change Boinc workunits / projects
  def workunit(project: String, name: String, action: WorkunitAction): F[Boolean]
  def project(url: String, action: ProjectAction): F[Boolean]
  def attachProject(url: String, authenticator: String, name: String): F[Boolean]
  def retryFileTransfer(project: String, file: String): F[Boolean]

  // Change Run Modes
  def setCpu(mode: BoincRPC.Modes.Value, duration: Double = 0): F[Boolean]
  def setGpu(mode: BoincRPC.Modes.Value, duration: Double = 0): F[Boolean]
  def setNetwork(mode: BoincRPC.Modes.Value, duration: Double = 0): F[Boolean]
  def setRun(mode: BoincRPC.Modes.Value, duration: Double = 0): F[Boolean]

  // Other stuff
  def getAppConfig(url: String): F[AppConfig]
  def setAppConfig(url: String, config: AppConfig): F[Boolean]

  def quit(): F[Unit]
