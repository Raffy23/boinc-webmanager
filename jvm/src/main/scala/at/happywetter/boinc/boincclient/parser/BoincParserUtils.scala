package at.happywetter.boinc.boincclient.parser

import at.happywetter.boinc.shared.boincrpc._
import org.jsoup.nodes.Document

import scala.xml.NodeSeq

/**
  * Created by: 
  *
  * @author Raphael
  * @version 25.08.2017
  */
object BoincParserUtils {

  implicit class BoincXMLNodeSeq(val node: NodeSeq) extends AnyVal {

    def toTask: Task = TaskParser.fromXML(node)
    def toResult: Result = ResultParser.fromXML(node)
    def toHostInfo: HostInfo = HostInfoParser.fromXML(node)
    def toDiskUsage: DiskUsage = DiskUsageParser.fromXML(node)
    def toProjects: List[Project] = ProjectParser.fromXML(node)
    def toState: BoincState = BoincStateParser.fromXML(node)
    def toFileTransfers: List[FileTransfer] = FileTransferParser.fromXML(node)
    def toCCState: CCState = CCStateParser.fromXML(node)
    def toGlobalPrefs: GlobalPrefsOverride = GlobalPrefsParser.fromXML(node)
    def toStatistics: Statistics = ProjectStatsParser.fromXML(node)
    def toVersion: BoincVersion = VersionParser.fromXML(node)

  }

  implicit class RichGlobalPrefs(val globalPrefsOverride: GlobalPrefsOverride) extends AnyVal {

    def toXML: NodeSeq = GlobalPrefsParser.toXML(globalPrefsOverride)

  }

  implicit class BoincHtmlDocument(val document: Document) extends AnyVal {

    def toMessages: List[Message] = MessageParser.fromDocument(document)
    def toNotices: List[Notice] = NoticeParser.fromDocument(document)

  }

}



