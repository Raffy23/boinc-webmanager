package at.happywetter.boinc.server

import java.io.File

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.shared.webrpc.BoincProjectMetaData

import scala.collection.mutable
import scala.xml.XML

/**
  * Created by: 
  *
  * @author Raphael
  * @version 10.08.2017
  */
class XMLProjectStore(path: String) {

  private val contents = XML.loadFile(new File(path))
  private val projects = new mutable.HashMap[String, BoincProjectMetaData]()

  (contents \ "project").foreach(node =>
    projects +=
      ((node \ "name").text ->
        BoincProjectMetaData(
          (node \ "name").text,
          (node \ "url").text,
          (node \ "general_area").text,
          (node \ "specific_area").text,
          (node \ "description").text,
          (node \ "home").text,
          (node \ "platforms" \ "name").theSeq.map(name => name.text).toList
        )
      )
  )

  def importFrom(config: Config): Unit = {
    config.boinc.projects.customProjects.foreach { case (name, project) =>
      projects += (
        (name,
          BoincProjectMetaData(
            name, project.url, project.generalArea, "", project.description, project.organization, List()
          )
        )
      )
    }
  }


  def getProjects: Map[String, BoincProjectMetaData] = projects.toMap

}
