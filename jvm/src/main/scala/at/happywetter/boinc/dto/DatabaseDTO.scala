package at.happywetter.boinc.dto

import at.happywetter.boinc.shared.boincrpc.BoincProjectMetaData

/**
 * Created by: 
 *
 * @author Raphael
 * @version 02.07.2020
 */
object DatabaseDTO {

  final case class CoreClient(name: String, address: String, port: Int, password: String, addedBy: String)
  object CoreClient {
    val ADDED_BY_USER      = "user"
    val ADDED_BY_DISCOVERY = "discovery"
  }

  final case class Project(name: String, url: String, generalArea: String, specificArea: String, description: String, home: String, platforms: Array[String]) {
    def toBoincProjectMetaData: BoincProjectMetaData =
      BoincProjectMetaData(name, url, generalArea, specificArea, description, home, platforms.toList)
  }

  object Project {
    def apply(dto: BoincProjectMetaData): Project = {
      new Project(dto.name, dto.url, dto.general_area, dto.specific_area, dto.description, dto.home, dto.platforms.toArray)
    }
  }

}
