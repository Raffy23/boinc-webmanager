package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared._

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object BoincParserUtils {

  implicit class BoincXMLNodeSeq(val node: NodeSeq) {

    def toTask: Task = TaskParser.fromXML(node)
    def toResult: Result = ResultParser.fromXML(node)
    def toHostInfo: HostInfo = HostInfoParser.fromXML(node)
    def toDiskUsage: DiskUsage = DiskUsageParser.fromXML(node)
    def toProjects: List[Project] = ProjectParser.fromXML(node)
    def toState: BoincState = BoincStateParser.fromXML(node)
    def toFileTransfers: List[FileTransfer] = FileTransferParser.fromXML(node)
    def toCCState: CCState = CCStateParser.fromXML(node)
    def toGlobalPrefs: GlobalPrefsOverride = GlobalPrefsParser.fromXML(node)

  }

  implicit class RichGlobalPrefs(val globalPrefsOverride: GlobalPrefsOverride) {

    def toXML: NodeSeq = GlobalPrefsParser.toXML(globalPrefsOverride)

  }

}



