package at.happywetter.boinc.server

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import at.happywetter.boinc.AppConfig.Config
import at.happywetter.boinc.Database
import at.happywetter.boinc.dto.DatabaseDTO.Project
import at.happywetter.boinc.shared.boincrpc.BoincProjectMetaData
import cats.effect._
import cats.effect.unsafe.implicits.global
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.xml.XML
import java.security.MessageDigest


/**
  * Created by: 
  *
  * @author Raphael
  * @version 10.08.2017
  */
class XMLProjectStore(db: Database) {

  private val projects = new AtomicReference[Map[String, BoincProjectMetaData]](Map.empty)

  def importFrom(config: Config): IO[Map[String, BoincProjectMetaData]] = IO {
    projects.updateAndGet(_ ++ config.boinc.projects.customProjects.map { case (name, project) =>
      computeETag(project.url)

      name -> BoincProjectMetaData(
        name, project.url, project.generalArea, "", project.description, project.organization, List()
      )
    })
  }

  def getProjects: Map[String, BoincProjectMetaData] = projects.get()

  def addProject(name: String, project: BoincProjectMetaData): IO[Unit] = IO {
    db.projects.insert(Project(project)).to[IO].unsafeRunSync()
    projects.updateAndGet(_ + (name -> project))
    computeETag(project.url)
  }

  private val digest = MessageDigest.getInstance("SHA-1");

  private def computeETag(projectURL: String): Unit = {
    digest.synchronized {
      digest.update(projectURL.getBytes)
    }
  }

  def eTag: String = {
    digest.synchronized {
      digest.digest().map("%02x" format _).mkString.take(12)
    }
  }

}

object XMLProjectStore {

  def apply(db: Database, config: Config): Resource[IO, XMLProjectStore] =
    Resource.eval(
      IO
        .pure(new XMLProjectStore(db))
        .flatMap { projectStore =>
          val logger = Slf4jLogger.getLoggerFromClass[IO](getClass)

          logger.trace("Reading projects.xml") *>
          IO.blocking {
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
          } *>
        projectStore.importFrom(config).map(_ => projectStore) <*
        logger.trace("Finished importing")
      }
    )

}
