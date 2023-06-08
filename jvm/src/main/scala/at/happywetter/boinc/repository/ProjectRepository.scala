package at.happywetter.boinc.repository

import at.happywetter.boinc.dto.DatabaseDTO.Project
import cats.effect.IO
import doobie.{Transactor, Write}
import doobie.implicits._
import doobie.h2.implicits._

/**
 * Created by: 
 *
 * @author Raphael
 * @version 08.07.2020
 */
class ProjectRepository(xa: Transactor[IO]):

  implicit private val ProjectWrite: Write[Project] =
    Write[(String, String, String, String, String, String, Array[String])].contramap(p =>
      (p.name, p.url, p.generalArea, p.specificArea, p.description, p.home, p.platforms)
    )

  def insert(project: Project): IO[Int] =
    sql"""INSERT INTO project (name, url, general_area, specific_area, description, home, platforms) VALUES ($project)""".update.run
      .transact(xa)

  def queryAll(): IO[List[Project]] =
    sql"""SELECT * FROM project"""
      .query[Project]
      .to[List]
      .transact(xa)
