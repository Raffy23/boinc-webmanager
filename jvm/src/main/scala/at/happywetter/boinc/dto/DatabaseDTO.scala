package at.happywetter.boinc.dto

import at.happywetter.boinc.shared.webrpc.BoincProjectMetaData

/**
 * Created by: 
 *
 * @author Raphael
 * @version 02.07.2020
 */
object DatabaseDTO {

  final case class CoreClient(name: String, ipAddress: String, port: Int, password: String, addedBy: Int)
  object CoreClient {
    val ADDED_BY_USER      = 0
    val ADDED_BY_DISCOVERY = 1
  }

  final case class Project(name: String, url: String, generalArea: String, specificArea: String, description: String, home: String, platforms: String) {
    def toBoincProjectMetaData: BoincProjectMetaData =
      BoincProjectMetaData(name, url, generalArea, specificArea, description, home, platforms.split(";").toList)
  }

  object Project {
    def apply(dto: BoincProjectMetaData): Project = {
      new Project(dto.name, dto.url, dto.general_area, dto.specific_area, dto.description, dto.home, dto.platforms.mkString(";"))
    }
  }

}
