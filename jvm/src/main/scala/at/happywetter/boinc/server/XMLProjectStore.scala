package at.happywetter.boinc.server

import java.io.File

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.Database
import at.happywetter.boinc.dto.DatabaseDTO.Project
import at.happywetter.boinc.shared.webrpc.BoincProjectMetaData
import at.happywetter.boinc.util.IOAppTimer
import cats.effect._
import cats.effect.concurrent._

import scala.xml.XML
import monix.execution.Scheduler.Implicits.global


/**
  * Created by: 
  *
  * @author Raphael
  * @version 10.08.2017
  */
class XMLProjectStore(db: Database)(implicit contextShift: ContextShift[IO]) {

  private val projects = Ref.of[IO, Map[String, BoincProjectMetaData]](Map.empty)

  def importFrom(config: Config): IO[Unit] = projects.flatMap { ref =>
    ref.update(_ ++ config.boinc.projects.customProjects.map { case (name, project) =>
        println(name)
        name -> BoincProjectMetaData(
          name, project.url, project.generalArea, "", project.description, project.organization, List()
        )
    })
  }

  def getProjects: IO[Ref[IO, Map[String, BoincProjectMetaData]]] = projects

  def addProject(name: String, project: BoincProjectMetaData): IO[Unit] = projects.flatMap { ref =>
    db.projects.insert(Project(project)).to[IO] *>
    ref.update(_ + (name -> project))
  }

}

object XMLProjectStore {

  def apply(db: Database, config: Config)(implicit contextShift: ContextShift[IO]): Resource[IO, XMLProjectStore] =
    Resource.liftF(IO
      .pure(new XMLProjectStore(db))
      .flatMap { projectStore =>

        // Read projects from projects.xml in the background
        contextShift.blockOn(IOAppTimer.blocker)(
          projectStore.projects.map(_.update(_ ++
            (XML.loadFile(new File(config.boinc.projects.xmlSource)) \ "project").map(node =>
              (node \ "name").text ->
                BoincProjectMetaData(
                  (node \ "name").text,
                  (node \ "url").text,
                  (node \ "general_area").text,
                  (node \ "specific_area").text,
                  (node \ "description").text,
                  (node \ "home").text,
                  (node \ "platforms" \ "name").theSeq.map(name => name.text).toList
                )
            ).toMap
          ))
        ) *>
        projectStore.importFrom(config).map(_ => projectStore)
      }
    )

}
