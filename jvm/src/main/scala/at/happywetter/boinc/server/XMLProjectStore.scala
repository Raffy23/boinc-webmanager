package at.happywetter.boinc.server

import java.io.File
import java.util.concurrent.atomic.AtomicReference

import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.Database
import at.happywetter.boinc.dto.DatabaseDTO.Project
import at.happywetter.boinc.shared.boincrpc.BoincProjectMetaData
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

  private val projects = new AtomicReference[Map[String, BoincProjectMetaData]](Map.empty)

  def importFrom(config: Config): IO[Map[String, BoincProjectMetaData]] = IO {
    projects.updateAndGet(_ ++ config.boinc.projects.customProjects.map { case (name, project) =>
      name -> BoincProjectMetaData(
        name, project.url, project.generalArea, "", project.description, project.organization, List()
      )
    })
  }

  def getProjects: Map[String, BoincProjectMetaData] = projects.get()

  def addProject(name: String, project: BoincProjectMetaData): IO[Unit] = IO {
    db.projects.insert(Project(project)).to[IO].unsafeRunSync()
    projects.updateAndGet(_ + (name -> project))
  }

}

object XMLProjectStore {

  def apply(db: Database, config: Config)(implicit contextShift: ContextShift[IO]): Resource[IO, XMLProjectStore] =
    Resource.liftF(IO
      .pure(new XMLProjectStore(db))
      .flatMap { projectStore =>

        // Read projects from projects.xml in the background
        contextShift.blockOn(IOAppTimer.blocker)(IO {
          projectStore.projects.updateAndGet(_ ++
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
          )
        }
        ) *>
        projectStore.importFrom(config).map(_ => projectStore)
      }
    )

}
