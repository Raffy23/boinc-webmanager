package at.happywetter.boinc.shared

/**
  * Created by: 
  *
  * @author Raphael
  * @version 07.08.2017
  */
final case class WorkunitRequestBody(project: String, action: String)
final case class ProjectRequestBody(project: String, action: String)
final case class User(username: String, passwordHash: String, nonce: String)
final case class BoincModeChange(mode: String, duration: Double)
final case class AddProjectBody(projectUrl: String, projectName: String, user: String, password: String)
final case class ApplicationError(reason: String)